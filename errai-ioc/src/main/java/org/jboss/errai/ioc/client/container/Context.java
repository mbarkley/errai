package org.jboss.errai.ioc.client.container;

import java.lang.annotation.Annotation;
import java.util.Collection;

public interface Context {

  void setContextManager(ContextManager contextManager);

  ContextManager getContextManager();

  <T> void registerFactory(Factory<T> factory);

  <T> T getInstance(String factoryName);

  void destroyInstance(Object instance);

  <T> T getActiveNonProxiedInstance(String factoryName);

  Class<? extends Annotation> getScope();

  boolean isActive();

  Collection<Factory<?>> getAllFactories();

  <T> T getNewInstance(String factoryName);

  boolean isManaged(Object ref);

  boolean addDestructionCallback(Object instance, DestructionCallback<?> callback);

}
