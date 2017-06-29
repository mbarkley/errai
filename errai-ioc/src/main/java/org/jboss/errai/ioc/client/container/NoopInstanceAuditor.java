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

/**
 *
 * @author Max Barkley <mbarkley@redhat.com>
 */
public class NoopInstanceAuditor implements InstanceAuditor {

  @Override
  public <T> void loadActiveInstance(final String factoryName, final Proxy<T> proxy, final T activeInstance) {
  }

  @Override
  public <T> void startCreatingActiveInstance(final FactoryHandle handle, final Proxy<T> proxy) {
  }

  @Override
  public <T> void finishCreatingActiveInstance(final FactoryHandle handle, final Proxy<T> proxy) {
  }

  @Override
  public <T> void createdProxy(final FactoryHandle handle, final Proxy<T> proxy) {
  }

  @Override
  public <T> void loadedProxy(final String factoryName, final Proxy<T> proxy) {
  }

}
