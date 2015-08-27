package org.jboss.errai.ioc.client.container;

import java.util.Collection;

public interface ContextManager {

  void addContext(Context context);

  <T> T getInstance(String factoryName);

  <T> T getEagerInstance(String factoryName);

  <T> T getNewInstance(String factoryName);

  Collection<FactoryHandle> getAllFactoryHandles();

  void destroy(Object instance);

  boolean isManaged(Object ref);

}
