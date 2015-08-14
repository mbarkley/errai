package org.jboss.errai.ioc.client.container;

public class ProxyHelperImpl<T> implements ProxyHelper<T> {

  private Context context;
  private T instance;

  @Override
  public void setInstance(final T instance) {
    this.instance = instance;
  }

  @Override
  public T getInstance() {
    if (instance == null) {
      // TODO improve message
      throw new RuntimeException("There is no active instance.");
    }

    return instance;
  }

  @Override
  public void clearInstance() {
    instance = null;
  }

  @Override
  public void setContext(final Context context) {
    if (context != null) {
      throw new RuntimeException("Context can only be set once.");
    }

    this.context = context;
  }

  @Override
  public Context getContext() {
    if (context == null) {
      throw new RuntimeException("Context has not yet been set.");
    }

    return context;
  }

}
