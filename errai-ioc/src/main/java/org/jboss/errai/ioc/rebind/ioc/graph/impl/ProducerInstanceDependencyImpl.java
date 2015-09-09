package org.jboss.errai.ioc.rebind.ioc.graph.impl;

import org.jboss.errai.codegen.meta.MetaClassMember;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder.DependencyType;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder.ProducerInstanceDependency;

class ProducerInstanceDependencyImpl extends BaseDependency implements ProducerInstanceDependency {

  MetaClassMember producingMember;

  ProducerInstanceDependencyImpl(final AbstractInjectable abstractInjectable, final DependencyType dependencyType, final MetaClassMember producingMember) {
    super(abstractInjectable, dependencyType);
    this.producingMember = producingMember;
  }

  @Override
  public MetaClassMember getProducingMember() {
    return producingMember;
  }

}