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

package org.jboss.errai.ioc.rebind.ioc.graph.impl;

import java.util.Objects;

import org.jboss.errai.codegen.meta.HasAnnotations;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder.DependencyType;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder.ImplicitDependency;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder.ResolutionCardinality;

/**
 *
 * @author Max Barkley <mbarkley@redhat.com>
 */
public class ImplicitDependencyImpl extends BaseDependency implements ImplicitDependency {

  ImplicitDependencyImpl(final InjectableHandle injectable) {
    super(injectable, DependencyType.Implicit);
  }

  @Override
  protected HasAnnotations getAnnotated() {
    return getHandle().getQualifier();
  }

  @Override
  public int hashCode() {
    return Objects.hash(getHandle(), getDependencyType());
  }

  @Override
  public ResolutionCardinality getCardinality() {
    return ResolutionCardinality.ANY;
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof ImplicitDependency) {
      final ImplicitDependency other = (ImplicitDependency) obj;
      return getHandle().equals(other.getHandle());
    }

    return false;
  }

}
