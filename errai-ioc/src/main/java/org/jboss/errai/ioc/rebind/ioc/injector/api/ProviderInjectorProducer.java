package org.jboss.errai.ioc.rebind.ioc.injector.api;

import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.ioc.rebind.ioc.injector.Injector;

public interface ProviderInjectorProducer extends InjectorProducer {

  Injector create(MetaClass type, MetaClass providerType, InjectionContext context);

}
