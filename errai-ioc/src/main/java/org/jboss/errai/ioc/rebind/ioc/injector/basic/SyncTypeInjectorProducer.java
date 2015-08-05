package org.jboss.errai.ioc.rebind.ioc.injector.basic;

import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.ioc.rebind.ioc.injector.Injector;
import org.jboss.errai.ioc.rebind.ioc.injector.api.InjectionContext;
import org.jboss.errai.ioc.rebind.ioc.injector.api.TypeInjectorProducer;

public class SyncTypeInjectorProducer implements TypeInjectorProducer {

  @Override
  public Injector create(MetaClass type, InjectionContext context) {
    return new TypeInjector(type, context);
  }

}
