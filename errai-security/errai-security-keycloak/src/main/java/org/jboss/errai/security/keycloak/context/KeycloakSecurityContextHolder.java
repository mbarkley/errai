package org.jboss.errai.security.keycloak.context;

import java.io.Serializable;

import javax.enterprise.context.SessionScoped;

import org.keycloak.KeycloakSecurityContext;

@SessionScoped
public class KeycloakSecurityContextHolder implements Serializable {

  private static final long serialVersionUID = 1L;

  private KeycloakSecurityContext securityContext;

  public KeycloakSecurityContext getSecurityContext() {
    return securityContext;
  }

  public void setSecurityContext(KeycloakSecurityContext securityContext) {
    this.securityContext = securityContext;
  }
}
