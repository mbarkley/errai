package org.jboss.errai.ioc.rebind.ioc.graph.impl;

import org.jboss.errai.codegen.meta.MetaMethod;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder.DependencyType;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder.DisposerMethodDependency;

/**
 * @see DisposerMethodDependency
 * @author Max Barkley <mbarkley@redhat.com>
 */
class DisposerMethodDependencyImpl extends BaseDependency implements DisposerMethodDependency {

  final MetaMethod disposer;

  DisposerMethodDependencyImpl(final AbstractInjectable abstractInjectable, final MetaMethod disposer) {
    super(abstractInjectable, DependencyType.DisposerMethod);
    this.disposer = disposer;
  }

  @Override
  public MetaMethod getDisposerMethod() {
    return disposer;
  }

}