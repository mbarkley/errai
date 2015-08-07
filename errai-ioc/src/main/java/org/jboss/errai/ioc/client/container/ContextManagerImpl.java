/*
 * Copyright 2015 JBoss, by Red Hat, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.errai.ioc.client.container;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @see ContextManager
 * @author Max Barkley <mbarkley@redhat.com>
 */
public class ContextManagerImpl implements ContextManager {

  private final Map<String, Context> contextsByFactoryName = new HashMap<String, Context>();
  private final Collection<Context> contexts = new ArrayList<Context>();

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
