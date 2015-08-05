package org.jboss.errai.ioc.rebind.ioc.injector.async;

import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.ioc.rebind.ioc.injector.Injector;
import org.jboss.errai.ioc.rebind.ioc.injector.api.ContextualProviderInjectorProducer;
import org.jboss.errai.ioc.rebind.ioc.injector.api.InjectionContext;

public class AsyncContextualProviderInjectorProducer implements ContextualProviderInjectorProducer {

  @Override
  public Injector create(MetaClass type, MetaClass providerType, InjectionContext context) {
    return new AsyncContextualProviderInjector(type, providerType, context);
  }

}
