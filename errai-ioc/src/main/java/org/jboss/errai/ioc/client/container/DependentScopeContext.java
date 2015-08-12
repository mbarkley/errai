package org.jboss.errai.ioc.client.container;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.Dependent;

import org.jboss.errai.ioc.client.api.ScopeContext;

@ScopeContext({Dependent.class})
public class DependentScopeContext implements Context {

  private ContextManager contextManager;
  private final Map<Class<?>, RuntimeInjector<?>> injectors = new HashMap<Class<?>, RuntimeInjector<?>>();

  @Override
  public void setContextManager(final ContextManager contextManager) {
    this.contextManager = contextManager;
  }

  @Override
  public <T> void registerInjector(final RuntimeInjector<T> injector) {
    injectors.put(injector.getClass(), injector);
  }

  @Override
  public <T> T getInstance(final Class<? extends RuntimeInjector<T>> injectorType) {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
  }

  @Override
  public <T> T getActiveNonProxiedInstance(Class<? extends RuntimeInjector<T>> injectorType) {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
  }

  @Override
  public Class<? extends Annotation> getScope() {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
  }

  @Override
  public boolean isActive() {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
  }

}
