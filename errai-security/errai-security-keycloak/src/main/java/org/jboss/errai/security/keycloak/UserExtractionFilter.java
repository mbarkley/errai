package org.jboss.errai.security.keycloak;

import java.io.IOException;

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

import org.jboss.errai.marshalling.server.MappingContextSingleton;
import org.jboss.errai.security.shared.api.UserCookieEncoder;
import org.jboss.errai.security.shared.api.identity.User;

/**
 * Loads information for OAuth user into the {@link KeycloakAuthenticationService}.
 *
 * @author Max Barkley <mbarkley@redhat.com>
 */
@WebFilter(filterName="ErraiUserExtractionFilter")
public class UserExtractionFilter implements Filter {

  static {
    MappingContextSingleton.get();
  }

  @Inject
  private KeycloakAuthenticationService keycloakAuthService;

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
  }

  @Override
  public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
          throws IOException, ServletException {
    final HttpServletResponse httpResponse = (HttpServletResponse) response;

    final User keycloakUser = keycloakAuthService.getUser();
    setUserCookie(keycloakUser, httpResponse);

    chain.doFilter(request, response);
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
