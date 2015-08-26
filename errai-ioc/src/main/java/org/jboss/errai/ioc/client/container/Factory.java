package org.jboss.errai.ioc.client.container;

public interface Factory<T> {

  T createInstance(ContextManager contextManager);

  Proxy<T> createProxy(Context context);

  FactoryHandle getHandle();

  void destroyInstance(Object instance, ContextManager contextManager);

}
