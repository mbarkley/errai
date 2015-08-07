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

/**
 * Normal scoped beans or dependent scoped beans decorated with AOP features
 * will be wrapped in proxies. All proxies produced by a {@link Factory} must
 * implement this interface.
 *
 * @param <T> The type of the instance being proxied.
 *
 * @author Max Barkley <mbarkley@redhat.com>
 */
public interface Proxy<T> {

  /**
   * @return Returns this proxy as the type of the instance it is proxying.
   */
  T asBeanType();

  /**
   * Set the instance that this proxy dispatches to (i.e. set the proxied instance).
   *
   * @param instance An instance created by the same factory as this proxy.
   */
  void setInstance(T instance);

  /**
   * Removes the reference to any instance that was previously proxied (via {@link #setInstance(Object)}).
   */
  void clearInstance();

  /**
   * This is called after a proxy is created so that the proxy can request a proxied instance on demand.
   *
   * @param context The context associated with the {@link Factory} that created this proxy.
   */
  void setContext(Context context);

  /**
   * If no proxied instance has yet been set, this method will request and instance from the {@link Context} and {@link #setInstance(Object) set} it.
   *
   * @return The instance wrapped by this {@link Proxy}.
   */
  T unwrappedInstance();

  /**
   * Called once after {@link #setInstance(Object)} is called.
   *
   * @param instance The instance wrapped by this proxy.
   */
  void initProxyProperties(final T instance);

}
