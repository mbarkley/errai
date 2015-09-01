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
    Map<String, Object> map = referenceMaps.get(maybeUnwrapProxy(instance));
    if (map == null) {
      map = new HashMap<String, Object>();
      referenceMaps.put(instance, map);
    }

    return map;
  }

  @SuppressWarnings("unchecked")
  protected <P> P getReferenceAs(final T instance, final String referenceName, final Class<P> type) {
    return (P) getInstanceRefMap(maybeUnwrapProxy(instance)).get(referenceName);
  }

  public abstract Proxy<T> createProxy(Context context);

  public abstract FactoryHandle getHandle();

  protected <D> D registerDependentScopedReference(final T instance, final D dependentScopedBeanRef) {
    dependentScopedDependencies.put(maybeUnwrapProxy(instance), dependentScopedBeanRef);

    return dependentScopedBeanRef;
  }

  @SuppressWarnings("unchecked")
  public void destroyInstance(final Object instance, final ContextManager contextManager) {
    final Object unwrapped = maybeUnwrapProxy(instance);
    generatedDestroyInstance(unwrapped, contextManager);
    referenceMaps.remove(unwrapped);
    for (final Object depRef : dependentScopedDependencies.get((T) unwrapped)) {
      contextManager.destroy(depRef);
    }
    dependentScopedDependencies.removeAll(instance);
  }

  protected abstract void generatedDestroyInstance(Object instance, ContextManager contextManager);

  @SuppressWarnings("unchecked")
  protected <P> P maybeUnwrapProxy(P instance) {
    if (instance instanceof Proxy) {
      return ((Proxy<P>) instance).unwrappedInstance();
    } else {
      return instance;
    }
  }

}
