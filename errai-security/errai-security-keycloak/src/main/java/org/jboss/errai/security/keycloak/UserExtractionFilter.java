package org.jboss.errai.security.keycloak;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
import javax.servlet.http.HttpServletResponse;

import org.jboss.errai.security.shared.api.Role;
import org.jboss.errai.security.shared.api.RoleImpl;
import org.jboss.errai.security.shared.api.UserCookieEncoder;
import org.jboss.errai.security.shared.api.identity.User;
import org.jboss.errai.security.shared.api.identity.User.StandardUserProperties;
import org.jboss.errai.security.shared.api.identity.UserImpl;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.AccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads information for OAuth user into the {@link KeycloakAuthenticationService}.
 *
 * @author Max Barkley <mbarkley@redhat.com>
 */
@WebFilter(filterName="ErraiKeycloakAuthenticationFilter")
public class UserExtractionFilter implements Filter {

  @Inject
  private KeycloakAuthenticationService keycloakAuthService;

  @Inject
  private KeycloakSecurityContextHolder securityContextHolder;

  private final Logger logger = LoggerFactory.getLogger(UserExtractionFilter.class);

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
  }

  @Override
  public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
          throws IOException, ServletException {
    final HttpServletResponse httpResponse = (HttpServletResponse) response;

    final User keycloakUser = getKeycloakUser(securityContextHolder.getSecurityContext());
    keycloakAuthService.setKeycloakUser(keycloakUser);
    setUserCookie(keycloakUser, httpResponse);

    chain.doFilter(request, response);
  }

  public User getKeycloakUser(final KeycloakSecurityContext securityContext) {
    if (securityContext != null) {
      final AccessToken token = securityContext.getToken();
      if (token != null) {
        try {
          return createKeycloakUser(token);
        }
        catch (Exception e) {
          logger.warn("An error occurred while attempting to extract Keycloak user form request.", e);
        }
      }
    }

    return User.ANONYMOUS;
  }

  protected User createKeycloakUser(final AccessToken accessToken) {
    final User user = new UserImpl(accessToken.getId(), createRoles(accessToken
        .getRealmAccess().getRoles()));
    user.setProperty(StandardUserProperties.FIRST_NAME, accessToken.getGivenName());
    user.setProperty(StandardUserProperties.LAST_NAME, accessToken.getFamilyName());
    user.setProperty(StandardUserProperties.EMAIL, accessToken.getEmail());

    return user;
  }

  private Collection<? extends Role> createRoles(final Set<String> roleNames) {
    final List<Role> roles = new ArrayList<Role>(roleNames.size());

    for (final String roleName : roleNames) {
      roles.add(new RoleImpl(roleName));
    }

    return roles;
  }

  @Override
  public void destroy() {
  }

  public static void setUserCookie(final User user, final HttpServletResponse response) {
    final Cookie userCookie = new Cookie(UserCookieEncoder.USER_COOKIE_NAME,
        UserCookieEncoder.toCookieValue(user));
    response.addCookie(userCookie);
  }
}
