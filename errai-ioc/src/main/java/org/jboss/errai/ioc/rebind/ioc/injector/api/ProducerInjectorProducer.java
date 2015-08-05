package org.jboss.errai.ioc.rebind.ioc.injector.api;

import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaClassMember;
import org.jboss.errai.ioc.rebind.ioc.injector.Injector;

public interface ProducerInjectorProducer extends InjectorProducer {

  Injector create(MetaClass type, MetaClassMember producerMember, InjectableInstance producerInjectableInstance);

}
