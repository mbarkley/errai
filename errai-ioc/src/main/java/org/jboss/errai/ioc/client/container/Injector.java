package org.jboss.errai.ioc.client.container;

public interface Injector<T> {

  T createInstance(ContextManager contextManager);

  Proxy<T> createProxy(Context context);

  InjectorHandle getHandle();

}
