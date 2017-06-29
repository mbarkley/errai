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
public interface InstanceAuditor {

  static InstanceAuditor noopAuditor() {
    return new NoopInstanceAuditor();
  }

  static InstanceAuditor debugAuditor() {
    return new DebugInstanceAuditor();
  }

  <T> void loadActiveInstance(String factoryName, Proxy<T> proxy, T activeInstance);
  <T> void startCreatingActiveInstance(FactoryHandle handle, Proxy<T> proxy);
  <T> void finishCreatingActiveInstance(FactoryHandle handle, Proxy<T> proxy);
  <T> void createdProxy(FactoryHandle handle, Proxy<T> proxy);
  <T> void loadedProxy(String factoryName, Proxy<T> proxy);

}
