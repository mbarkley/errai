package org.jboss.errai.security.client.local;

import org.jboss.errai.ioc.client.container.IOC;
import org.jboss.errai.marshalling.client.Marshalling;
import org.jboss.errai.marshalling.client.api.MarshallerFramework;
import org.jboss.errai.security.client.local.context.SecurityContext;
import org.jboss.errai.security.shared.api.identity.User;
import org.jboss.errai.security.shared.api.identity.UserImpl;

import com.google.gwt.user.client.Cookies;

/**
 * Tests for proper behaviour when the app loads up and the remembered user
 * cookie is already populated. See also
 * {@link UnpopulatedUserStorageIntegrationTest} for tests where the cookie is
 * not set when the page initially loads.
 */
public class PrePopulatedUserStorageIntegrationTest extends AbstractSecurityInterceptorTest {

  private User prePopulatedUser;

  @Override
  public String getModuleName() {
    return "org.jboss.errai.security.SecurityTest";
  }

  @Override
  protected void gwtSetUp() throws Exception {
    MarshallerFramework.initializeDefaultSessionProvider();
    prePopulatedUser = new UserImpl("remembered");
    Cookies.setCookie(SecurityContext.USER_COOKIE_NAME, Marshalling.toJSON(prePopulatedUser));
    super.gwtSetUp();
  }

  public void testUsingRememberedUserOnAppStart() throws Exception {
    final SecurityContext securityContext = IOC.getBeanManager().lookupBean(SecurityContext.class).getInstance();
    assertEquals(prePopulatedUser, securityContext.getActiveUserCache().getUser());
  }

  public void testGracefulFailureWithRememberedUserButInvalidServerSession() throws Exception {
    asyncTest();

    final SecurityContext securityContext = IOC.getBeanManager().lookupBean(SecurityContext.class).getInstance();
    assertEquals(prePopulatedUser, securityContext.getActiveUserCache().getUser());

    // this is a new HTTP session, so we're not actually logged in on the server.
    // once the security service's RPC call comes back, the client-side context should agree that we're not logged in
    testUntil(TIME_LIMIT, new Runnable() {

      @Override
      public void run() {
        assertNull(securityContext.getActiveUserCache().getUser());
        assertNull(Cookies.getCookie(SecurityContext.USER_COOKIE_NAME));
        finishTest();
      }
    });
  }

}
