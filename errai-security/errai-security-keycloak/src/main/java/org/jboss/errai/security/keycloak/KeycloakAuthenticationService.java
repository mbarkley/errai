package org.jboss.errai.security.keycloak;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.jboss.errai.bus.server.annotations.Service;
import org.jboss.errai.security.keycloak.extension.Wrapped;
import org.jboss.errai.security.shared.api.Role;
import org.jboss.errai.security.shared.api.RoleImpl;
import org.jboss.errai.security.shared.api.identity.User;
import org.jboss.errai.security.shared.api.identity.User.StandardUserProperties;
import org.jboss.errai.security.shared.api.identity.UserImpl;
import org.jboss.errai.security.shared.exception.AlreadyLoggedInException;
import org.jboss.errai.security.shared.exception.FailedAuthenticationException;
import org.jboss.errai.security.shared.service.AuthenticationService;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.AccessToken;

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

  private KeycloakSecurityContext keycloakSecurityContext;

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
    return keycloakSecurityContext != null && keycloakSecurityContext.getToken() != null;
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
    setSecurityContext(null);
  }

  @Override
  public User getUser() {
    if (keycloakIsLoggedIn()) {
      return getKeycloakUser();
    }
    else if (wrappedIsLoggedIn()) {
      return authServiceInstance.get().getUser();
    }
    else {
      return User.ANONYMOUS;
    }
  }

  private User getKeycloakUser() {
    if (!keycloakIsLoggedIn()) {
      throw new IllegalStateException(
          "Cannot call getKeycloakUser if not logged in to through Keycloak.");
    }

    if (keycloakUser == null) {
      keycloakUser = createKeycloakUser(keycloakSecurityContext.getToken());
    }

    return keycloakUser;
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

  void setSecurityContext(final KeycloakSecurityContext keycloakSecurityContext) {
    if (wrappedIsLoggedIn() && keycloakSecurityContext != null) {
      throw new AlreadyLoggedInException("Logged in as " + authServiceInstance.get().getUser());
    }
    this.keycloakSecurityContext = keycloakSecurityContext;
    keycloakUser = null;
  }
}
