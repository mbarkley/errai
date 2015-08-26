package org.jboss.errai.ioc.client.container;

import java.util.HashMap;
import java.util.Map;

public abstract class Factory<T> {

  private final Map<T, Map<String, Object>> referenceMaps = new HashMap<T, Map<String,Object>>();

  public abstract T createInstance(ContextManager contextManager);

  protected void setReference(final T instance, final String referenceName, final Object ref) {
    final Map<String, Object> instanceRefMap = getInstanceRefMap(instance);
    instanceRefMap.put(referenceName, ref);
  }

  private Map<String, Object> getInstanceRefMap(final T instance) {
    Map<String, Object> map = referenceMaps.get(instance);
    if (map == null) {
      map = new HashMap<String, Object>();
      referenceMaps.put(instance, map);
    }

    return map;
  }

  @SuppressWarnings("unchecked")
  protected <P> P getReferenceAs(final T instance, final String referenceName, final Class<P> type) {
    return (P) getInstanceRefMap(instance).get(referenceName);
  }

  public abstract Proxy<T> createProxy(Context context);

  public abstract FactoryHandle getHandle();

  public void destroyInstance(Object instance, ContextManager contextManager) {
    generatedDestroyInstance(instance, contextManager);
    referenceMaps.remove(instance);
  }

  protected abstract void generatedDestroyInstance(Object instance, ContextManager contextManager);

}
