package org.jboss.errai.ioc.client.container;

import java.lang.annotation.Annotation;
import java.util.Set;

public interface FactoryHandle {

  Set<Annotation> getQualifiers();

  Set<Class<?>> getAssignableTypes();

  Class<?> getActualType();

  String getFactoryName();

  Class<? extends Annotation> getScope();

  boolean isEager();

  Class<? extends BeanActivator> getBeanActivatorType();

  String getBeanName();

}
