package org.jboss.errai.ioc.client.container;

import java.util.Collection;

public interface ContextManager {

  void addContext(Context context);

  <T> T getInstance(String injectorName);

  <T> T getEagerInstance(String injectorName);

  <T> T getNewInstance(String injectorName);

  Collection<InjectorHandle> getAllInjectorHandles();

}
