package org.jboss.errai.ioc.client.container;

import java.lang.annotation.Annotation;
import java.util.Set;

public interface InjectorHandle {

  Set<Annotation> getQualifiers();

  Set<Class<?>> getAssignableTypes();

  Class<?> getActualType();

  String getInjectorName();

  Class<? extends Annotation> getScope();

  boolean isEager();

}
