package org.jboss.errai.ioc.client.container;

public interface ContextManager {

  void addContext(Context context);

  <T> T getInstance(Class<? extends RuntimeInjector<T>> injectorType);

}
