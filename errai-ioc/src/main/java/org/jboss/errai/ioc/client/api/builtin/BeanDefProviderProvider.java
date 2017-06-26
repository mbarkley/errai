/*
 * Copyright (C) 2017 Red Hat, Inc. and/or its affiliates.
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

package org.jboss.errai.ioc.client.api.builtin;

import static org.jboss.errai.ioc.client.IOCUtil.getSyncBean;
import static org.jboss.errai.ioc.client.IOCUtil.joinQualifiers;
import static org.jboss.errai.ioc.client.container.IOC.getBeanManager;

import java.lang.annotation.Annotation;
import java.util.Iterator;
import java.util.stream.Stream;

import org.jboss.errai.ioc.client.IOCUtil;
import org.jboss.errai.ioc.client.QualifierUtil;
import org.jboss.errai.ioc.client.api.BeanDefProvider;
import org.jboss.errai.ioc.client.api.ContextualTypeProvider;
import org.jboss.errai.ioc.client.api.IOCProvider;
import org.jboss.errai.ioc.client.container.SyncBeanDef;

/**
 *
 * @author Max Barkley <mbarkley@redhat.com>
 */
@IOCProvider
public class BeanDefProviderProvider implements ContextualTypeProvider<BeanDefProvider<?>> {

  private static final Annotation[] DEFAULT_QUALIFIERS = new Annotation[] { QualifierUtil.DEFAULT_ANNOTATION };

  @Override
  public BeanDefProvider<?> provide(final Class<?>[] typeargs, final Annotation[] qualifiers) {
    return new BeanDefProviderImpl<>(typeargs[0], qualifiers.length == 0 ? DEFAULT_QUALIFIERS : qualifiers);
  }

  private static class BeanDefProviderImpl<T> implements BeanDefProvider<T> {

    private final Class<T> type;
    private final Annotation[] qualifiers;

    public BeanDefProviderImpl(final Class<T> type, final Annotation... qualifiers) {
      this.type = type;
      this.qualifiers = qualifiers;
    }

    @Override
    public SyncBeanDef<T> get() {
      return getSyncBean(type, qualifiers);
    }

    @Override
    public Iterator<SyncBeanDef<T>> iterator() {
      return getBeanManager().lookupBeans(type, qualifiers).iterator();
    }

    @Override
    public BeanDefProvider<T> select(final Annotation... qualifiers) {
      return new BeanDefProviderImpl<>(type, joinQualifiers(this.qualifiers, qualifiers));
    }

    @Override
    public <U extends T> BeanDefProvider<U> select(final Class<U> subType, final Annotation... qualifiers) {
      return new BeanDefProviderImpl<>(subType, joinQualifiers(this.qualifiers, qualifiers));
    }

    @Override
    public Stream<SyncBeanDef<T>> stream() {
      return getBeanManager().lookupBeans(type, qualifiers).stream();
    }

    @Override
    public boolean isUnsatisfied() {
      return IOCUtil.isUnsatisfied(type, qualifiers);
    }

    @Override
    public boolean isAmbiguous() {
      return IOCUtil.isAmbiguous(type, qualifiers);
    }

  }

}
