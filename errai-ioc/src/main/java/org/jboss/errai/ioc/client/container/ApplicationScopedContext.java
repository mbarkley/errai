package org.jboss.errai.ioc.client.container;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Singleton;

import org.jboss.errai.ioc.client.api.ScopeContext;

@ScopeContext({ApplicationScoped.class, Singleton.class})
public class ApplicationScopedContext extends AbstractContext {

  private final Map<Class<?>, Object> instances = new HashMap<Class<?>, Object>();

  @Override
  public <T> T getInstance(final Class<? extends RuntimeInjector<T>> injectorType) {
    final Proxy<T> proxy = getOrCreateProxy(injectorType);

    return proxy.asBeanType();
  }

  @Override
  public <T> T getActiveNonProxiedInstance(final Class<? extends RuntimeInjector<T>> injectorType) {
    @SuppressWarnings("unchecked")
    T instance = (T) instances.get(injectorType);
    if (instance == null) {
      instance = getInjector(injectorType).createInstance(getContextManager());
      instances.put(injectorType, instance);
    }

    return instance;
  }

  @Override
  public Class<? extends Annotation> getScope() {
    return ApplicationScoped.class;
  }

  @Override
  public boolean isActive() {
    return true;
  }

}
