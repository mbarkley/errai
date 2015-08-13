package org.jboss.errai.ioc.client.container;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractContext implements Context {

  private final Map<Class<?>, Injector<?>> injectors = new HashMap<Class<?>, Injector<?>>();
  private final Map<Class<?>, Proxy<?>> proxies = new HashMap<Class<?>, Proxy<?>>();

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
    injectors.put(injector.getClass(), injector);
  }

  protected <T> Proxy<T> getOrCreateProxy(final Class<? extends Injector<T>> injectorType) {
    @SuppressWarnings("unchecked")
    Proxy<T> proxy = (Proxy<T>) proxies.get(injectorType);
    if (proxy == null) {
      final Injector<T> injector = getInjector(injectorType);
      proxy = injector.createProxy();
      proxy.setContext(this);
    }

    return proxy;
  }

  protected <T> Injector<T> getInjector(final Class<? extends Injector<T>> injectorType) {
    @SuppressWarnings("unchecked")
    final Injector<T> injector = (Injector<T>) injectors.get(injectorType);
    if (injector == null) {
      throw new RuntimeException("Could not find registered injector " + injectorType.getName());
    }

    return injector;
  }

  @Override
  public Collection<Injector<?>> getAllInjectors() {
    return Collections.unmodifiableCollection(injectors.values());
  }

}
