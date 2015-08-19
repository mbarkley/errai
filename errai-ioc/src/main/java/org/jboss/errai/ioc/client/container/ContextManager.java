package org.jboss.errai.ioc.client.container;

import java.util.Collection;

public interface ContextManager {

  void addContext(Context context);

  <T> T getInstance(String injectorTypeSimpleName);

  <T> T getNewInstance(String injecorTypeSimpleName);

  Collection<InjectorHandle> getAllInjectorHandles();

}
