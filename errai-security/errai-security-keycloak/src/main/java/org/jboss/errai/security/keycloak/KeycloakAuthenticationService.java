package org.jboss.errai.security.keycloak;

import java.io.Serializable;

import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.jboss.errai.bus.server.annotations.Service;
import org.jboss.errai.security.keycloak.extension.Wrapped;
import org.jboss.errai.security.shared.api.identity.User;
import org.jboss.errai.security.shared.exception.AlreadyLoggedInException;
import org.jboss.errai.security.shared.exception.FailedAuthenticationException;
import org.jboss.errai.security.shared.service.AuthenticationService;

/**
 * An {@link AuthenticationService} implementation that integrates with
 * Keycloak. This implementation optionally wraps another
 * {@link AuthenticationService} so that an app can have local and foreign user
 * authentication.
 *
 * @author Max Barkley <mbarkley@redhat.com>
 */
@Service
@SessionScoped
public class KeycloakAuthenticationService implements AuthenticationService, Serializable {

  private static final long serialVersionUID = 1L;

  @Inject
  @Wrapped
  private Instance<AuthenticationService> authServiceInstance;

  private User keycloakUser;

  @Override
  public User login(final String username, final String password) {
    if (!keycloakIsLoggedIn()) {
      return performLoginWithWrappedService(username, password);
    }
    else {
      throw new AlreadyLoggedInException("Already logged in through Keycloak.");
    }
  }

  private User performLoginWithWrappedService(final String username, final String password) {
    if (!authServiceInstance.isUnsatisfied()) {
      return authServiceInstance.get().login(username, password);
    }
    else {
      throw new FailedAuthenticationException(
          "Must provide a non-keycloak AuthenticationService to use the login method.");
    }
  }

  @Override
  public boolean isLoggedIn() {
    return keycloakIsLoggedIn() || wrappedIsLoggedIn();
  }

  private boolean wrappedIsLoggedIn() {
    return !authServiceInstance.isUnsatisfied() && authServiceInstance.get().isLoggedIn();
  }

  private boolean keycloakIsLoggedIn() {
    return keycloakUser != null;
  }

  void setKeycloakUser(final User user) {
    keycloakUser = user;
  }

  @Override
  public void logout() {
    if (keycloakIsLoggedIn()) {
      keycloakLogout();
    }
    else if (wrappedIsLoggedIn()) {
      authServiceInstance.get().logout();
    }
  }

  private void keycloakLogout() {
    keycloakUser = null;
  }

  @Override
  public User getUser() {
    if (keycloakIsLoggedIn()) {
      return keycloakUser;
    }
    else if (wrappedIsLoggedIn()) {
      return authServiceInstance.get().getUser();
    }
    else {
      return User.ANONYMOUS;
    }
  }
}
