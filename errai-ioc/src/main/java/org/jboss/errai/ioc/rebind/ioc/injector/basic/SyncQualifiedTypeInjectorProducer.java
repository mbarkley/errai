package org.jboss.errai.ioc.rebind.ioc.injector.basic;

import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaParameterizedType;
import org.jboss.errai.ioc.rebind.ioc.injector.Injector;
import org.jboss.errai.ioc.rebind.ioc.injector.api.QualifiedTypeInjectorProducer;

public class SyncQualifiedTypeInjectorProducer implements QualifiedTypeInjectorProducer {

  @Override
  public Injector create(MetaClass type, Injector delegate, MetaParameterizedType parameterizedType) {
    return new QualifiedTypeInjectorDelegate(type, delegate, parameterizedType);
  }

}
