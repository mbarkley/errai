/*
 * Copyright 2013 JBoss, by Red Hat, Inc
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
import java.util.Collection;

/**
 * @author Mike Brock
 */
public interface SyncBeanManager extends ClientBeanManager {

  /**
   * Looks up all beans with the specified bean name as specified by {@link javax.inject.Named}.
   *
   * @param name
   *     the name of bean to lookup
   *
   * @return and unmodifiable list of all beans with the specified name.
   */
  Collection<IOCBeanDef> lookupBeans(String name);

  /**
   * Looks up all beans of the specified type.
   *
   * @param type
   *     The type of the bean
   *
   * @return An unmodifiable list of all the beans that match the specified type. Returns an empty list if there is
   *         no matching type.
   */
  <T> Collection<IOCBeanDef<T>> lookupBeans(Class<T> type);

  /**
   * Looks up a bean reference based on type and qualifiers. Returns <tt>null</tt> if there is no type associated
   * with the specified
   *
   * @param type
   *     The type of the bean
   * @param qualifiers
   *     qualifiers to match
   *
   * @return An unmodifiable list of all beans which match the specified type and qualifiers. Returns an empty list
   *         if no beans match.
   */
  <T> Collection<IOCBeanDef<T>> lookupBeans(Class<T> type, Annotation... qualifiers);

  /**
   * Looks up a bean reference based on type and qualifiers. Returns <tt>null</tt> if there is no type associated
   * with the specified
   *
   * @param type
   *     The type of the bean
   * @param qualifiers
   *     qualifiers to match
   * @param <T>
   *     The type of the bean
   *
   * @return An instance of the {@link IOCSingletonBean} for the matching type and qualifiers.
   *         Throws an {@link IOCResolutionException} if there is a matching type but none of the
   *         qualifiers match or if more than one bean  matches.
   */
  <T> IOCBeanDef<T> lookupBean(Class<T> type, Annotation... qualifiers);
}
