/*
 * Copyright 2011 JBoss, by Red Hat, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.errai.ioc.rebind.ioc.injector.api;

import java.lang.annotation.Annotation;
import java.util.Arrays;

import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaConstructor;
import org.jboss.errai.codegen.meta.MetaField;
import org.jboss.errai.codegen.meta.MetaMethod;
import org.jboss.errai.codegen.meta.MetaParameter;
import org.jboss.errai.codegen.util.PrivateAccessType;
import org.jboss.errai.ioc.rebind.ioc.injector.Injector;
import org.jboss.errai.ioc.rebind.ioc.metadata.QualifyingMetadata;

/**
 * @author Mike Brock
 */
public abstract class InjectionPoint<T> {
  protected final T annotation;
  protected final TaskType taskType;

  protected final Injector injector;
  protected final InjectionContext injectionContext;

  protected InjectionPoint(final T annotation,
                        final TaskType taskType,
                        final Injector injector,
                        final InjectionContext injectionContext) {

    this.annotation = annotation;
    this.taskType = taskType;
    this.injector = injector;
    this.injectionContext = injectionContext;
  }

  public T getAnnotation() {
    return annotation;
  }

  public Annotation getRawAnnotation() {
    return (Annotation) annotation;
  }

  public MetaConstructor getConstructor() {
    throw new RuntimeException("Cannot invoke on a non-constructor injection point.");
  }

  public TaskType getTaskType() {
    return taskType;
  }

  public MetaMethod getMethod() {
    throw new RuntimeException("Cannot invoke on a non-method injection point.");
  }

  public MetaField getField() {
    throw new RuntimeException("Cannot invoke on a non-field injection point.");
  }

  /**
   * Returns the element type or a method return type, based on what the injection point is.
   * <p/>
   * <strong>Parameters</strong>:
   * <pre><code>
   *  public class MyClass {
   *   public void MyClass(@A Set set) {
   *   }
   * <p/>
   *   public void setMethod(Foo foo, @B Bar t) {
   *   }
   *  }
   * </code></pre>
   * If the element being decorated is the parameter where <tt>@A</tt> represents the injection/decorator point,
   * then the type returned by this method will be <tt>Set</tt>. If the element being decorated is the parameter
   * where <tt>@B</tt> represents the injection point, then the type returned by this method will be <tt>Bar</tt>.
   * <p/>
   * <strong>Fields</strong>:
   * <pre><code>
   *  public class MyClass {
   *    {@literal @}A private Map myField;
   *  }
   * </code></pre>
   * If the element being decorated is the field where <tt>@A</tt> represents the injection/decorator point,
   * then the type returned by this method wil be <tt>Map</tt>.
   * <p/>
   * <strong>Methods</strong>:
   * <pre><code>
   *  public class MyClass {
   *    {@literal @}A private List getList() {
   *    }
   * <p/>
   *    {@literal @}B private void doSomething() {
   *    }
   *  }
   * </code></pre>
   * If the element being decorated is the method where <tt>@A</tt> represents the injection/decorator point,
   * then the type returned by this method will be <tt>List</tt>. If the element being decorated is the method
   * where <tt>@B</tt> represents the injection/decorator point, then the type returned by this method will be
   * <tt>void</tt>.
   * <p/>
   * <strong>Constructor and Types</strong>:
   * <pre><code>
   *  {@literal @}A
   *  public class MyClass {
   *    {@literal @}B
   *    public MyClass() {
   *    }
   *  }
   * </code></pre>
   * If the class element being decorated is the method where <tt>@A</tt> represents the injection/decorator point,
   * then the type returned by this method will be <tt>MyClass</tt>. Also, if the constructor element being
   * decorated is the constructor where <tt>@B</tt> represents the injection/decorator point, then the type
   * returned by this method will be <tt>MyClass</tt>.
   *
   * @return The underlying type of the element or return type for a method.
   */
  public abstract MetaClass getElementTypeOrMethodReturnType();

  public abstract MetaClass getElementType();

  /**
   * Returns the parameter reference if the injection point is a parameter, otherwise returns <tt>null</tt>.
   *
   * @return the {@link MetaParameter} reference if the injection point is a parameter, otherwise <tt>null</tt>.
   */
  public MetaParameter getParm() {
    throw new RuntimeException("Cannot invoke on a non-parameter injection point.");
  }

  /**
   * Returns the {@link Injector} reference for the the bean
   *
   * @return
   */
  public Injector getInjector() {
    return injector;
  }

  public InjectionContext getInjectionContext() {
    return injectionContext;
  }

  public void ensureMemberExposed() {
    ensureMemberExposed(PrivateAccessType.Both);
  }

  public abstract void ensureMemberExposed(final PrivateAccessType accessType);

  public abstract String getMemberName();

  public abstract MetaClass getEnclosingType();

  public abstract Annotation[] getQualifiers();

  public QualifyingMetadata getQualifyingMetadata() {
    return injectionContext.getProcessingContext().getQualifyingMetadataFactory().createFrom(getQualifiers());
  }

  public boolean isAnnotationPresent(Class<? extends Annotation> annotation) {
    return getAnnotation(annotation) != null;
  }

  @SuppressWarnings("unchecked")
  public <A extends Annotation> A getAnnotation(Class<A> annotation) {
    for (Annotation a : Arrays.asList(getAnnotations())) {
      if (annotation != null && annotation.isAssignableFrom(a.annotationType())) {
        return (A) a;
      }
    }
    return null;
  }

  public abstract Annotation[] getAnnotations();

}
