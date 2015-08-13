package org.jboss.errai.ioc.rebind.ioc.graph;

import java.lang.annotation.Annotation;

import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaField;
import org.jboss.errai.ioc.rebind.ioc.injector.api.WiringElementType;

/**
 * @author Max Barkley <mbarkley@redhat.com>
 */
public interface DependencyGraphBuilder {

  Injectable addConcreteInjectable(MetaClass injectedType, Qualifier qualifier, Class<? extends Annotation> literalScope,
          InjectorType injectorType, WiringElementType... wiringTypes);

  Injectable lookupAbstractInjectable(MetaClass type, Qualifier qualifier);

  void addDependency(Injectable from, Injectable to, DependencyType dependencyType);

  DependencyGraph createGraph();

  public static enum InjectorType {
    Type, Producer, Provider, ContextualProvider, Abstract
  }

  public static enum DependencyType {
    Constructor, Field, ProducerInstance, ProducerParameter
  }

  public static interface Dependency {

    Injectable getInjectable();

    DependencyType getDependencyType();

  }

  public static interface ParamDependency extends Dependency {

    int getParamIndex();

  }

  public static interface FieldDependency extends Dependency {

    MetaField getField();

  }
}
