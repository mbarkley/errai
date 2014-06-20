package org.jboss.errai.security.keycloak;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletResponse;

import org.keycloak.KeycloakSecurityContext;

/**
 * Redirects users to the Keycloak server for authentication.
 *
 * @author Max Barkley <mbarkley@redhat.com>
 */
@WebFilter(filterName = "ErraiLoginRedirectFilter")
public class LoginRedirectFilter implements Filter {

  @Inject
  private ServletContext servletContext;

  @Inject
  private KeycloakAuthenticationService keycloakAuthService;

  @Override
  public void init(final FilterConfig filterConfig) throws ServletException {
  }

  @Override
  public void doFilter(final ServletRequest request, final ServletResponse response,
      final FilterChain chain) throws IOException, ServletException {
    final HttpServletResponse httpResponse = (HttpServletResponse) response;
    keycloakAuthService.setSecurityContext((KeycloakSecurityContext) request.getAttribute(KeycloakSecurityContext.class
            .getName()));
    // TODO make this configurable
    httpResponse.sendRedirect(servletContext.getContextPath());
  }

  @Override
  public void destroy() {
  }
}
