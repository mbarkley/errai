package org.jboss.errai.ioc.client.container;

public class ProxyHelperImpl<T> implements ProxyHelper<T> {

  private final String injectorTypeSimpleName;

  private Context context;
  private T instance;

  public ProxyHelperImpl(final String injectorTypeSimpleName) {
    this.injectorTypeSimpleName = injectorTypeSimpleName;
  }

  @Override
  public void setInstance(final T instance) {
    this.instance = instance;
  }

  @Override
  public T getInstance() {
    if (instance == null) {
      trySettingInstance();
    }

    return instance;
  }

  private void trySettingInstance() {
    assertContextIsSet();

    if (context.isActive()) {
      instance = context.getActiveNonProxiedInstance(injectorTypeSimpleName);
    } else {
      throw new RuntimeException("Cannot invoke method on bean from inactive " + context.getScope().getSimpleName() + " context.");
    }
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
    assertContextIsSet();

    return context;
  }

  private void assertContextIsSet() {
    if (context == null) {
      throw new RuntimeException("Context has not yet been set.");
    }
  }

}