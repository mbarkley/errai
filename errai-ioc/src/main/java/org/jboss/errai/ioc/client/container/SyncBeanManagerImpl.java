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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.enterprise.inject.Alternative;

import com.google.gwt.thirdparty.guava.common.collect.ArrayListMultimap;
import com.google.gwt.thirdparty.guava.common.collect.Multimap;

/**
 * A simple bean manager provided by the Errai IOC framework. The manager provides access to all of the wired beans
 * and their instances. Since the actual wiring code is generated, the bean manager is populated by the generated
 * code at bootstrap time.
 *
 * @author Mike Brock
 */
@Alternative
public class SyncBeanManagerImpl implements SyncBeanManager, SyncBeanManagerSetup {

  private final class IOCBeanDefImplementation<T> implements IOCBeanDef<T> {
    private final InjectorHandle handle;
    private final String name;
    private final Class<T> type;

    private IOCBeanDefImplementation(InjectorHandle handle, String name, Class<T> type) {
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
      return contextManager.getInstance(handle.getInjectorName());
    }

    @Override
    public T getInstance(CreationalContext context) {
      return getInstance();
    }

    @Override
    public T newInstance() {
      return contextManager.getNewInstance(handle.getInjectorName());
    }

    @Override
    public Set<Annotation> getQualifiers() {
      return handle.getQualifiers();
    }

    @Override
    public boolean matches(Set<Annotation> annotations) {
      return annotations.containsAll(getQualifiers());
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
      // TODO Auto-generated method stub
      throw new RuntimeException("Not yet implemented.");
    }
  }

  private ContextManager contextManager;
  private final Multimap<String, InjectorHandle> handlesByTypeName = ArrayListMultimap.create();
  private final Map<String, Class<?>> typesByName = new HashMap<String, Class<?>>();

  @Override
  public void destroyBean(Object ref) {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
  }

  @Override
  public boolean isManaged(Object ref) {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
  }

  @Override
  public Object getActualBeanReference(Object ref) {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
  }

  @Override
  public void addProxyReference(Object proxyRef, Object realRef) {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
  }

  @Override
  public boolean isProxyReference(Object ref) {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
  }

  @Override
  public boolean addDestructionCallback(Object beanInstance, DestructionCallback<?> destructionCallback) {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
  }

  @Override
  public void addBeanToContext(Object ref, CreationalContext creationalContext) {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
  }

  @Override
  public void destroyAllBeans() {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
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
    for (final InjectorHandle handle : contextManager.getAllInjectorHandles()) {
      for (final Class<?> assignableType : handle.getAssignableTypes()) {
        handlesByTypeName.put(assignableType.getName(), handle);
        typesByName.put(assignableType.getName(), assignableType);
      }
    }
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  public Collection<IOCBeanDef> lookupBeans(final String name) {
    return (Collection) lookupBeans(typesByName.get(name));
  }

  @Override
  public <T> Collection<IOCBeanDef<T>> lookupBeans(final Class<T> type) {
    final String name = type.getName();
    final Collection<InjectorHandle> handles = handlesByTypeName.get(name);
    final Collection<IOCBeanDef<T>> beanDefs = new ArrayList<IOCBeanDef<T>>(handles.size());

    for (final InjectorHandle handle : handles) {
      beanDefs.add(new IOCBeanDefImplementation<T>(handle, name, type));
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
    final Collection<IOCBeanDef<T>> resolved = lookupBeans(type, qualifiers);
    if (resolved.isEmpty()) {
      throw new RuntimeException("No beans matched " + type.getName() + " with qualifiers " + qualifiers);
    } else if (resolved.size() > 1) {
      final StringBuilder builder = new StringBuilder();
      builder.append("Multiple beans matched " + type.getName() + " with qualifiers " + qualifiers + "\n")
             .append("Found:\n");
      for (final IOCBeanDef<T> beanDef : resolved) {
        builder.append("  ")
               .append(beanDef.toString())
               .append("\n");
      }
      throw new RuntimeException(builder.toString());
    } else {
      return resolved.iterator().next();
    }
  }
}
