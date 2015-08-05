package org.jboss.errai.ioc.rebind.ioc.injector.async;

import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaClassMember;
import org.jboss.errai.ioc.rebind.ioc.injector.Injector;
import org.jboss.errai.ioc.rebind.ioc.injector.api.InjectableInstance;
import org.jboss.errai.ioc.rebind.ioc.injector.api.ProducerInjectorProducer;

public class AsyncProducerInjectorProducer implements ProducerInjectorProducer {

  @Override
  public Injector create(MetaClass type, MetaClassMember producerMember, InjectableInstance producerInjectableInstance) {
    return new AsyncProducerInjector(type, producerMember, producerInjectableInstance);
  }

}
