package org.jboss.errai.ioc.rebind.ioc.injector.basic;

import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.ioc.rebind.ioc.injector.Injector;
import org.jboss.errai.ioc.rebind.ioc.injector.api.ContextualProviderInjectorProducer;
import org.jboss.errai.ioc.rebind.ioc.injector.api.InjectionContext;

public class SyncContextualProviderInjectorProducer implements ContextualProviderInjectorProducer {

  @Override
  public Injector create(MetaClass type, MetaClass providerType, InjectionContext context) {
    return new ContextualProviderInjector(type, providerType, context);
  }

}
