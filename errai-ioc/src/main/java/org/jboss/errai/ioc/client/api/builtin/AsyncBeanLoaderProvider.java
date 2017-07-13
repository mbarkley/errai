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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jboss.errai.ioc.client.api.AsyncBeanLoader;
import org.jboss.errai.ioc.client.api.ContextualTypeProvider;
import org.jboss.errai.ioc.client.api.IOCProvider;
import org.jboss.errai.ioc.client.container.IOC;
import org.jboss.errai.ioc.client.container.IOCResolutionException;
import org.jboss.errai.ioc.client.container.async.AsyncBeanDef;
import org.jboss.errai.ioc.client.container.async.AsyncBeanManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Max Barkley <mbarkley@redhat.com>
 */
@IOCProvider
public class AsyncBeanLoaderProvider implements ContextualTypeProvider<AsyncBeanLoader<?>> {

  private static final Logger logger = LoggerFactory.getLogger(AsyncBeanLoader.class);

  @Override
  public AsyncBeanLoader<?> provide(final Class<?>[] typeargs, final Annotation[] qualifiers) {
    final Class<?> type = typeargs[0];
    return new AsyncBeanLoaderImpl<>(cb -> {
      final AsyncBeanManager bm = IOC.getAsyncBeanManager();
      try {
        final AsyncBeanDef<?> beanDef = bm.lookupBean(type, qualifiers);
        beanDef.getInstance(instance -> cb.accept(instance));
      } catch (final IOCResolutionException ex) {
        logger.error("Unable to lookup bean.", ex);
      }
    }, error -> {
      logger.error("Error invoking method on async bean, " + type.getName(), error);
    });
  }

  public static class AsyncBeanLoaderImpl<T> implements AsyncBeanLoader<T> {

    private Consumer<Consumer<T>> loader;
    private Queue<Consumer<T>> queued = new LinkedList<>();
    private T instance;
    private final Consumer<Throwable> asyncErrorHandler;

    public AsyncBeanLoaderImpl(final Consumer<Consumer<T>> loader, final Consumer<Throwable> asyncErrorHandler) {
      this.loader = loader;
      this.asyncErrorHandler = asyncErrorHandler;
    }

    @Override
    public void call(final Consumer<T> method) {
      schedule(method, true);
    }

    @Override
    public void enqueue(final Consumer<T> method) {
      schedule(method, false);
    }

    @Override
    public <R> R syncCall(final Function<T, R> method) throws IllegalStateException {
      if (instance == null) {
        throw new IllegalStateException("Cannot invoke method on bean that is not yet loaded.");
      }
      else {
        return method.apply(instance);
      }
    }

    private void schedule(final Consumer<T> method, final boolean load) {
      if (instance == null) {
        final RuntimeException ctx = new RuntimeException("This is the initial call that triggered the async method invocation.");
        queued.add(inst -> {
          try {
            method.accept(inst);
          } catch (final Throwable t) {
            Throwable cur;
            for (cur = t; cur.getCause() != null; cur = cur.getCause());
            cur.initCause(ctx);
            throw t;
          }
        });
        if (load) {
          maybeLoadBean();
        }
      }
      else {
        method.accept(instance);
      }
    }

    private void maybeLoadBean() {
      if (loader != null) {
        loader.accept(t -> {
          instance = t;
          processQueue(t);
        });
        loader = null;
      }
    }

    private void processQueue(final T instance) {
      final List<Throwable> thrown = new ArrayList<>(queued.size());
      queued.forEach(action -> {
        try {
          action.accept(instance);
        } catch (final Throwable t) {
          thrown.add(t);
        }
      });
      queued = null;
      thrown.forEach(asyncErrorHandler);
    }
  }

}
