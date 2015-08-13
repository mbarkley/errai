package org.jboss.errai.ioc.client.container;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Singleton;

import org.jboss.errai.ioc.client.api.ScopeContext;

@ScopeContext({ApplicationScoped.class, Singleton.class})
public class ApplicationScopedContext extends AbstractContext {

  private final Map<String, Object> instances = new HashMap<String, Object>();

  @Override
  public <T> T getInstance(final String injectorTypeSimpleName) {
    final Proxy<T> proxy = getOrCreateProxy(injectorTypeSimpleName);

    return proxy.asBeanType();
  }

  @Override
  public <T> T getActiveNonProxiedInstance(final String injectorTypeSimpleName) {
    @SuppressWarnings("unchecked")
    T instance = (T) instances.get(injectorTypeSimpleName);
    if (instance == null) {
      instance = this.<T>getInjector(injectorTypeSimpleName).createInstance(getContextManager());
      instances.put(injectorTypeSimpleName, instance);
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
