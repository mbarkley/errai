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
import org.jboss.errai.codegen.meta.MetaClassMember;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder.DependencyType;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder.ProducerMemberDependency;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder.ResolutionCardinality;

/**
 * @see ProducerMemberDependency
 * @author Max Barkley <mbarkley@redhat.com>
 */
class ProducerMemberDependencyImpl extends BaseDependency implements ProducerMemberDependency {

  MetaClassMember producingMember;

  ProducerMemberDependencyImpl(final InjectableHandle injectable, final DependencyType dependencyType, final MetaClassMember producingMember) {
    super(injectable, dependencyType);
    this.producingMember = producingMember;
  }

  @Override
  public ResolutionCardinality getCardinality() {
    return (producingMember.isStatic()) ? ResolutionCardinality.EMPTY : ResolutionCardinality.SINGLE;
  }

  @Override
  public MetaClassMember getProducingMember() {
    return producingMember;
  }

  @Override
  protected HasAnnotations getAnnotated() {
    return producingMember;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((producingMember == null) ? 0 : producingMember.hashCode());
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
    final ProducerMemberDependencyImpl other = (ProducerMemberDependencyImpl) obj;
    if (producingMember == null) {
      if (other.producingMember != null)
        return false;
    }
    else if (!producingMember.equals(other.producingMember))
      return false;
    return true;
  }

}
