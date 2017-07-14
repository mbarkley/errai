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

package org.jboss.errai.ioc.rebind.ioc.graph.api;

import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.ioc.rebind.ioc.graph.impl.InjectableHandle;

/**
 *
 * @author Max Barkley <mbarkley@redhat.com>
 */
public interface HasInjectableHandle {

  /**
   * @return A handle that can be used for looking up this injectable.
   */
  InjectableHandle getHandle();

  /**
   * @return The class of the injectable.
   */
  default MetaClass getInjectedType() {
    return getHandle().getType();
  }

  /**
   * @return The qualifier of this injectable.
   */
  default Qualifier getQualifier() {
    return getHandle().getQualifier();
  }

}
