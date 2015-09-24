package org.jboss.errai.ioc.client.container;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Singleton;

import org.jboss.errai.ioc.client.api.EntryPoint;
import org.jboss.errai.ioc.client.api.ScopeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of {@link Context} for singleton beans. All calls to
 * {@link #getInstance(String)} will return the same bean instance.
 *
 * @author Max Barkley <mbarkley@redhat.com>
 */
@ScopeContext({ApplicationScoped.class, Singleton.class, EntryPoint.class})
public class ApplicationScopedContext extends AbstractContext {

  private static final Logger logger = LoggerFactory.getLogger(ApplicationScopedContext.class);

  private final Map<String, Object> instances = new HashMap<String, Object>();

  @Override
  public <T> T getInstance(final String factoryName) {
    final Proxy<T> proxy = getOrCreateProxy(factoryName);
    if (proxy == null) {
      return getActiveNonProxiedInstance(factoryName);
    } else {
      return proxy.asBeanType();
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T getActiveNonProxiedInstance(final String factoryName) {
    if (instances.containsKey(factoryName)) {
      return (T) instances.get(factoryName);
    } else if (isCurrentlyCreatingInstance(factoryName)) {
      final Factory<T> factory = this.<T>getFactory(factoryName);
      final T incomplete = factory.getIncompleteInstance();
      if (incomplete == null) {
        throw new RuntimeException("Could not obtain an incomplete instance of " + factory.getHandle().getActualType().getName() + " to break a circular injection.");
      } else {
        logger.warn("An incomplete " + factory.getHandle().getActualType() + " was required to break a circular injection.");
        return incomplete;
      }
    } else {
      return createNewUnproxiedInstance(factoryName);
    }
  }

  private <T> T createNewUnproxiedInstance(final String factoryName) {
    final Factory<T> factory = this.<T>getFactory(factoryName);
    beforeCreateInstance(factoryName);
    final T instance = factory.createInstance(getContextManager());
    afterCreateInstance(factoryName);
    instances.put(factoryName, instance);
    registerInstance(instance, factory);
    factory.invokePostConstructs(instance);
    return instance;
  }

  @Override
  public Class<? extends Annotation> getScope() {
    return ApplicationScoped.class;
  }

  @Override
  public boolean isActive() {
    return true;
  }

}
