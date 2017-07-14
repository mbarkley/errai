/*
 * Copyright (C) 2015 Red Hat, Inc. and/or its affiliates.
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

package org.jboss.errai.ioc.rebind.ioc.graph.impl;

import org.jboss.errai.codegen.meta.HasAnnotations;
import org.jboss.errai.codegen.meta.MetaMethod;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder.DependencyType;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder.SetterParameterDependency;

/**
 * @see SetterParameterDependency
 * @author Max Barkley <mbarkley@redhat.com>
 */
class SetterParameterDependencyImpl extends BaseDependency implements SetterParameterDependency {

  final MetaMethod method;

  SetterParameterDependencyImpl(final InjectableHandle injectable, final MetaMethod method) {
    super(injectable, DependencyType.SetterParameter);
    this.method = method;
  }

  @Override
  public MetaMethod getMethod() {
    return method;
  }

  @Override
  protected HasAnnotations getAnnotated() {
    return method;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((method == null) ? 0 : method.hashCode());
    return result;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    final SetterParameterDependencyImpl other = (SetterParameterDependencyImpl) obj;
    if (method == null) {
      if (other.method != null)
        return false;
    }
    else if (!method.equals(other.method))
      return false;
    return true;
  }

}
