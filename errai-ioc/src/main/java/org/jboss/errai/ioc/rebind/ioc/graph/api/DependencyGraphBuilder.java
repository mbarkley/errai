package org.jboss.errai.ioc.rebind.ioc.graph.api;

import java.lang.annotation.Annotation;

import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaClassMember;
import org.jboss.errai.codegen.meta.MetaField;
import org.jboss.errai.codegen.meta.MetaMethod;
import org.jboss.errai.codegen.meta.MetaParameter;
import org.jboss.errai.ioc.rebind.ioc.injector.api.WiringElementType;

/**
 * @author Max Barkley <mbarkley@redhat.com>
 */
public interface DependencyGraphBuilder {

  public String SHORT_NAMES_PROP = "errai.graph_builder.short_factory_names";
  public boolean SHORT_NAMES = Boolean.getBoolean(SHORT_NAMES_PROP);

  Injectable addConcreteInjectable(MetaClass injectedType, Qualifier qualifier, Class<? extends Annotation> literalScope,
          InjectableType factoryType, WiringElementType... wiringTypes);

  Injectable addTransientInjectable(MetaClass injectedType, Qualifier qualifier, Class<? extends Annotation> literalScope, WiringElementType... wiringTypes);

  void addFieldDependency(Injectable concreteInjectable, MetaClass type, Qualifier qualifier, MetaField dependentField);

  void addConstructorDependency(Injectable concreteInjectable, MetaClass type, Qualifier qualifier, int paramIndex, MetaParameter param);

  void addProducerParamDependency(Injectable concreteInjectable, MetaClass type, Qualifier qualifier, int paramIndex, MetaParameter param);

  void addProducerMemberDependency(Injectable concreteInjectable, MetaClass type, Qualifier qualifier, MetaClassMember producingMember);

  void addSetterMethodDependency(Injectable concreteInjectable, MetaClass type, Qualifier qualifier, MetaMethod setter);

  void addDisposesMethodDependency(Injectable concreteInjectable, MetaClass type, Qualifier qualifier, MetaMethod disposer);

  void addDisposesParamDependency(Injectable concreteInjectable, MetaClass type, Qualifier qualifier, Integer index, MetaParameter param);

  DependencyGraph createGraph(boolean removeUnreachable);

  public static enum InjectableType {
    Type, JsType, Producer, Provider, ContextualProvider, Abstract, Transient, Extension
  }

  public static enum DependencyType {
    Constructor, Field, ProducerMember, ProducerParameter, SetterParameter, DisposerMethod, DisposerParameter
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

  public static interface ProducerInstanceDependency extends Dependency {

    MetaClassMember getProducingMember();

  }

  public static interface DisposerMethodDependency extends Dependency {

    MetaMethod getDisposerMethod();

  }

}
