package org.jboss.errai.ioc.rebind.ioc.graph.impl;

import org.jboss.errai.codegen.meta.MetaParameter;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder.DependencyType;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder.ParamDependency;

/**
 * @see ParamDependency
 * @author Max Barkley <mbarkley@redhat.com>
 */
class ParamDependencyImpl extends BaseDependency implements ParamDependency {

  final int paramIndex;
  final MetaParameter parameter;

  ParamDependencyImpl(final AbstractInjectable abstractInjectable, final DependencyType dependencyType, final int paramIndex, final MetaParameter parameter) {
    super(abstractInjectable, dependencyType);
    this.paramIndex = paramIndex;
    this.parameter = parameter;
  }

  @Override
  public int getParamIndex() {
    return paramIndex;
  }

  @Override
  public MetaParameter getParameter() {
    return parameter;
  }

}