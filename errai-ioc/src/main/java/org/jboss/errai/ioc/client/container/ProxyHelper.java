package org.jboss.errai.ioc.client.container;

public interface ProxyHelper<T> {

  void setInstance(T instance);

  T getInstance();

  void clearInstance();

  void setContext(Context context);

  Context getContext();
}
