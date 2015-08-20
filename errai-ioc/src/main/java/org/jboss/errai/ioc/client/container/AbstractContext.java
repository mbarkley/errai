package org.jboss.errai.ioc.client.container;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractContext implements Context {

  private final Map<String, Injector<?>> injectors = new HashMap<String, Injector<?>>();
  private final Map<String, Proxy<?>> proxies = new HashMap<String, Proxy<?>>();

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
  public <T> void registerInjector(final Injector<T> injector) {
    injectors.put(injector.getHandle().getInjectorName(), injector);
  }

  protected <T> Proxy<T> getOrCreateProxy(final String injectorName) {
    @SuppressWarnings("unchecked")
    Proxy<T> proxy = (Proxy<T>) proxies.get(injectorName);
    if (proxy == null) {
      final Injector<T> injector = getInjector(injectorName);
      proxy = injector.createProxy(this);
      proxies.put(injectorName, proxy);
    }

    return proxy;
  }

  protected <T> Injector<T> getInjector(final String injectorName) {
    @SuppressWarnings("unchecked")
    final Injector<T> injector = (Injector<T>) injectors.get(injectorName);
    if (injector == null) {
      throw new RuntimeException("Could not find registered injector " + injectorName);
    }

    return injector;
  }

  @Override
  public Collection<Injector<?>> getAllInjectors() {
    return Collections.unmodifiableCollection(injectors.values());
  }

  @Override
  public <T> T getNewInstance(final String injecorTypeSimpleName) {
    final Injector<T> injector = getInjector(injecorTypeSimpleName);
    final Proxy<T> proxy = injector.createProxy(this);

    return proxy.asBeanType();
  }

}
