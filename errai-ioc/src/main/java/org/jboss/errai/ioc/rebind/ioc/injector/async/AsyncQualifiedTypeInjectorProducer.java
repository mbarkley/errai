package org.jboss.errai.ioc.rebind.ioc.injector.async;

import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaParameterizedType;
import org.jboss.errai.ioc.rebind.ioc.injector.Injector;
import org.jboss.errai.ioc.rebind.ioc.injector.api.QualifiedTypeInjectorProducer;

public class AsyncQualifiedTypeInjectorProducer implements QualifiedTypeInjectorProducer {

  @Override
  public Injector create(MetaClass type, Injector delegate, MetaParameterizedType parameterizedType) {
    return new AsyncQualifiedTypeInjectorDelegate(type, delegate, parameterizedType);
  }

}
