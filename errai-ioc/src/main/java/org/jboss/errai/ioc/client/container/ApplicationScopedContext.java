package org.jboss.errai.ioc.client.container;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Singleton;

import org.jboss.errai.ioc.client.api.ScopeContext;

@ScopeContext({ApplicationScoped.class, Singleton.class})
public class ApplicationScopedContext implements Context {

  private final Map<Class<?>, RuntimeInjector<?>> injectors = new HashMap<Class<?>, RuntimeInjector<?>>();
  private final Map<Class<?>, Object> instances = new HashMap<Class<?>, Object>();
  private final Map<Class<?>, Proxy<?>> proxies = new HashMap<Class<?>, Proxy<?>>();

  private ContextManager contextManager;

  @Override
  public <T> void registerInjector(final RuntimeInjector<T> injector) {
    injectors.put(injector.getClass(), injector);
  }

  @Override
  public <T> T getInstance(final Class<? extends RuntimeInjector<T>> injectorType) {
    final Proxy<T> proxy = getOrCreateProxy(injectorType);

    return proxy.asBeanType();
  }

  private <T> Proxy<T> getOrCreateProxy(final Class<? extends RuntimeInjector<T>> injectorType) {
    @SuppressWarnings("unchecked")
    Proxy<T> proxy = (Proxy<T>) proxies.get(injectorType);
    if (proxy == null) {
      final RuntimeInjector<T> injector = getInjector(injectorType);
      proxy = injector.createProxy();
      proxy.setContext(this);
    }

    return proxy;
  }

  @Override
  public <T> T getActiveNonProxiedInstance(final Class<? extends RuntimeInjector<T>> injectorType) {
    @SuppressWarnings("unchecked")
    T instance = (T) instances.get(injectorType);
    if (instance == null) {
      instance = getInjector(injectorType).createInstance(contextManager);
      instances.put(injectorType, instance);
    }

    return instance;
  }

  private <T> RuntimeInjector<T> getInjector(final Class<? extends RuntimeInjector<T>> injectorType) {
    @SuppressWarnings("unchecked")
    final RuntimeInjector<T> injector = (RuntimeInjector<T>) injectors.get(injectorType);
    if (injector == null) {
      throw new RuntimeException("Could not find registered injector " + injectorType.getName());
    }

    return injector;
  }

  @Override
  public Class<? extends Annotation> getScope() {
    return ApplicationScoped.class;
  }

  @Override
  public boolean isActive() {
    return true;
  }

  @Override
  public void setContextManager(final ContextManager contextManager) {
    this.contextManager = contextManager;
  }

}
