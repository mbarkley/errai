package org.jboss.errai.ioc.client.container;

import java.lang.annotation.Annotation;
import java.util.Collection;

public interface Context {

  void setContextManager(ContextManager contextManager);

  <T> void registerInjector(Injector<T> injector);

  <T> T getInstance(String injectorTypeSimpleName);

  <T> T getActiveNonProxiedInstance(String injectorTypeSimpleName);

  Class<? extends Annotation> getScope();

  boolean isActive();

  Collection<Injector<?>> getAllInjectors();

}