package org.jboss.errai.ioc.rebind.ioc.graph;

import java.lang.annotation.Annotation;

import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaField;
import org.jboss.errai.codegen.meta.MetaMethod;
import org.jboss.errai.codegen.meta.MetaParameter;
import org.jboss.errai.ioc.rebind.ioc.injector.api.WiringElementType;

/**
 * @author Max Barkley <mbarkley@redhat.com>
 */
public interface DependencyGraphBuilder {

  Injectable addConcreteInjectable(MetaClass injectedType, Qualifier qualifier, Class<? extends Annotation> literalScope,
          FactoryType factoryType, WiringElementType... wiringTypes);

  Injectable lookupAbstractInjectable(MetaClass type, Qualifier qualifier);

  void addDependency(Injectable concreteInjectable, Dependency dependency);

  FieldDependency createFieldDependency(Injectable abstractInjectable, MetaField dependentField);

  ParamDependency createConstructorDependency(Injectable abstractInjectable, int paramIndex, MetaParameter param);

  ParamDependency createProducerParamDependency(Injectable abstractInjectable, int paramIndex, MetaParameter param);

  Dependency createProducerInstanceDependency(Injectable abstractInjectble);

  SetterParameterDependency createSetterMethodDependency(Injectable abstractInjectable, MetaMethod setter);

  DependencyGraph createGraph(boolean removeUnreachable);

  public static enum FactoryType {
    Type, JsType, Producer, Provider, ContextualProvider, Abstract, CustomProvided, CustomProvider
  }

  public static enum DependencyType {
    Constructor, Field, ProducerInstance, ProducerParameter, SetterParameter
  }

  public static interface Dependency {

    Injectable getInjectable();

    DependencyType getDependencyType();

  }

  public static interface ParamDependency extends Dependency {

    int getParamIndex();

    MetaParameter getParameter();

  }

  public static interface FieldDependency extends Dependency {

    MetaField getField();

  }

  public static interface SetterParameterDependency extends Dependency {

    MetaMethod getMethod();

  }
}
