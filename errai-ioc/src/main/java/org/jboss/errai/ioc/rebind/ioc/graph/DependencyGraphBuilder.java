package org.jboss.errai.ioc.rebind.ioc.graph;

import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.ioc.rebind.ioc.injector.api.WiringElementType;

public interface DependencyGraphBuilder {

  Injector addConcreteInjector(MetaClass injectedType, Qualifier qualifier, InjectorType injectorType, WiringElementType... wiringTypes);

  Injector lookupAlias(MetaClass type, Qualifier qualifier, WiringElementType... wiringTypes);

  void addDependency(Injector from, Injector to, DependencyType dependencyType);

  DependencyGraph resolveDependencies();

  public static enum InjectorType {
    Type, Producer, Provider, ContextualProvider
  }

  public static enum DependencyType {
    Constructor, Field, ProducerInstance, ProducerParameter
  }
}
