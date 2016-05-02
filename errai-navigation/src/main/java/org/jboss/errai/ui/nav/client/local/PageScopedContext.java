/**
 * Copyright (C) 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.errai.ui.nav.client.local;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import org.jboss.errai.ioc.client.api.ScopeContext;
import org.jboss.errai.ioc.client.container.AbstractContext;
import org.jboss.errai.ioc.client.container.Factory;
import org.jboss.errai.ioc.client.container.Proxy;
import org.jboss.errai.ui.nav.client.local.api.PageScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Max Barkley <mbarkley@redhat.com>
 */
@ScopeContext({Page.class, PageScoped.class})
public class PageScopedContext extends AbstractContext {

  private static class PageContext {
    final String pageClassName;
    final Map<String, Object> instancesByFactoryName = new HashMap<>();

    PageContext(final String pageClassName) {
      this.pageClassName = pageClassName;
    }
  }

  private static final Logger logger = LoggerFactory.getLogger(PageScopedContext.class);

  private static PageScopedContext instance;

  static PageScopedContext getInstance() {
    if (instance == null) {
      throw new RuntimeException("Can't access " + PageScopedContext.class.getSimpleName() + " before it is created by the container.");
    }
    else {
      return instance;
    }
  }

  private PageContext activeContext;

  public PageScopedContext() {
    if (instance != null) {
      logger.warn("Creating a new " + PageScopedContext.class.getSimpleName() + " when one already exists.");
    }
    instance = this;
  }

  @Override
  public Class<? extends Annotation> getScope() {
    return PageScoped.class;
  }

  @Override
  public boolean handlesScope(Class<? extends Annotation> scope) {
    return PageScoped.class.equals(scope) || Page.class.equals(scope);
  }

  @Override
  public boolean isActive() {
    return activeContext != null;
  }

  void setCurrentPage(final String fqcn) {
    if (activeContext != null && !activeContext.pageClassName.equals(fqcn)) {
      for (final Proxy<?> proxy : getExistingProxies()) {
        proxy.clearInstance();
      }
      for (final Object instance : activeContext.instancesByFactoryName.values()) {
        destroyInstance(instance);
      }
    }
    activeContext = new PageContext(fqcn);
  }

  @Override
  protected void registerInstance(final Object unwrappedInstance, final Factory<?> factory) {
    super.registerInstance(unwrappedInstance, factory);
    activeContext.instancesByFactoryName.put(factory.getHandle().getFactoryName(), unwrappedInstance);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected <T> T getActiveInstance(final String factoryName) {
    return (T) activeContext.instancesByFactoryName.get(factoryName);
  }

  @Override
  protected boolean hasActiveInstance(final String factoryName) {
    return isActive() && activeContext.instancesByFactoryName.containsKey(factoryName);
  }

}
