package org.jboss.errai.ioc.client.container;

import java.util.HashMap;
import java.util.Map;

public class ContextManagerImpl implements ContextManager {

  private final Map<Class<?>, Context> contextsByInjector = new HashMap<Class<?>, Context>();

  @Override
  public void addContext(final Context context) {
    for (final RuntimeInjector<?> injector : context.getAllInjectors()) {
      contextsByInjector.put(injector.getClass(), context);
    }
  }

  @Override
  public <T> T getInstance(final Class<? extends RuntimeInjector<T>> injectorType) {
    return contextsByInjector.get(injectorType).getInstance(injectorType);
  }

}
