package org.jboss.errai.ioc.rebind.ioc.injector.async;

import org.jboss.errai.codegen.Statement;
import org.jboss.errai.ioc.rebind.ioc.injector.AbstractInjector;
import org.jboss.errai.ioc.rebind.ioc.injector.api.InjectionContext;

/**
 * @author Mike Brock
 */
public abstract class AbstractAsyncInjector extends AbstractInjector {

  @Override
  public void registerWithBeanManager(final InjectionContext context, final Statement valueRef) {
    throw new RuntimeException("To be removed.");
  }
}
