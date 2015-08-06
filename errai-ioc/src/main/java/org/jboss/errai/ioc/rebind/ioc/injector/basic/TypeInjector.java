/*
 * Copyright 2013 JBoss, by Red Hat, Inc
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

package org.jboss.errai.ioc.rebind.ioc.injector.basic;

import static org.jboss.errai.codegen.meta.MetaClassFactory.parameterizedAs;
import static org.jboss.errai.codegen.meta.MetaClassFactory.typeParametersOf;
import static org.jboss.errai.codegen.util.Stmt.invokeStatic;
import static org.jboss.errai.codegen.util.Stmt.loadVariable;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.inject.Specializes;
import javax.inject.Named;

import org.jboss.errai.codegen.InnerClass;
import org.jboss.errai.codegen.Statement;
import org.jboss.errai.codegen.builder.impl.ClassBuilder;
import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.impl.build.BuildMetaClass;
import org.jboss.errai.codegen.util.Refs;
import org.jboss.errai.ioc.client.api.qualifiers.BuiltInQualifiers;
import org.jboss.errai.ioc.client.container.BeanProvider;
import org.jboss.errai.ioc.rebind.ioc.bootstrapper.IOCProcessingContext;
import org.jboss.errai.ioc.rebind.ioc.exception.InjectionFailure;
import org.jboss.errai.ioc.rebind.ioc.injector.AbstractInjector;
import org.jboss.errai.ioc.rebind.ioc.injector.InjectUtil;
import org.jboss.errai.ioc.rebind.ioc.injector.Injector;
import org.jboss.errai.ioc.rebind.ioc.injector.api.InjectableInstance;
import org.jboss.errai.ioc.rebind.ioc.injector.api.InjectionContext;
import org.jboss.errai.ioc.rebind.ioc.injector.api.WiringElementType;
import org.jboss.errai.ioc.rebind.ioc.metadata.JSR330QualifyingMetadata;

import com.google.gwt.core.client.GWT;

/**
 * This injector implementation is responsible for the lion's share of the container's workload. It is responsible
 * for generating the <tt>SimpleCreationalContext</tt>'s which produce instances of beans. It is also responsible for
 * handling the differences in semantics between singleton and dependent-scoped beans.
 *
 * @author Mike Brock
 */
public class TypeInjector extends AbstractInjector {
  protected final MetaClass type;
  protected final String innerProviderClassName;
  protected String instanceVarName;

  public TypeInjector(final MetaClass type, final InjectionContext context) {
    this.type = type;

    if (!context.isReachable(type)) {
      disableSoftly();
    }

    // check to see if this is a singleton and/or alternative bean
    this.testMock = context.isElementType(WiringElementType.TestMockBean, type);
    this.singleton = context.isElementType(WiringElementType.SingletonBean, type);
    this.alternative = context.isElementType(WiringElementType.AlternativeBean, type);

    this.instanceVarName = InjectUtil.getNewInjectorName().concat("_").concat(type.getName());

    final Set<Annotation> qualifiers = JSR330QualifyingMetadata.createSetFromAnnotations(type.getAnnotations());

    qualifiers.add(BuiltInQualifiers.ANY_INSTANCE);

    if (type.isAnnotationPresent(Specializes.class)) {
      qualifiers.addAll(makeSpecialized(context));
    }

    if (type.isAnnotationPresent(Named.class)) {
      final Named namedAnnotation = type.getAnnotation(Named.class);

      this.beanName = namedAnnotation.value().equals("")
          ? type.getBeanDescriptor().getBeanName() : namedAnnotation.value();
    }

    if (!qualifiers.isEmpty()) {
      qualifyingMetadata = context.getProcessingContext().getQualifyingMetadataFactory()
          .createFrom(qualifiers.toArray(new Annotation[qualifiers.size()]));
    }
    else {
      qualifyingMetadata = context.getProcessingContext().getQualifyingMetadataFactory().createDefaultMetadata();
    }

    innerProviderClassName = "org.jboss.errai.ioc.client." + type.getFullyQualifiedName().replace('.', '_') + "_provider";
  }

  @Override
  public void renderProvider(final InjectableInstance injectableInstance) {
    if ((isRendered() && isEnabled()) ||
        !injectableInstance.getInjectionContext().isIncluded(type)) {
      return;
    }

    final InjectionContext injectContext = injectableInstance.getInjectionContext();
    final IOCProcessingContext procContext = injectContext.getProcessingContext();

    final BuildMetaClass runtimeProviderClass = addRuntimeProviderAbstractInnerClass(procContext);
    addCreationalCallbackField(procContext, runtimeProviderClass);
  }

  private void addCreationalCallbackField(final IOCProcessingContext procContext, final BuildMetaClass runtimeProviderClass) {
    final MetaClass providerType = parameterizedAs(BeanProvider.class, typeParametersOf(type));

    creationalCallbackVarName = InjectUtil.getNewInjectorName().concat("_").concat(type.getName()).concat("_creational");

    procContext.getBootstrapBuilder()
               .privateField(creationalCallbackVarName, providerType)
               .initializesWith(invokeStatic(GWT.class, "create", runtimeProviderClass))
               .finish();
  }

  private BuildMetaClass addRuntimeProviderAbstractInnerClass(final IOCProcessingContext procContext) {
    final BuildMetaClass classDef = ClassBuilder.define(innerProviderClassName)
                                                .publicScope()
                                                .abstractClass()
                                                .implementsInterface(parameterizedAs(BeanProvider.class, typeParametersOf(type)))
                                                .body()
                                                .getClassDefinition();
    procContext.getBootstrapClass().addInnerClass(new InnerClass(classDef));

    return classDef;
  }

  @Override
  public Statement getBeanInstance(final InjectableInstance injectableInstance) {
    renderProvider(injectableInstance);

    if (isSingleton() && !hasNewQualifier(injectableInstance)) {

      /**
       * if this bean is a singleton bean and there is no @New qualifier on the site we're injecting
       * into, we merely return a reference to the singleton instance variable from the bootstrapper.
       */
      return Refs.get(instanceVarName);
    }
    else {

      /**
       * if the bean is not singleton, or it's scope is overridden to be effectively dependent,
       * we return a call CreationContext.getInstance() on the SimpleCreationalContext for this injector.
       */
      return loadVariable(creationalCallbackVarName).invoke("getInstance", Refs.get("context"));
    }
  }

  private Set<Annotation> makeSpecialized(final InjectionContext context) {
    final MetaClass type = getInjectedType();

    if (type.getSuperClass().getFullyQualifiedName().equals(Object.class.getName())) {
      throw new InjectionFailure("the specialized bean " + type.getFullyQualifiedName() + " must directly inherit "
          + "from another bean");
    }

    final Set<Annotation> qualifiers = new HashSet<Annotation>();

    MetaClass cls = type;
    while ((cls = cls.getSuperClass()) != null && !cls.getFullyQualifiedName().equals(Object.class.getName())) {
      if (!context.hasInjectorForType(cls)) {
        context.addType(cls);
      }

      context.declareOverridden(cls);

      final List<Injector> injectors = context.getInjectors(cls);

      for (final Injector inj : injectors) {
        if (this.beanName == null) {
          this.beanName = inj.getBeanName();
        }

        inj.setEnabled(false);
        qualifiers.addAll(Arrays.asList(inj.getQualifyingMetadata().getQualifiers()));
      }
    }

    return qualifiers;
  }

  @Override
  public boolean isPseudo() {
    return replaceable;
  }

  @Override
  public String getInstanceVarName() {
    return instanceVarName;
  }

  @Override
  public MetaClass getInjectedType() {
    return type;
  }

  @Override
  public String getCreationalCallbackVarName() {
    return creationalCallbackVarName;
  }

  @Override
  public boolean isRegularTypeInjector() {
    return true;
  }
}
