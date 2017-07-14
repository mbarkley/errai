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
import org.jboss.errai.codegen.meta.MetaField;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder.DependencyType;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder.FieldDependency;

/**
 * @see FieldDependency
 * @author Max Barkley <mbarkley@redhat.com>
 */
class FieldDependencyImpl extends BaseDependency implements FieldDependency {

  final MetaField field;

  FieldDependencyImpl(final InjectableHandle injectable, final MetaField field) {
    super(injectable, DependencyType.Field);
    this.field = field;
  }

  @Override
  public MetaField getField() {
    return field;
  }

  @Override
  protected HasAnnotations getAnnotated() {
    return field;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((field == null) ? 0 : field.hashCode());
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
    final FieldDependencyImpl other = (FieldDependencyImpl) obj;
    if (field == null) {
      if (other.field != null)
        return false;
    }
    else if (!field.equals(other.field))
      return false;
    return true;
  }

}
