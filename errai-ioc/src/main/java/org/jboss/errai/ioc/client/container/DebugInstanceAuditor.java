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

package org.jboss.errai.ioc.client.container;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.jboss.errai.common.client.api.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Max Barkley <mbarkley@redhat.com>
 */
public class DebugInstanceAuditor implements InstanceAuditor {

  private static final Logger logger = LoggerFactory.getLogger(DebugInstanceAuditor.class);

  private CreationContext active;
  private final List<CreationContext> contexts = new ArrayList<>();
  private final Map<Proxy<?>, CreationContext> contextsByProxy = new IdentityHashMap<>();
  private final Map<String, FactoryHandle> handlesByFactoryName = new HashMap<>();

  @Override
  public <T> void startCreatingActiveInstance(final FactoryHandle handle, final Proxy<T> proxy) {
    if (active == null) {
      newActiveContext(handle);
    }
    handlesByFactoryName.putIfAbsent(handle.getFactoryName(), handle);

    final long time = System.currentTimeMillis();
    if (proxy == null) {
      active.creationEvents.add(new EagerCreationEvent(handle, time));
    }
    else {
      final CreationContext ctx = contextsByProxy.get(proxy);
      ctx.creationEvents.add(new LazyCreationEvent(handle, time, active));
      if (!active.equals(ctx)) {
        active.creationEvents.add(new ForeignLazyCreationEvent(handle, time, ctx));
      }
    }
  }

  private void newActiveContext(final FactoryHandle handle) {
    active = new CreationContext(handle);
    contexts.add(active);
  }

  @Override
  public <T> void loadActiveInstance(final String factoryName, final Proxy<T> proxy, final T activeInstance) {
    final FactoryHandle handle = handlesByFactoryName.get(factoryName);
    if (active != null) {
      active.creationEvents.add(new LookupEvent(handle, System.currentTimeMillis()));
    }
  }

  @Override
  public <T> void finishCreatingActiveInstance(final FactoryHandle handle, final Proxy<T> proxy) {
    if (active.root.equals(handle)) {
      logger.debug("Creation context finished:\n{}", active);
      active = null;
    }
  }

  @Override
  public <T> void createdProxy(final FactoryHandle handle, final Proxy<T> proxy) {
    Assert.notNull(proxy);
    if (active == null) {
      newActiveContext(handle);
    }
    handlesByFactoryName.putIfAbsent(handle.getFactoryName(), handle);
    contextsByProxy.put(proxy, active);
  }

  @Override
  public <T> void loadedProxy(final String factoryName, final Proxy<T> proxy) {
    final FactoryHandle handle = handlesByFactoryName.get(factoryName);
    if (active != null) {
      active.creationEvents.add(new LookupEvent(handle, System.currentTimeMillis()));
    }
  }

  private static class CreationContext {
    private final FactoryHandle root;
    private final List<Event> creationEvents = new ArrayList<>();
    CreationContext(final FactoryHandle root) {
      this.root = root;
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder();
      sb.append("Root (").append(creationEvents.size()).append(" events):").append(root);
      creationEvents.forEach(evt -> sb.append("\n\t").append(evt));

      return sb.toString();
    }
  }

  private static abstract class Event {
    final FactoryHandle created;
    final long time;
    Event(final FactoryHandle created, final long time) {
      this.created = created;
      this.time = time;
    }
    @Override
    public String toString() {
      final Date date = new Date(time);
      return "[" + date + "] " + eventMessage();
    }
    protected abstract String eventMessage();
  }

  private static class LookupEvent extends Event {
    LookupEvent(final FactoryHandle created, final long time) {
      super(created, time);
    }
    @Override
    protected String eventMessage() {
      return "looked up " + created;
    }
  }

  private static class EagerCreationEvent extends Event {
    EagerCreationEvent(final FactoryHandle created, final long time) {
      super(created, time);
    }
    @Override
    protected String eventMessage() {
      return "eagerly created " + created;
    }
  }

  private static class LazyCreationEvent extends Event {
    final CreationContext active;
    LazyCreationEvent(final FactoryHandle created, final long time, final CreationContext active) {
      super(created, time);
      this.active = active;
    }
    @Override
    protected String eventMessage() {
      if (active == null) {
        return "lazily created " + created;
      }
      else {
        return "lazily created " + created + " in context of " + active.root;
      }
    }
  }

  private static class ForeignLazyCreationEvent extends Event {
    final CreationContext owningContext;
    ForeignLazyCreationEvent(final FactoryHandle created, final long time, final CreationContext owningContext) {
      super(created, time);
      this.owningContext = owningContext;
    }
    @Override
    protected String eventMessage() {
      return "forced creation of lazy bean, " + created + ", from context of " + owningContext.root;
    }
  }
}
