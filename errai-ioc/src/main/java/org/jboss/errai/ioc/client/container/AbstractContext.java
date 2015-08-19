package org.jboss.errai.ioc.client.container;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractContext implements Context {

  private final Map<String, Injector<?>> injectors = new HashMap<String, Injector<?>>();
  private final Map<String, Proxy<?>> proxies = new HashMap<String, Proxy<?>>();

  private ContextManager contextManager;

  protected ContextManager getContextManager() {
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
  public <T> void registerInjector(final Injector<T> injector) {
    injectors.put(injector.getClass().getSimpleName(), injector);
  }

  protected <T> Proxy<T> getOrCreateProxy(final String injectorTypeSimpleName) {
    @SuppressWarnings("unchecked")
    Proxy<T> proxy = (Proxy<T>) proxies.get(injectorTypeSimpleName);
    if (proxy == null) {
      final Injector<T> injector = getInjector(injectorTypeSimpleName);
      proxy = injector.createProxy(this);
    }

    return proxy;
  }

  protected <T> Injector<T> getInjector(final String injectorTypeSimpleName) {
    @SuppressWarnings("unchecked")
    final Injector<T> injector = (Injector<T>) injectors.get(injectorTypeSimpleName);
    if (injector == null) {
      throw new RuntimeException("Could not find registered injector " + injectorTypeSimpleName);
    }

    return injector;
  }

  @Override
  public Collection<Injector<?>> getAllInjectors() {
    return Collections.unmodifiableCollection(injectors.values());
  }

  @Override
  public <T> T getNewInstance(String injecorTypeSimpleName) {
    final Injector<Object> injector = getInjector(injecorTypeSimpleName);
    @SuppressWarnings("unchecked")
    final Proxy<T> proxy = (Proxy<T>) injector.createProxy(this);

    return proxy.asBeanType();
  }

}
