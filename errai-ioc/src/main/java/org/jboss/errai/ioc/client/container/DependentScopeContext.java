package org.jboss.errai.ioc.client.container;

import java.lang.annotation.Annotation;

import javax.enterprise.context.Dependent;

import org.jboss.errai.ioc.client.api.ScopeContext;

/**
 * The {@link Context} implementation for all {@link Dependent} scoped beans.
 * Unlike other scopes, beans which have no explicit scope will be considered
 * dependent. Therefore some beans will be registered with this scope that do
 * not actually have the {@link Dependent} annotation.
 *
 * @author Max Barkley <mbarkley@redhat.com>
 */
@ScopeContext({Dependent.class})
public class DependentScopeContext extends AbstractContext {

  @Override
  public <T> T getInstance(final String factoryName) {
    final Factory<T> factory = this.<T>getFactory(factoryName);
    final Proxy<T> proxy = factory.createProxy(this);
    final T instance;
    if (proxy == null) {
      instance = getActiveNonProxiedInstance(factoryName);
    } else {
      instance = proxy.asBeanType();
    }
    return instance;
  }

  @Override
  public <T> T getActiveNonProxiedInstance(final String factoryName) {
    final Factory<T> factory = this.<T>getFactory(factoryName);
    final T instance = factory.createInstance(getContextManager());
    registerInstance(instance, factory);
    factory.invokePostConstructs(instance);

    return instance;
  }

  @Override
  public Class<? extends Annotation> getScope() {
    return Dependent.class;
  }

  @Override
  public boolean isActive() {
    return true;
  }

}
