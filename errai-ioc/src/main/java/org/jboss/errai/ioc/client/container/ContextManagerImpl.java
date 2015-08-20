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
  public <T> T getInstance(final String injectorName) {
    return contextsByInjectorName.get(injectorName).getInstance(injectorName);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T getEagerInstance(final String injectorName) {
    final Context context = contextsByInjectorName.get(injectorName);
    final T instance = context.<T>getInstance(injectorName);
    if ((instance instanceof Proxy) && !(instance instanceof NonProxiableWrapper)) {
      final T nonProxiedInstance = context.<T>getActiveNonProxiedInstance(injectorName);
      ((Proxy<T>) instance).setInstance(nonProxiedInstance);
    }

    return instance;
  }

  @Override
  public <T> T getNewInstance(final String injectorName) {
    return contextsByInjectorName.get(injectorName).getNewInstance(injectorName);
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
