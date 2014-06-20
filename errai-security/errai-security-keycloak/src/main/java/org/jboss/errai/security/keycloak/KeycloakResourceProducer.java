package org.jboss.errai.security.keycloak;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.keycloak.adapters.KeycloakDeployment;

@ApplicationScoped
public class KeycloakResourceProducer {

  @Produces
  @ApplicationScoped
  public KeycloakDeployment getKeycloakDeployment() {
    return new KeycloakDeployment();
  }
}
