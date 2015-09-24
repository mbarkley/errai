package org.jboss.errai.ioc.rebind.ioc.graph.api;

import org.jboss.errai.ioc.client.container.FactoryHandle;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder.Dependency;

/**
 * A collection of {@link Injectable} whose {@link Dependency dependencies} have
 * all been fully satisfied.
 *
 * @see DependencyGraphBuilder
 * @author Max Barkley <mbarkley@redhat.com>
 */
public interface DependencyGraph extends Iterable<Injectable> {

  /**
   * @param factoryName The unique {@link FactoryHandle#getFactoryName() name} of the desired {@link Injectable}.
   * @return The unique {@link Injectable} with the given {@link FactoryHandle#getFactoryName() name}.
   */
  Injectable getConcreteInjectable(String factoryName);

}
