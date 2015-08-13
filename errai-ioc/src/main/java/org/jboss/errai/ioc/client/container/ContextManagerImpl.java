package org.jboss.errai.ioc.client.container;

import java.util.HashMap;
import java.util.Map;

public class ContextManagerImpl implements ContextManager {

  private final Map<String, Context> contextsByInjectorName = new HashMap<String, Context>();

  @Override
  public void addContext(final Context context) {
    for (final Injector<?> injector : context.getAllInjectors()) {
      contextsByInjectorName.put(injector.getClass().getSimpleName(), context);
    }
  }

  @Override
  public <T> T getInstance(final String injectorTypeSimpleName) {
    return contextsByInjectorName.get(injectorTypeSimpleName).getInstance(injectorTypeSimpleName);
  }

}
