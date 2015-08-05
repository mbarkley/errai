package org.jboss.errai.ioc.rebind.ioc.injector.async;

import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.ioc.rebind.ioc.injector.Injector;
import org.jboss.errai.ioc.rebind.ioc.injector.api.InjectionContext;
import org.jboss.errai.ioc.rebind.ioc.injector.api.ProviderInjectorProducer;

public class AsyncProviderInjectorProducer implements ProviderInjectorProducer {

  @Override
  public Injector create(MetaClass type, MetaClass providerType, InjectionContext context) {
    return new AsyncProviderInjector(type, providerType, context);
  }

}
