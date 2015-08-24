package org.jboss.errai.ioc.client.container;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class ContextManagerImpl implements ContextManager {

  private final Map<String, Context> contextsByFactoryName = new HashMap<String, Context>();
  // XXX bug in bootsrapper generator adds same context multiple times.
  private final Collection<Context> contexts = new HashSet<Context>();

  @Override
  public void addContext(final Context context) {
    if (!contexts.contains(context)) {
      contexts.add(context);
      context.setContextManager(this);
      for (final Factory<?> factory : context.getAllFactories()) {
        contextsByFactoryName.put(factory.getHandle().getFactoryName(), context);
      }
    }
  }

  @Override
  public <T> T getInstance(final String factoryName) {
    return contextsByFactoryName.get(factoryName).getInstance(factoryName);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T getEagerInstance(final String factoryName) {
    final Context context = contextsByFactoryName.get(factoryName);
    final T instance = context.<T>getInstance(factoryName);
    if ((instance instanceof Proxy) && !(instance instanceof NonProxiableWrapper)) {
      final T nonProxiedInstance = context.<T>getActiveNonProxiedInstance(factoryName);
      ((Proxy<T>) instance).setInstance(nonProxiedInstance);
    }

    return instance;
  }

  @Override
  public <T> T getNewInstance(final String factoryName) {
    return contextsByFactoryName.get(factoryName).getNewInstance(factoryName);
  }

  @Override
  public Collection<FactoryHandle> getAllFactoryHandles() {
    final Collection<FactoryHandle> allHandles = new ArrayList<FactoryHandle>();
    for (final Context context : contexts) {
      for (final Factory<?> factory : context.getAllFactories()) {
        allHandles.add(factory.getHandle());
      }
    }

    return allHandles;
  }

}
