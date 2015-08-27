package org.jboss.errai.ioc.client.container;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public abstract class AbstractContext implements Context {

  private final Map<String, Factory<?>> factories = new HashMap<String, Factory<?>>();
  private final Map<String, Proxy<?>> proxies = new HashMap<String, Proxy<?>>();
  private final Map<Object, Factory<?>> factoriesByCreatedInstances = new IdentityHashMap<Object, Factory<?>>();

  private ContextManager contextManager;

  @Override
  public ContextManager getContextManager() {
    if (contextManager == null) {
      throw new RuntimeException("ContextManager has not been set.");
    }

    return contextManager;
  }

  @Override
  public void setContextManager(final ContextManager contextManager) {
    this.contextManager = contextManager;
  }

  @Override
  public <T> void registerFactory(final Factory<T> factory) {
    factories.put(factory.getHandle().getFactoryName(), factory);
  }

  protected <T> Proxy<T> getOrCreateProxy(final String factoryName) {
    @SuppressWarnings("unchecked")
    Proxy<T> proxy = (Proxy<T>) proxies.get(factoryName);
    if (proxy == null) {
      final Factory<T> factory = getFactory(factoryName);
      proxy = factory.createProxy(this);
      proxies.put(factoryName, proxy);
    }

    return proxy;
  }

  protected <T> Factory<T> getFactory(final String factoryName) {
    @SuppressWarnings("unchecked")
    final Factory<T> factory = (Factory<T>) factories.get(factoryName);
    if (factory == null) {
      throw new RuntimeException("Could not find registered factory " + factoryName);
    }

    return factory;
  }

  @Override
  public Collection<Factory<?>> getAllFactories() {
    return Collections.unmodifiableCollection(factories.values());
  }

  @Override
  public <T> T getNewInstance(final String factoryName) {
    final Factory<T> factory = getFactory(factoryName);
    final Proxy<T> proxy = factory.createProxy(this);

    return proxy.asBeanType();
  }

  protected void registerInstance(Object instance, Factory<?> factory) {
    factoriesByCreatedInstances.put(instance, factory);
  }

  @Override
  public void destroyInstance(final Object instance) {
    final Factory<?> factory = factoriesByCreatedInstances.get(instance);
    if (factory != null) {
      factory.destroyInstance(instance, contextManager);
      factoriesByCreatedInstances.remove(instance);
    }
  }

  @Override
  public boolean isManaged(final Object ref) {
    return factoriesByCreatedInstances.containsKey(ref);
  }

}
