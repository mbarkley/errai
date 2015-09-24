package org.jboss.errai.ioc.rebind.ioc.graph.impl;

import org.jboss.errai.codegen.meta.MetaMethod;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder.DependencyType;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder.SetterParameterDependency;

/**
 * @see SetterParameterDependency
 * @author Max Barkley <mbarkley@redhat.com>
 */
class SetterParameterDependencyImpl extends BaseDependency implements SetterParameterDependency {

  final MetaMethod method;

  SetterParameterDependencyImpl(final AbstractInjectable abstractInjectable, final MetaMethod method) {
    super(abstractInjectable, DependencyType.SetterParameter);
    this.method = method;
  }

  @Override
  public MetaMethod getMethod() {
    return method;
  }

}