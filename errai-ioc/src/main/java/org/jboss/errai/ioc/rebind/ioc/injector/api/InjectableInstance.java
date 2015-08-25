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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.errai.codegen.Statement;
import org.jboss.errai.codegen.builder.ClassStructureBuilder;
import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaClassFactory;
import org.jboss.errai.codegen.meta.MetaField;
import org.jboss.errai.codegen.meta.MetaMethod;
import org.jboss.errai.codegen.util.PrivateAccessType;
import org.jboss.errai.codegen.util.Stmt;
import org.jboss.errai.ioc.client.container.RefHolder;
import org.jboss.errai.ioc.rebind.ioc.bootstrapper.IOCProcessingContext;
import org.jboss.errai.ioc.rebind.ioc.injector.InjectUtil;
import org.jboss.errai.ioc.rebind.ioc.injector.Injector;

public abstract class InjectableInstance<T extends Annotation> extends InjectionPoint<T> {
  private static final String TRANSIENT_DATA_KEY = "InjectableInstance::TransientData";
  private static final TransientDataHolder EMPTY_HOLDER = TransientDataHolder.makeEmpty();

  private static class TransientDataHolder {
    private final Map<String, Map<MetaClass, Statement>> unsatisfiedTransients;
    private final Map<String, Map<MetaClass, Statement>> transientValues;

    private TransientDataHolder(Map<String, Map<MetaClass, Statement>> unsatisfiedTransients, Map<String, Map<MetaClass, Statement>> transientValues) {
      this.unsatisfiedTransients = unsatisfiedTransients;
      this.transientValues = transientValues;
    }

    static TransientDataHolder makeEmpty() {
      return new TransientDataHolder(Collections.<String, Map<MetaClass, Statement>>emptyMap(), Collections.<String, Map<MetaClass, Statement>>emptyMap());
    }

    static TransientDataHolder makeNew() {
      return new TransientDataHolder(new HashMap<String, Map<MetaClass, Statement>>(), new HashMap<String, Map<MetaClass, Statement>>());
    }
  }

  private final Set<MetaMethod> exposedMethods = new HashSet<MetaMethod>();
  private final Map<MetaField, PrivateAccessType> exposedFields = new HashMap<MetaField, PrivateAccessType>();
  protected final ClassStructureBuilder<?> injectorClassBuilder;

  protected InjectableInstance(final T annotation,
                               final TaskType taskType,
                               final Injector injector,
                               final InjectionContext injectionContext,
                               final ClassStructureBuilder<?> injectorClassBuilder) {

    super(annotation, taskType, injector, injectionContext);
    this.injectorClassBuilder = injectorClassBuilder;
  }

  private TransientDataHolder getTransientDataHolder() {
    if (!getTargetInjector().hasAttribute(TRANSIENT_DATA_KEY)) {
      return EMPTY_HOLDER;
    }
    else {
      return (TransientDataHolder) getTargetInjector().getAttribute(TRANSIENT_DATA_KEY);
    }
  }

  private TransientDataHolder getOrCreateWritableDataHolder() {
    if (!getTargetInjector().hasAttribute(TRANSIENT_DATA_KEY)) {
      final TransientDataHolder holder = TransientDataHolder.makeNew();
      getTargetInjector().setAttribute(TRANSIENT_DATA_KEY, holder);
      return holder;
    }
    else {
      return (TransientDataHolder) getTargetInjector().getAttribute(TRANSIENT_DATA_KEY);
    }
  }

  /**
   * Record a transient value -- ie. a value we want the IOC container to track and be referenceable
   * while wiring the code, but not something that is injected.
   */
  public void addTransientValue(final String name, final Class<?> type, final Statement valueRef) {
    addTransientValue(name, MetaClassFactory.get(type), valueRef);
  }

  public void addTransientValue(final String name, final MetaClass type, final Statement valueRef) {
    final TransientDataHolder holder = getOrCreateWritableDataHolder();

    Map<MetaClass, Statement> classStatementMap = holder.transientValues.get(name);
    if (classStatementMap == null) {
      holder.transientValues.put(name, classStatementMap = new HashMap<MetaClass, Statement>());
    }

    if (classStatementMap.containsKey(type)) {
      throw new RuntimeException("transient value already exists: " + name + "::" + type.getFullyQualifiedName());
    }

    final IOCProcessingContext pCtx = getInjectionContext().getProcessingContext();
    if (hasUnsatisfiedTransientValue(name, type)) {
      final Statement unsatisfiedTransientValue = getUnsatisfiedTransientValue(name, type);
      pCtx.append(Stmt.nestedCall(unsatisfiedTransientValue).invoke("set", valueRef));
      classStatementMap.put(type, Stmt.nestedCall(unsatisfiedTransientValue).invoke("get"));
      markSatisfied(name, type);
    }
    else {
      final String varName = InjectUtil.getUniqueVarName();
      pCtx.append(Stmt.declareFinalVariable(varName, type, valueRef));
      classStatementMap.put(type, Stmt.loadVariable(varName));
    }
  }

  public Statement getTransientValue(final String name, final Class<?> type) {
    return getTransientValue(name, MetaClassFactory.get(type));
  }

  public Statement getTransientValue(final String name, final MetaClass type) {
    final TransientDataHolder holder = getTransientDataHolder();
    final Map<MetaClass, Statement> metaClassStatementMap = holder.transientValues.get(name);
    if (metaClassStatementMap != null) {
      final Statement statement = metaClassStatementMap.get(type);
      if (statement != null) {
        return statement;
      }
    }

    if (hasUnsatisfiedTransientValue(name, type)) {
      return Stmt.nestedCall(getUnsatisfiedTransientValue(name, type)).invoke("get");
    }

    final String holderVar = InjectUtil.getUniqueVarName();
    final MetaClass holderType = MetaClassFactory.parameterizedAs(RefHolder.class, MetaClassFactory.typeParametersOf(type));

    final IOCProcessingContext pCtx = getInjectionContext().getProcessingContext();
    pCtx.append(Stmt.declareFinalVariable(holderVar, holderType, Stmt.newObject(holderType)));

    addUnsatisifiedTransientValue(name, type, Stmt.loadVariable(holderVar));

    return Stmt.loadVariable(holderVar).invoke("get");
  }


  private void addUnsatisifiedTransientValue(final String name, final MetaClass type, final Statement holderRef) {
    final TransientDataHolder holder = getOrCreateWritableDataHolder();

    Map<MetaClass, Statement> metaClassStringMap = holder.unsatisfiedTransients.get(name);
    if (metaClassStringMap == null) {
      holder.unsatisfiedTransients.put(name, metaClassStringMap = new HashMap<MetaClass, Statement>());
    }

    metaClassStringMap.put(type, holderRef);
  }


  private Statement getUnsatisfiedTransientValue(final String name, final MetaClass type) {
    final TransientDataHolder holder = getTransientDataHolder();

    if (holder.unsatisfiedTransients.containsKey(name) && holder.unsatisfiedTransients.get(name).containsKey(type)) {
      return holder.unsatisfiedTransients.get(name).get(type);
    }
    return null;
  }

  private void markSatisfied(final String name, final MetaClass type) {
    final TransientDataHolder holder = getTransientDataHolder();

    if (holder.unsatisfiedTransients.containsKey(name) && holder.unsatisfiedTransients.get(name).containsKey(type)) {
      final Map<MetaClass, Statement> metaClassStatementMap = holder.unsatisfiedTransients.get(name);
      metaClassStatementMap.remove(type);
      if (metaClassStatementMap.isEmpty()) {
        holder.unsatisfiedTransients.remove(name);
      }
    }
  }

  public void addExposedField(final MetaField field, final PrivateAccessType accessType) {
    if (exposedFields.containsKey(field)) {
      if (PrivateAccessType.Both.equals(accessType)) {
        exposedFields.put(field, accessType);
      } else if (!exposedFields.get(field).equals(accessType)) {
        exposedFields.put(field, PrivateAccessType.Both);
      }
    } else {
      exposedFields.put(field, accessType);
    }
  }

  public void addExposedMethod(final MetaMethod method) {
    exposedMethods.add(method);
  }

  public boolean hasUnsatisfiedTransientValue(final String name, final MetaClass type) {
    return getUnsatisfiedTransientValue(name, type) != null;
  }

  /**
   * Returns an instance of a {@link Statement} which represents the value associated for injection at this
   * InjectionPoint. This statement may represent a raw field access, a method call to a getter method, or an
   * internalized variable in the bootstrapper which is holding the value.
   *
   * @return a statement representing the value of the injection point.
   */
  public abstract Statement getValueStatement();

  public abstract Injector getTargetInjector();

  public abstract Statement callOrBind(final Statement... values);

}
