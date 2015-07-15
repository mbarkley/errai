package org.jboss.errai.ioc.rebind.ioc.injector.api;

import java.lang.annotation.Annotation;

import org.jboss.errai.codegen.Statement;
import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaConstructor;
import org.jboss.errai.codegen.meta.MetaField;
import org.jboss.errai.codegen.meta.MetaMethod;
import org.jboss.errai.codegen.meta.MetaParameter;
import org.jboss.errai.codegen.util.PrivateAccessType;
import org.jboss.errai.ioc.rebind.ioc.injector.Injector;

public interface InjectableInstance<T extends Annotation> {

  /**
   * Record a transient value -- ie. a value we want the IOC container to track and be referenceable
   * while wiring the code, but not something that is injected.
   */
  void addTransientValue(String name, Class type, Statement valueRef);

  void addTransientValue(String name, MetaClass type, Statement valueRef);

  Statement getTransientValue(String name, Class type);

  Statement getTransientValue(String name, MetaClass type);

  boolean hasAnyUnsatified();

  boolean hasUnsatisfiedTransientValue(String name, MetaClass type);

  /**
   * Returns an instance of a {@link Statement} which represents the value associated for injection at this
   * InjectionPoint. This statement may represent a raw field access, a method call to a getter method, or an
   * internalized variable in the bootstrapper which is holding the value.
   *
   * @return a statement representing the value of the injection point.
   */
  Statement getValueStatement();

  Injector getTargetInjector();

  Statement callOrBind(Statement... values);

  InjectionContext getInjectionContext();

  Injector getInjector();

  MetaClass getEnclosingType();

  void ensureMemberExposed();

  MetaClass getElementTypeOrMethodReturnType();

  TaskType getTaskType();

  MetaMethod getMethod();

  MetaField getField();

  T getAnnotation();

  boolean isAnnotationPresent(Class<? extends Annotation> class1);

  MetaParameter getParm();

  <A extends Annotation> A getAnnotation(Class<A> class1);

  MetaConstructor getConstructor();

  String getMemberName();

  Annotation[] getQualifiers();

  Annotation getRawAnnotation();

  void ensureMemberExposed(PrivateAccessType read);

  MetaClass getElementType();

}