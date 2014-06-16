package org.jboss.errai.security.keycloak;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.errai.security.shared.api.Role;
import org.jboss.errai.security.shared.api.RoleImpl;
import org.jboss.errai.security.shared.api.UserCookieEncoder;
import org.jboss.errai.security.shared.api.identity.User;
import org.jboss.errai.security.shared.api.identity.User.StandardUserProperties;
import org.jboss.errai.security.shared.api.identity.UserImpl;
import org.keycloak.OAuth2Constants;
import org.keycloak.RSATokenVerifier;
import org.keycloak.VerificationException;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.servlet.ServletOAuthClient;

/**
 * Loads information for OAuth user into the {@link KeycloakAuthenticationService}.
 *
 * @author Max Barkley <mbarkley@redhat.com>
 */
@WebFilter(filterName="ErraiKeycloakAuthenticationFilter")
public class KeycloakAuthenticationFilter implements Filter {

  @SuppressWarnings("serial")
  static class TypedList extends ArrayList<RoleRepresentation> {
  }

  @Inject
  private ServletOAuthClient oauthClient;
  
  @Inject
  private KeycloakDeployment keycloakDeployment;

  @Inject
  private KeycloakAuthenticationService keycloakAuthService;

  @Inject
  private RedirectPageStore redirectPageStore;

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
  }

  @Override
  public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
          throws IOException, ServletException {
    final HttpServletRequest httpRequest = (HttpServletRequest) request;
    final HttpServletResponse httpResponse = (HttpServletResponse) response;
    final Map<String, String[]> reqParams = httpRequest.getParameterMap();

    if (reqParams.containsKey(OAuth2Constants.CODE)) {
      try {
        final User keycloakUser = createKeycloakUser(oauthClient.getBearerToken(httpRequest));
        keycloakAuthService.setKeycloakUser(keycloakUser);
        setUserCookie(keycloakUser, httpResponse);
      }
      catch (Exception e) {
        throw new ServletException(e);
      }
    }

    httpResponse.sendRedirect(redirectPageStore.getRedirectedPagePath());
  }

  @Override
  public void destroy() {
  }

  private User createKeycloakUser(final AccessTokenResponse accessTokenResponse) throws VerificationException {
    if (accessTokenResponse == null || accessTokenResponse.getIdToken() == null) {
      return User.ANONYMOUS;
    }

    final IDToken idToken = ServletOAuthClient.extractIdToken(accessTokenResponse.getIdToken());
    final AccessToken accessToken = RSATokenVerifier.verifyToken(accessTokenResponse.getToken(),
        keycloakDeployment.getRealmKey(), keycloakDeployment.getRealm());

    final User user = new UserImpl(idToken.getId(), createRoles(accessToken
        .getRealmAccess().getRoles()));
    user.setProperty(StandardUserProperties.FIRST_NAME, idToken.getGivenName());
    user.setProperty(StandardUserProperties.LAST_NAME, idToken.getFamilyName());
    user.setProperty(StandardUserProperties.EMAIL, idToken.getEmail());

    return user;
  }

  private Collection<? extends Role> createRoles(final Set<String> roleNames) {
    final List<Role> roles = new ArrayList<Role>(roleNames.size());
    
    for (final String roleName : roleNames) {
      roles.add(new RoleImpl(roleName));
    }
    
    return roles;
  }

  public static void setUserCookie(final User user, final HttpServletResponse response) {
    final Cookie userCookie = new Cookie(UserCookieEncoder.USER_COOKIE_NAME,
        UserCookieEncoder.toCookieValue(user));
    response.addCookie(userCookie);
  }
}
