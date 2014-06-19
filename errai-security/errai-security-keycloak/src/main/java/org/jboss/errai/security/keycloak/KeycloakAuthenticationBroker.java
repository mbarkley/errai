package org.jboss.errai.security.keycloak;

import java.io.Closeable;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.keycloak.representations.AccessTokenResponse;

public interface KeycloakAuthenticationBroker extends Closeable {

  void redirectRelative(String relativeUri, HttpServletRequest request, HttpServletResponse response) throws IOException;
  
  AccessTokenResponse getBearerToken(HttpServletRequest request) throws Exception;
}
