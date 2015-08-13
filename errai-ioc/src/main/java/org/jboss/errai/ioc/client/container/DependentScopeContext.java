package org.jboss.errai.ioc.client.container;

import java.lang.annotation.Annotation;

import javax.enterprise.context.Dependent;

import org.jboss.errai.ioc.client.api.ScopeContext;

@ScopeContext({Dependent.class})
public class DependentScopeContext extends AbstractContext {

  @Override
  public <T> T getInstance(final Class<? extends Injector<T>> injectorType) {
    return getInjector(injectorType).createInstance(getContextManager());
  }

  @Override
  public <T> T getActiveNonProxiedInstance(final Class<? extends Injector<T>> injectorType) {
    throw new RuntimeException("This method should never be called on the DependentScopeContext because it's beans are not proxied.");
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
