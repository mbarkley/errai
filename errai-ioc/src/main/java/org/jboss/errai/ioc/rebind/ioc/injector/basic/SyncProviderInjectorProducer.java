package org.jboss.errai.ioc.rebind.ioc.injector.basic;

import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.ioc.rebind.ioc.injector.Injector;
import org.jboss.errai.ioc.rebind.ioc.injector.api.InjectionContext;
import org.jboss.errai.ioc.rebind.ioc.injector.api.ProviderInjectorProducer;

public class SyncProviderInjectorProducer implements ProviderInjectorProducer {

  @Override
  public Injector create(MetaClass type, MetaClass providerType, InjectionContext context) {
    return new ProviderInjector(type, providerType, context);
  }

}
