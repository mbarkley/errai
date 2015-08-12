package org.jboss.errai.ioc.client.container;

public interface Proxy<T> {

  T asBeanType();

  void setInstance(T instance);

  void clearInstance();

  void setContext(Context context);

}
