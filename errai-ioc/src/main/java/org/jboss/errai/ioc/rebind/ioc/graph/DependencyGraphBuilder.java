package org.jboss.errai.ioc.rebind.ioc.graph;

import java.lang.annotation.Annotation;

import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.ioc.rebind.ioc.injector.api.WiringElementType;

/**
 * @author Max Barkley <mbarkley@redhat.com>
 */
public interface DependencyGraphBuilder {

  Injectable addConcreteInjector(MetaClass injectedType, Qualifier qualifier, Class<? extends Annotation> literalScope,
          InjectorType injectorType, WiringElementType... wiringTypes);

  Injectable lookupAlias(MetaClass type, Qualifier qualifier);

  void addDependency(Injectable from, Injectable to, DependencyType dependencyType);

  DependencyGraph createGraph();

  public static enum InjectorType {
    Type, Producer, Provider, ContextualProvider
  }

  public static enum DependencyType {
    Constructor, Field, ProducerInstance, ProducerParameter
  }
}
