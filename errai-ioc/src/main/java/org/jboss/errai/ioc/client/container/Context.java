package org.jboss.errai.ioc.client.container;

import java.lang.annotation.Annotation;
import java.util.Collection;

public interface Context {

  void setContextManager(ContextManager contextManager);

  <T> void registerInjector(RuntimeInjector<T> injector);

  <T> T getInstance(Class<? extends RuntimeInjector<T>> injectorType);

  <T> T getActiveNonProxiedInstance(Class<? extends RuntimeInjector<T>> injectorType);

  Class<? extends Annotation> getScope();

  boolean isActive();

  Collection<RuntimeInjector<?>> getAllInjectors();

}
