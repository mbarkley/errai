package org.jboss.errai.ioc.client.container;

public class NonProxiableWrapper<T> implements Proxy<T> {

  private final T instance;

  public NonProxiableWrapper(T instance) {
    this.instance = instance;
  }

  @Override
  public T asBeanType() {
    return instance;
  }

  @Override
  public void setInstance(T instance) {
    throw new RuntimeException("Cannot set instance for a non-proxiable type.");
  }

  @Override
  public void clearInstance() {
    throw new RuntimeException("Cannot clear instance for a non-proxiable type.");
  }

  @Override
  public void setContext(Context ignore) {
    // No-op since non-proxied type cannot protect from being referenced from outside of active scope.
  }

  @Override
  public T unwrappedInstance() {
    return instance;
  }

}
