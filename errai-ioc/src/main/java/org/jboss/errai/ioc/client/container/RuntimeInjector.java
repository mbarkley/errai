package org.jboss.errai.ioc.client.container;

public interface RuntimeInjector<T> {

  T createInstance(ContextManager contextManager);

  Proxy<T> createProxy();

}
