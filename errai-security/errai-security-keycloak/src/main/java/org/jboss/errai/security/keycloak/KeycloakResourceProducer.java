package org.jboss.errai.security.keycloak;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.servlet.ServletOAuthClient;

@ApplicationScoped
public class KeycloakResourceProducer {

  @Produces
  @ApplicationScoped
  public ServletOAuthClient getServletOAuthClient() {
    return new ServletOAuthClient();
  }
  
  @Produces
  @ApplicationScoped
  public KeycloakAuthenticationBroker getKeycloakAuthenticationBroker(final ServletOAuthClient oAuthClient) {
    return new OAuthBroker(oAuthClient);
  }

  @Produces
  @ApplicationScoped
  public KeycloakDeployment getKeycloakDeployment() {
    return new KeycloakDeployment();
  }
}
