package org.jboss.errai.ioc.client.container;

import java.lang.annotation.Annotation;
import java.util.Collection;

public interface Context {

  void setContextManager(ContextManager contextManager);

  ContextManager getContextManager();

  <T> void registerInjector(Injector<T> injector);

  <T> T getInstance(String injectorName);

  <T> T getActiveNonProxiedInstance(String injectorName);

  Class<? extends Annotation> getScope();

  boolean isActive();

  Collection<Injector<?>> getAllInjectors();

  <T> T getNewInstance(String injecorName);

}
