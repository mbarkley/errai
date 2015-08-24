package org.jboss.errai.ioc.rebind.ioc.graph;

public interface DependencyGraph extends Iterable<Injectable> {

  Injectable getConcreteInjectable(String factoryName);

}
