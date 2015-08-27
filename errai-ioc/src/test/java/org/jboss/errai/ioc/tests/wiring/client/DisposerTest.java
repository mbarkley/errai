package org.jboss.errai.ioc.tests.wiring.client;

import org.jboss.errai.ioc.client.container.IOC;
import org.jboss.errai.ioc.client.test.AbstractErraiIOCTest;
import org.jboss.errai.ioc.tests.wiring.client.res.DependentBean;
import org.jboss.errai.ioc.tests.wiring.client.res.DependentBeanWithDisposer;
import org.jboss.errai.ioc.tests.wiring.client.res.SingletonBeanWithDisposer;

/**
 * @author Mike Brock
 */
public class DisposerTest extends AbstractErraiIOCTest {
  @Override
  public String getModuleName() {
    return "org.jboss.errai.ioc.tests.wiring.IOCWiringTests";
  }

  public void testDisposerFailsToDestroyAppScope() {

    final SingletonBeanWithDisposer outerBean = IOC.getBeanManager().lookupBean(SingletonBeanWithDisposer.class).getInstance();
    assertNotNull(outerBean);
    assertNotNull(outerBean.getDependentBeanDisposer());
    final DependentBean innerBean = outerBean.getBean();
    assertNotNull(innerBean);

    outerBean.dispose();

    assertFalse("inner bean should have been disposed", IOC.getBeanManager().isManaged(innerBean));
    assertTrue("outer bean should not have been disposed", IOC.getBeanManager().isManaged(outerBean));
    assertTrue("bean's destructor should have been called", innerBean.isPreDestroyCalled());

  }

  public void testDisposerWorksWithDependentScope() {

    final DependentBeanWithDisposer outerBean = IOC.getBeanManager().lookupBean(DependentBeanWithDisposer.class).getInstance();
    assertNotNull(outerBean);
    assertNotNull(outerBean.getDependentBeanDisposer());
    final DependentBean innerBean = outerBean.getBean();
    assertNotNull(innerBean);

    outerBean.dispose();

    assertFalse("inner bean should have been disposed", IOC.getBeanManager().isManaged(innerBean));
    assertTrue("outer bean should not have been disposed", IOC.getBeanManager().isManaged(outerBean));
    assertTrue("inner bean's destructor should have been called", innerBean.isPreDestroyCalled());

  }
}
