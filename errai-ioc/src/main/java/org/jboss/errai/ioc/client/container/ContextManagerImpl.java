package org.jboss.errai.ioc.client.container;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * @see ContextManager
 * @author Max Barkley <mbarkley@redhat.com>
 */
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
    if ((instance instanceof Proxy)) {
      ((Proxy<T>) instance).unwrappedInstance();
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

  @Override
  public void destroy(final Object instance) {
    for (final Context context : contexts) {
      context.destroyInstance(instance);
    }
  }

  @Override
  public boolean isManaged(final Object ref) {
    for (final Context context : contexts) {
      if (context.isManaged(ref)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public boolean addDestructionCallback(Object instance, DestructionCallback<?> callback) {
    boolean success = false;
    for (final Context context : contexts) {
      success = success || context.addDestructionCallback(instance, callback);
    }

    return success;
  }

  @Override
  public <P> P getInstanceProperty(final Object instance, final String propertyName, final Class<P> type) {
    for (final Context context : contexts) {
      if (context.isManaged(instance)) {
        return context.getInstanceProperty(instance, propertyName, type);
      }
    }

    throw new RuntimeException("The given instance, " + instance + ", is not managed.");
  }

  @Override
  public void finishInit() {
    for (final Context context : contexts) {
      for (final Factory<?> factory : context.getAllFactories()) {
        factory.init(context);
      }
    }
  }

}
