package org.jboss.errai.ioc.client.container;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Singleton;

import org.jboss.errai.ioc.client.api.EntryPoint;
import org.jboss.errai.ioc.client.api.ScopeContext;

@ScopeContext({ApplicationScoped.class, Singleton.class, EntryPoint.class})
public class ApplicationScopedContext extends AbstractContext {

  private final Map<String, Object> instances = new HashMap<String, Object>();

  @Override
  public <T> T getInstance(final String factoryName) {
    final Proxy<T> proxy = getOrCreateProxy(factoryName);
    if (proxy == null) {
      return getActiveNonProxiedInstance(factoryName);
    } else {
      return proxy.asBeanType();
    }
  }

  @Override
  public <T> T getActiveNonProxiedInstance(final String factoryName) {
    @SuppressWarnings("unchecked")
    T instance = (T) instances.get(factoryName);
    if (instance == null) {
      final Factory<T> factory = this.<T>getFactory(factoryName);
      instance = factory.createInstance(getContextManager());
      instances.put(factoryName, instance);
      registerInstance(instance, factory);
      factory.invokePostConstructs(instance);
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
