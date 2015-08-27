package org.jboss.errai.ioc.client.container;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Supplier;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;

public abstract class Factory<T> {

  private final Map<T, Map<String, Object>> referenceMaps = new HashMap<T, Map<String,Object>>();
  private final SetMultimap<T, Object> dependentScopedDependencies = Multimaps
          .newSetMultimap(new IdentityHashMap<T, Collection<Object>>(), new Supplier<Set<Object>>() {
            @Override
            public Set<Object> get() {
              return Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
            }
          });

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

  protected void registerDependentScopedReference(final T instance, final Object dependentScopedBeanRef) {
    dependentScopedDependencies.put(instance, dependentScopedBeanRef);
  }

  @SuppressWarnings("unchecked")
  public void destroyInstance(final Object instance, final ContextManager contextManager) {
    generatedDestroyInstance(instance, contextManager);
    referenceMaps.remove(instance);
    for (final Object depRef : dependentScopedDependencies.get((T) instance)) {
      contextManager.destroy(depRef);
    }
    dependentScopedDependencies.removeAll(instance);
  }

  protected abstract void generatedDestroyInstance(Object instance, ContextManager contextManager);

}
