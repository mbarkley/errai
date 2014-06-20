package org.jboss.errai.security.keycloak;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.jboss.logging.Logger;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.servlet.ServletOAuthClient;

/**
 * <p>
 * Configures the {@link ServletOAuthClient} on context initialization.
 *
 * <p>
 * This class was taken from the keycloak <a href=
 * "https://github.com/keycloak/keycloak/blob/1.0-beta-2/examples/demo-template/third-party-cdi/src/main/java/org/keycloak/example/oauth/AppContextListener.java"
 * >third-party-cdi example</a>.
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@WebListener
public class AppContextListener implements ServletContextListener {

  private static final Logger logger = Logger.getLogger(AppContextListener.class);

  @Inject
  private KeycloakDeployment keycloakDeployment;

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    System.out.println("Context initialized!");
    final ServletContext context = sce.getServletContext();

    final KeycloakDeployment foundDeployment = KeycloakDeploymentBuilder.build(getConfigInputStream(context));
    copyDeployment(foundDeployment, keycloakDeployment);

    logger.info("Keycloak deployment configured and started");
  }

  private void copyDeployment(final KeycloakDeployment from, final KeycloakDeployment to) {
    to.setBearerOnly(from.isBearerOnly());
    to.setClient(from.getClient());
    to.setCors(from.isCors());
    to.setCorsAllowedHeaders(from.getCorsAllowedHeaders());
    to.setCorsAllowedMethods(from.getCorsAllowedMethods());
    to.setCorsMaxAge(from.getCorsMaxAge());
    to.setExposeToken(from.isExposeToken());
    to.setNotBefore(from.getNotBefore());
    to.setPublicClient(from.isPublicClient());
    to.setRealm(from.getRealm());
    to.setRealmKey(from.getRealmKey());
    to.setResourceCredentials(from.getResourceCredentials());
    to.setResourceName(from.getResourceName());
    to.setScope(from.getScope());
    to.setSslRequired(from.isSslRequired());
    to.setStateCookieName(from.getStateCookieName());
    to.setUseResourceRoleMappings(from.isUseResourceRoleMappings());

    // Must be set after realm
    to.setAuthServerBaseUrl(from.getAuthServerBaseUrl());
  }

  private InputStream getConfigInputStream(final ServletContext context) {
    InputStream is = null;
    String path = context.getInitParameter("keycloak.config.file");
    if (path == null) {
      is = context.getResourceAsStream("/WEB-INF/keycloak.json");
    }
    else {
      try {
        is = new FileInputStream(path);
      }
      catch (FileNotFoundException e) {
        throw new RuntimeException(e);
      }
    }
    return is;
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
  }
}
