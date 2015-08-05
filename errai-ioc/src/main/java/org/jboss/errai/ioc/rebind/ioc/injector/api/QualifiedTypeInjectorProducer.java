package org.jboss.errai.ioc.rebind.ioc.injector.api;

import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaParameterizedType;
import org.jboss.errai.ioc.rebind.ioc.injector.Injector;

public interface QualifiedTypeInjectorProducer extends InjectorProducer {

  Injector create(MetaClass type, Injector delegate, MetaParameterizedType parameterizedType);

}
