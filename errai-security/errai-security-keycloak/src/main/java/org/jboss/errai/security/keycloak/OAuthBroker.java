package org.jboss.errai.security.keycloak;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.servlet.ServletOAuthClient;

public class OAuthBroker implements KeycloakAuthenticationBroker {
  
  private final ServletOAuthClient oAuthClient;

  public OAuthBroker(final ServletOAuthClient oAuthClient) {
    this.oAuthClient = oAuthClient;
  }

  @Override
  public void close() throws IOException {
    oAuthClient.stop();
  }

  @Override
  public void redirectRelative(final String relativeUri, final HttpServletRequest request,
      final HttpServletResponse response) throws IOException {
    oAuthClient.redirectRelative(relativeUri, request, response);
  }

  @Override
  public AccessTokenResponse getBearerToken(HttpServletRequest request) throws Exception {
    return oAuthClient.getBearerToken(request);
  }

}
