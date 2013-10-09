package org.jboss.errai.requireroles.client.local;

import org.jboss.errai.bus.client.api.BusErrorCallback;
import org.jboss.errai.bus.client.api.messaging.Message;
import org.jboss.errai.common.client.framework.ProxyProvider;
import org.jboss.errai.common.client.framework.RemoteServiceProxyFactory;
import org.jboss.errai.enterprise.client.cdi.AbstractErraiCDITest;
import org.jboss.errai.ioc.client.container.IOC;
import org.jboss.errai.security.shared.AuthenticationService;

public class RequireRoleElementTest extends AbstractErraiCDITest {

  AuthenticationService authServ = new PermissiveAuth();
  
  @Override
  public String getModuleName() {
    return "org.jboss.errai.requireroles.RequireRoleElementTest";
  }
  
  @Override
  protected void gwtSetUp() throws Exception {
    disableBus = true;
    super.gwtSetUp();
    RemoteServiceProxyFactory.addRemoteProxy(AuthenticationService.class, new ProxyProvider() {
      @Override
      public Object getProxy() {
        return authServ;
      }
    });
  }

  public void testBothHiddenWhenLoggedOut() throws Exception {
    RequireRoleTestModule module = IOC.getBeanManager().lookupBean(RequireRoleTestModule.class).getInstance();
    assertFalse(module.admin.isVisible());
    assertFalse(module.user.isVisible());
  }
  
  public void testAdminHiddenWhenLoggedInAsUser() throws Exception {
    RequireRoleTestModule module = IOC.getBeanManager().lookupBean(RequireRoleTestModule.class).getInstance();
    module.identity.setUsername("user");
    module.identity.login(null, new BusErrorCallback() {
      @Override
      public boolean error(Message message, Throwable throwable) {
        fail("No error should have occurred");
        return false;
      }
    });
    
    assertFalse(module.admin.isVisible());
    assertTrue(module.user.isVisible());
  }
  
  public void testUserHiddenWhenLoggedInAsAdmin() throws Exception {
    RequireRoleTestModule module = IOC.getBeanManager().lookupBean(RequireRoleTestModule.class).getInstance();
    module.identity.setUsername("admin");
    module.identity.login(null, new BusErrorCallback() {
      @Override
      public boolean error(Message message, Throwable throwable) {
        fail("No error should have occurred");
        return false;
      }
    });
    
    assertFalse(module.admin.isVisible());
    assertTrue(module.admin.isVisible());
  }
  
}
