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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpHeaders;
import org.keycloak.servlet.ServletOAuthClient;

/**
 * Redirects users to the Keycloak server for authentication.
 *
 * @author Max Barkley <mbarkley@redhat.com>
 */
@WebFilter(filterName = "ErraiKeycloakRedirectFilter")
public class KeycloakRedirectFilter implements Filter {

  public static final String DEFAULT_RELATIVE_REDIRECT_PATH = "/auth-process";

  public static final String RELATIVE_REDIRECT_PARAM_NAME = "relativeRedirect";

  private String relativeRedirectPath = DEFAULT_RELATIVE_REDIRECT_PATH;

  @Inject
  private ServletOAuthClient oAuthClient;

  @Inject
  private RedirectPageStore redirectPageStore;

  @Override
  public void init(final FilterConfig filterConfig) throws ServletException {
    final String redirectParam = filterConfig.getInitParameter(RELATIVE_REDIRECT_PARAM_NAME);
    if (redirectParam != null) {
      relativeRedirectPath = redirectParam;
    }
  }

  @Override
  public void doFilter(final ServletRequest request, final ServletResponse response,
      final FilterChain chain) throws IOException, ServletException {
    final HttpServletRequest httpRequest = (HttpServletRequest) request;
    final String referer = httpRequest.getHeader(HttpHeaders.REFERER);

    if (referer != null) {
      redirectPageStore.setRedirectedPagePath(referer);
    }

    oAuthClient.redirectRelative(relativeRedirectPath, httpRequest,
        (HttpServletResponse) response);
  }

  @Override
  public void destroy() {
  }
}
