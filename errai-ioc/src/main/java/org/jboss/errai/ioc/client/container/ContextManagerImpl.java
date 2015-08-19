package org.jboss.errai.ioc.client.container;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class ContextManagerImpl implements ContextManager {

  private final Map<String, Context> contextsByInjectorName = new HashMap<String, Context>();
  // XXX bug in bootsrapper generator adds same context multiple times.
  private final Collection<Context> contexts = new HashSet<Context>();

  @Override
  public void addContext(final Context context) {
    if (!contexts.contains(context)) {
      contexts.add(context);
      context.setContextManager(this);
      for (final Injector<?> injector : context.getAllInjectors()) {
        contextsByInjectorName.put(injector.getHandle().getInjectorName(), context);
      }
    }
  }

  @Override
  public <T> T getInstance(final String injectorTypeSimpleName) {
    return contextsByInjectorName.get(injectorTypeSimpleName).getInstance(injectorTypeSimpleName);
  }

  @Override
  public <T> T getNewInstance(final String injecorTypeSimpleName) {
    return contextsByInjectorName.get(injecorTypeSimpleName).getNewInstance(injecorTypeSimpleName);
  }

  @Override
  public Collection<InjectorHandle> getAllInjectorHandles() {
    final Collection<InjectorHandle> allHandles = new ArrayList<InjectorHandle>();
    for (final Context context : contexts) {
      for (final Injector<?> injector : context.getAllInjectors()) {
        allHandles.add(injector.getHandle());
      }
    }

    return allHandles;
  }

}
