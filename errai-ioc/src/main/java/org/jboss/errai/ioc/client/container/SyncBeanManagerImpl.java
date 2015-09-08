/*
 * Copyright 2011 JBoss, by Red Hat, Inc
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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Default;

import org.jboss.errai.ioc.client.QualifierUtil;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;

/**
 * A simple bean manager provided by the Errai IOC framework. The manager provides access to all of the wired beans
 * and their instances. Since the actual wiring code is generated, the bean manager is populated by the generated
 * code at bootstrap time.
 *
 * @author Mike Brock
 */
@Alternative
public class SyncBeanManagerImpl implements SyncBeanManager {

  private static final Default DEFAULT = new Default() {

    @Override
    public Class<? extends Annotation> annotationType() {
      return Default.class;
    }
  };

  private ContextManager contextManager;
  private final Multimap<String, FactoryHandle> handlesByTypeName = ArrayListMultimap.create();
  private final ListMultimap<String, Class<?>> typesByName = ArrayListMultimap.create();

  @Override
  public void destroyBean(Object ref) {
    contextManager.destroy(ref);
  }

  @Override
  public boolean isManaged(Object ref) {
    return contextManager.isManaged(ref);
  }

  @Override
  public Object getActualBeanReference(Object ref) {
    if (isProxyReference(ref)) {
      return ((Proxy<?>) ref).unwrappedInstance();
    } else {
      return ref;
    }
  }

  @Override
  public void addProxyReference(Object proxyRef, Object realRef) {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
  }

  @Override
  public boolean isProxyReference(Object ref) {
    return ref instanceof Proxy;
  }

  @Override
  public boolean addDestructionCallback(Object beanInstance, DestructionCallback<?> destructionCallback) {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
  }

  @Override
  public void destroyAllBeans() {
    // TODO Decide how this should be implemented.
  }

  @Override
  public void setContextManager(final ContextManager contextManager) {
    if (this.contextManager != null) {
      throw new RuntimeException("The ContextManager must only be set once.");
    }
    this.contextManager = contextManager;
    init();
  }

  private void init() {
    final Collection<FactoryHandle> eager = new ArrayList<FactoryHandle>();
    for (final FactoryHandle handle : contextManager.getAllFactoryHandles()) {
      if (handle.isEager()) {
        eager.add(handle);
      }
      for (final Class<?> assignableType : handle.getAssignableTypes()) {
        handlesByTypeName.put(assignableType.getName(), handle);
        typesByName.put(assignableType.getName(), assignableType);
      }
      typesByName.put(handle.getBeanName(), handle.getActualType());
    }

    for (final FactoryHandle handle : eager) {
      contextManager.getEagerInstance(handle.getFactoryName());
    }
  }

  @SuppressWarnings({ "rawtypes" })
  @Override
  public Collection<IOCBeanDef> lookupBeans(final String name) {
    final Collection<IOCBeanDef> beanDefs = new ArrayList<IOCBeanDef>();
    for (final Class<?> type : typesByName.get(name)) {
      beanDefs.addAll(lookupBeans(type));
    }

    return beanDefs;
  }

  @Override
  public <T> Collection<IOCBeanDef<T>> lookupBeans(final Class<T> type) {
    final Collection<FactoryHandle> handles = handlesByTypeName.get(type.getName());
    final Collection<IOCBeanDef<T>> beanDefs = new ArrayList<IOCBeanDef<T>>(handles.size());

    for (final FactoryHandle handle : handles) {
      beanDefs.add(new IOCBeanDefImplementation<T>(handle, handle.getBeanName(), type));
    }

    return beanDefs;
  }

  @Override
  public <T> Collection<IOCBeanDef<T>> lookupBeans(Class<T> type, Annotation... qualifiers) {
    final Set<Annotation> qualifierSet = new HashSet<Annotation>(Arrays.asList(qualifiers));
    final Collection<IOCBeanDef<T>> candidates = lookupBeans(type);
    final Iterator<IOCBeanDef<T>> iter = candidates.iterator();
    while (iter.hasNext()) {
      final IOCBeanDef<T> beanDef = iter.next();
      if (!beanDef.matches(qualifierSet)) {
        iter.remove();
      }
    }

    return candidates;
  }

  @Override
  public <T> IOCBeanDef<T> lookupBean(Class<T> type, Annotation... qualifiers) {
    if (qualifiers.length == 0) {
      qualifiers = new Annotation[] { DEFAULT };
    }
    final Collection<IOCBeanDef<T>> resolved = lookupBeans(type, qualifiers);
    if (resolved.isEmpty()) {
      throw new IOCResolutionException("No beans matched " + type.getName() + " with qualifiers " + qualifiers);
    } else if (resolved.size() > 1) {
      final StringBuilder builder = new StringBuilder();
      builder.append("Multiple beans matched " + type.getName() + " with qualifiers " + qualifiers + "\n")
             .append("Found:\n");
      for (final IOCBeanDef<T> beanDef : resolved) {
        builder.append("  ")
               .append(beanDef.toString())
               .append("\n");
      }
      throw new IOCResolutionException(builder.toString());
    } else {
      return resolved.iterator().next();
    }
  }

  /**
   * For testing only.
   */
  public void reset() {
    contextManager = null;
    typesByName.clear();
    handlesByTypeName.clear();
  }

  private final class IOCBeanDefImplementation<T> implements IOCBeanDef<T> {
    private final FactoryHandle handle;
    private final String name;
    private final Class<T> type;

    private IOCBeanDefImplementation(FactoryHandle handle, String name, Class<T> type) {
      this.handle = handle;
      this.name = name;
      this.type = type;
    }

    @Override
    public String toString() {
      return "[type=" + type.getName() + ", qualifiers=" + handle.getQualifiers() + "]";
    }

    @Override
    public Class<T> getType() {
      return type;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<T> getBeanClass() {
      return (Class<T>) handle.getActualType();
    }

    @Override
    public Class<? extends Annotation> getScope() {
      return handle.getScope();
    }

    @Override
    public T getInstance() {
      final T instance = contextManager.getInstance(handle.getFactoryName());
      if (instance instanceof Proxy) {
        @SuppressWarnings("unchecked")
        final Proxy<T> proxy = (Proxy<T>) instance;
        // Forces bean to be loaded.
        proxy.unwrappedInstance();
      }

      return instance;
    }

    @Override
    public T newInstance() {
      return contextManager.getNewInstance(handle.getFactoryName());
    }

    @Override
    public Set<Annotation> getQualifiers() {
      return handle.getQualifiers();
    }

    @Override
    public boolean matches(Set<Annotation> annotations) {
      return QualifierUtil.matches(annotations, handle.getQualifiers());
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public boolean isConcrete() {
      // TODO Auto-generated method stub
      throw new RuntimeException("Not yet implemented.");
    }

    @Override
    public boolean isActivated() {
      final Class<? extends BeanActivator> activatorType = handle.getBeanActivatorType();
      if (activatorType == null) {
        return true;
      } else {
        final BeanActivator activator = lookupBean(activatorType).getInstance();
        return activator.isActivated();
      }
    }
  }
}
