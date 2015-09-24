package org.jboss.errai.ioc.rebind.ioc.graph.impl;

import org.jboss.errai.ioc.rebind.ioc.graph.api.Injectable;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder.Dependency;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder.DependencyType;

/**
 * Base implementation for all {@link Dependency dependencies}.
 *
 * @author Max Barkley <mbarkley@redhat.com>
 */
class BaseDependency implements Dependency {
  AbstractInjectable injectable;
  final DependencyType dependencyType;

  BaseDependency(final AbstractInjectable abstractInjectable, final DependencyType dependencyType) {
    this.injectable = abstractInjectable;
    this.dependencyType = dependencyType;
  }

  @Override
  public String toString() {
    return "[depType=" + dependencyType.toString() + ", abstractInjectable=" + injectable.toString() + "]";
  }

  @Override
  public Injectable getInjectable() {
    return injectable.resolution;
  }

  @Override
  public DependencyType getDependencyType() {
    return dependencyType;
  }
}