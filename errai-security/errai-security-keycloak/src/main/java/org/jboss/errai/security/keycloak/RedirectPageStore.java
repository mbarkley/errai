package org.jboss.errai.security.keycloak;

import java.io.Serializable;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.servlet.ServletContext;

/**
 * @author mbarkley
 */
@SessionScoped
public class RedirectPageStore implements Serializable {

  private static final long serialVersionUID = 1L;

  private String redirectedPagePath;

  @Inject
  private ServletContext servletContext;

  public void setRedirectedPagePath(final String redirectedPagePath) {
    this.redirectedPagePath = redirectedPagePath;
  }

  public String getRedirectedPagePath() {
    if (redirectedPagePath != null) {
      return redirectedPagePath;
    }
    else {
      return servletContext.getContextPath();
    }
  }
}
