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

package org.jboss.errai.ioc.rebind.ioc.bootstrapper;

import static org.jboss.errai.codegen.meta.MetaClassFactory.parameterizedAs;
import static org.jboss.errai.codegen.meta.MetaClassFactory.typeParametersOf;
import static org.jboss.errai.codegen.util.Stmt.invokeStatic;
import static org.jboss.errai.codegen.util.Stmt.loadVariable;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.NormalScope;
import javax.inject.Provider;
import javax.inject.Scope;

import org.jboss.errai.codegen.BlockStatement;
import org.jboss.errai.codegen.InnerClass;
import org.jboss.errai.codegen.builder.ClassStructureBuilder;
import org.jboss.errai.codegen.builder.impl.ClassBuilder;
import org.jboss.errai.codegen.builder.impl.ObjectBuilder;
import org.jboss.errai.codegen.meta.HasAnnotations;
import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaConstructor;
import org.jboss.errai.codegen.meta.MetaField;
import org.jboss.errai.codegen.meta.MetaMethod;
import org.jboss.errai.codegen.meta.MetaParameter;
import org.jboss.errai.codegen.meta.impl.build.BuildMetaClass;
import org.jboss.errai.codegen.util.Stmt;
import org.jboss.errai.config.util.ClassScanner;
import org.jboss.errai.ioc.client.api.ContextualTypeProvider;
import org.jboss.errai.ioc.client.api.EnabledByProperty;
import org.jboss.errai.ioc.client.api.ScopeContext;
import org.jboss.errai.ioc.client.container.Context;
import org.jboss.errai.ioc.client.container.RuntimeInjector;
import org.jboss.errai.ioc.rebind.ioc.graph.DefaultQualifierFactory;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraph;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraphBuilder;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraphBuilder.DependencyType;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraphBuilder.InjectorType;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraphBuilderImpl;
import org.jboss.errai.ioc.rebind.ioc.graph.Injectable;
import org.jboss.errai.ioc.rebind.ioc.graph.QualifierFactory;
import org.jboss.errai.ioc.rebind.ioc.injector.api.InjectionContext;
import org.jboss.errai.ioc.rebind.ioc.injector.api.WiringElementType;

import com.google.gwt.core.client.GWT;

public class IOCProcessor {

  private final InjectionContext injectionContext;
  private final QualifierFactory qualFactory;

  public IOCProcessor(final InjectionContext injectionContext) {
    this.injectionContext = injectionContext;
    this.qualFactory = new DefaultQualifierFactory();
  }

  public void process(final IOCProcessingContext processingContext) {
    final Collection<MetaClass> allMetaClasses = findRelevantClasses(processingContext);

    final DependencyGraphBuilder graphBuilder = new DependencyGraphBuilderImpl();
    final DependencyGraph dependencyGraph = processDependencies(allMetaClasses, graphBuilder);
    InjectorGenerator.setDependencyGraph(dependencyGraph);

    final Map<Class<? extends Annotation>, MetaClass> scopeContexts = findScopeContexts(processingContext);
    final BlockStatement initializer = processingContext.getBootstrapClass().getInstanceInitializer();
    for (final Class<? extends Annotation> scope : injectionContext.getAnnotationsForElementType(WiringElementType.NormalScopedBean)) {
      if (scopeContexts.containsKey(scope)) {
        final MetaClass scopeContextImpl = scopeContexts.get(scope);
        if (!scopeContextImpl.isDefaultInstantiable()) {
          throw new RuntimeException("The @ScopeContext " + scopeContextImpl.getName() + " must have a public, no-args constructor.");
        }

        initializer.addStatement(Stmt.declareFinalVariable(getContextVarName(scopeContextImpl), Context.class, ObjectBuilder.newInstanceOf(scopeContextImpl)));
      }
    }
    final MetaClass dependentContext = scopeContexts.get(Dependent.class);
    initializer.addStatement(Stmt.declareFinalVariable(getContextVarName(dependentContext), Context.class, ObjectBuilder.newInstanceOf(dependentContext)));

    for (final Injectable injectable : dependencyGraph) {
      addRuntimeInjectorDeclaration(injectable, processingContext);
      registerInjectorWithContext(injectable, scopeContexts, initializer);
    }
  }

  private void registerInjectorWithContext(final Injectable injectable,
          final Map<Class<? extends Annotation>, MetaClass> scopeContexts, final BlockStatement initializer) {
    final String contextVarName = getContextVarName(scopeContexts.get(injectable.getScope()));
    initializer.addStatement(
            loadVariable(contextVarName).invoke("registerInjector", invokeStatic(GWT.class, "create", injectable.getInjectedType().asClass())));
  }

  private String getContextVarName(final MetaClass scopeContextImpl) {
    // TODO cache in map?
    return scopeContextImpl.getFullyQualifiedName().replace('.', '_') + "_context";
  }

  private Map<Class<? extends Annotation>, MetaClass> findScopeContexts(final IOCProcessingContext processingContext) {
    final Collection<MetaClass> scopeContexts = ClassScanner.getTypesAnnotatedWith(ScopeContext.class);
    final Map<Class<? extends Annotation>, MetaClass> annoToContextImpl = new HashMap<Class<? extends Annotation>, MetaClass>();
    for (final MetaClass scopeContext : scopeContexts) {
      if (!scopeContext.isAssignableTo(Context.class)) {
        throw new RuntimeException("They type " + scopeContext.getFullyQualifiedName()
                + " was annotated with @ScopeContext but does not implement " + Context.class.getName());
      }
      final ScopeContext anno = scopeContext.getAnnotation(ScopeContext.class);
      for (final Class<? extends Annotation> scope : anno.value()) {
        annoToContextImpl.put(scope, scopeContext);
      }
    }

    return annoToContextImpl;
  }

  private Collection<MetaClass> findRelevantClasses(final IOCProcessingContext processingContext) {
    final Collection<MetaClass> allMetaClasses = new HashSet<MetaClass>();

    final WiringElementType[] typeLevelWiringTypes = new WiringElementType[] {
        WiringElementType.DependentBean,
        WiringElementType.NormalScopedBean,
        WiringElementType.TopLevelProvider,
        WiringElementType.ContextualTopLevelProvider
    };

    final WiringElementType[] methodLevelWiringTypes = new WiringElementType[] {
        WiringElementType.ProducerElement
    };

    final WiringElementType[] fieldLevelWiringTypes = new WiringElementType[] {
        WiringElementType.ProducerElement,
        WiringElementType.InjectionPoint
    };

    for (final WiringElementType wiringType : typeLevelWiringTypes) {
      for (final Class<? extends Annotation> anno : injectionContext.getAnnotationsForElementType(wiringType)) {
        allMetaClasses.addAll(ClassScanner.getTypesAnnotatedWith(anno));
      }
    }

    for (final WiringElementType wiringType : methodLevelWiringTypes) {
      for (final Class<? extends Annotation> anno : injectionContext.getAnnotationsForElementType(wiringType)) {
        for (final MetaMethod method : ClassScanner.getMethodsAnnotatedWith(anno, processingContext.getPackages(), processingContext.getGeneratorContext())) {
          allMetaClasses.add(method.getDeclaringClass());
        }
      }
    }

    for (final WiringElementType wiringType : fieldLevelWiringTypes) {
      for (final Class<? extends Annotation> anno : injectionContext.getAnnotationsForElementType(wiringType)) {
        for (final MetaField field : ClassScanner.getFieldsAnnotatedWith(anno, processingContext.getPackages(), processingContext.getGeneratorContext())) {
          allMetaClasses.add(field.getDeclaringClass());
        }
      }
    }
    return allMetaClasses;
  }

  private void addRuntimeInjectorDeclaration(final Injectable injector, final IOCProcessingContext processingContext) {
    final ClassStructureBuilder<?> builder = processingContext.getBootstrapBuilder();
    final BuildMetaClass runtimeInjector = ClassBuilder.define(InjectorGenerator.getInjectorClassName(injector.getInjectedType()))
                                                       .publicScope()
                                                       .abstractClass()
                                                       .implementsInterface(parameterizedAs(RuntimeInjector.class, typeParametersOf(injector.getInjectedType())))
                                                       .body()
                                                       .getClassDefinition();
    builder.declaresInnerClass(new InnerClass(runtimeInjector));
  }

  private DependencyGraph processDependencies(final Collection<MetaClass> types, final DependencyGraphBuilder builder) {
    for (final MetaClass type : types) {
      processType(type, builder);
    }

    return builder.createGraph();
  }

  private void processType(final MetaClass type, final DependencyGraphBuilder builder) {
    if (isTypeInjectable(type)) {
      final Injectable typeInjector = builder.addConcreteInjector(type, qualFactory.create(type), getDirectScope(type), InjectorType.Type, WiringElementType.NormalScopedBean);
      processInjectionPoints(typeInjector, builder);
      processProducerMethods(typeInjector, builder);
      processProducerFields(typeInjector, builder);
      maybeProcessAsProvider(typeInjector, builder);
    }
  }

  private void maybeProcessAsProvider(final Injectable typeInjector, final DependencyGraphBuilder builder) {
    final MetaClass type = typeInjector.getInjectedType();
    final Collection<Class<? extends Annotation>> providerAnnotations = injectionContext.getAnnotationsForElementType(WiringElementType.TopLevelProvider);
    for (final Class<? extends Annotation> anno : providerAnnotations) {
      if (type.isAnnotationPresent(anno)) {
        if (type.isAssignableTo(Provider.class)) {
          addProviderInjector(typeInjector, builder);
        }
        else if (type.isAssignableTo(ContextualTypeProvider.class)) {
          addContextualProviderInjector(typeInjector, builder);
        }

        break;
      }
    }
  }

  private void addContextualProviderInjector(final Injectable providerInjectable, final DependencyGraphBuilder builder) {
    final MetaClass providerImpl = providerInjectable.getInjectedType();
    final MetaMethod providerMethod = providerImpl.getMethod("provide", Class[].class, Annotation[].class);
    final MetaClass providedType = providerMethod.getReturnType();
    final Injectable providedInjectable = builder.addConcreteInjector(providedType, qualFactory.universalQualifier(),
            Dependent.class, InjectorType.ContextualProvider, WiringElementType.TopLevelProvider);
    final Injectable alias = builder.lookupAlias(providerImpl, providerInjectable.getQualifier());
    builder.addDependency(providedInjectable, alias, DependencyType.ProducerParameter);
  }

  private Class<? extends Annotation> getDirectScope(final HasAnnotations annotated) {
    // TODO validate that there's only one scope?
    for (final Annotation anno : annotated.getAnnotations()) {
      final Class<? extends Annotation> annoType = anno.annotationType();
      if (annoType.isAnnotationPresent(Scope.class) || annoType.isAnnotationPresent(NormalScope.class)) {
        return annoType;
      }
    }

    return Dependent.class;
  }

  private void addProviderInjector(final Injectable providerImplInjectable, final DependencyGraphBuilder builder) {
    final MetaClass providerImpl = providerImplInjectable.getInjectedType();
    final MetaMethod providerMethod = providerImpl.getMethod("get", new Class[0]);
    final MetaClass providedType = providerMethod.getReturnType();
    final Injectable providedInjectable = builder.addConcreteInjector(providedType, qualFactory.universalQualifier(),
            Dependent.class, InjectorType.Provider, WiringElementType.TopLevelProvider);
    builder.addDependency(providedInjectable, builder.lookupAlias(providerImpl, providerImplInjectable.getQualifier()), DependencyType.ProducerParameter);
  }

  private void processProducerMethods(final Injectable typeInjectable, final DependencyGraphBuilder builder) {
    final MetaClass type = typeInjectable.getInjectedType();
    final Collection<Class<? extends Annotation>> producerAnnos = injectionContext.getAnnotationsForElementType(WiringElementType.ProducerElement);
    for (final Class<? extends Annotation> anno : producerAnnos) {
      final List<MetaMethod> methods = type.getMethodsAnnotatedWith(anno);
      for (final MetaMethod method : methods) {
        final Class<? extends Annotation> directScope = getDirectScope(method);
        final WiringElementType wiringTypes = getWiringTypesForScopeAnnotation(directScope);
        final Injectable producerInjectable = builder.addConcreteInjector(method.getReturnType(), qualFactory.create(method), directScope, InjectorType.Producer, wiringTypes);
        builder.addDependency(producerInjectable, typeInjectable, DependencyType.ProducerInstance);
        for (final MetaParameter param : method.getParameters()) {
          final Injectable paramAlias = builder.lookupAlias(param.getType(), qualFactory.create(param));
          builder.addDependency(paramAlias, producerInjectable, DependencyType.ProducerParameter);
        }
      }
    }
  }

  private WiringElementType getWiringTypesForScopeAnnotation(Class<? extends Annotation> directScope) {
    if (directScope.equals(Dependent.class)) {
      return WiringElementType.DependentBean;
    } else {
      return WiringElementType.NormalScopedBean;
    }
  }

  private void processProducerFields(final Injectable typeInjectable, final DependencyGraphBuilder builder) {
    final MetaClass type = typeInjectable.getInjectedType();
    final Collection<Class<? extends Annotation>> producerAnnos = injectionContext.getAnnotationsForElementType(WiringElementType.ProducerElement);
    for (final Class<? extends Annotation> anno : producerAnnos) {
      final List<MetaField> params = type.getFieldsAnnotatedWith(anno);
      for (final MetaField param : params) {
        final Injectable producerInjectable = builder.lookupAlias(param.getType(), qualFactory.create(param));
        builder.addDependency(producerInjectable, typeInjectable, DependencyType.ProducerInstance);
      }
    }
  }

  private void processInjectionPoints(final Injectable typeInjector, final DependencyGraphBuilder builder) {
    final MetaClass type = typeInjector.getInjectedType();
    final MetaConstructor injectableConstructor = getInjectableConstructor(type);
    if (injectableConstructor != null) {
      addConstructorInjectionPoints(typeInjector, injectableConstructor, builder);
    }
    addFieldInjectionPoints(typeInjector, builder);
  }

  private void addFieldInjectionPoints(final Injectable typeInjector, final DependencyGraphBuilder builder) {
    final MetaClass type = typeInjector.getInjectedType();
    final Collection<Class<? extends Annotation>> injectAnnotations = injectionContext.getAnnotationsForElementType(WiringElementType.InjectionPoint);
    for (final Class<? extends Annotation> inject : injectAnnotations) {
      for (final MetaField field : type.getFieldsAnnotatedWith(inject)) {
        final Injectable fieldAlias = builder.lookupAlias(field.getType(), qualFactory.create(field));
        builder.addDependency(typeInjector, fieldAlias, DependencyType.Field);
      }
    }
  }

  private void addConstructorInjectionPoints(final Injectable typeInjector, final MetaConstructor injectableConstructor, final DependencyGraphBuilder builder) {
    for (final MetaParameter param : injectableConstructor.getParameters()) {
      final Injectable paramAlias = builder.lookupAlias(param.getType(), qualFactory.create(param));
      builder.addDependency(typeInjector, paramAlias, DependencyType.Constructor);
    }
  }

  private MetaConstructor getInjectableConstructor(final MetaClass type) {
    final Collection<Class<? extends Annotation>> injectAnnotations = injectionContext.getAnnotationsForElementType(WiringElementType.InjectionPoint);
    for (final MetaConstructor con : type.getConstructors()) {
      for (final Class<? extends Annotation> anno : injectAnnotations) {
        if (con.isAnnotationPresent(anno)) {
          return con;
        }
      }
    }

    return null;
  }

  private boolean isTypeInjectable(final MetaClass type) {
    if (hasEnablingProperty(type)) {
      return isEnabledByProperty(type);
    } else {
      if (isNormalScoped(type)) {
        if (isProxyable(type)) {
          return hasAtMostOneInjectableConstructor(type);
        } else {
          // TODO improve message
          throw new RuntimeException("The type " + type.getName() + " must be proxyable.");
        }
      } else {
        return true;
      }
    }
  }

  private boolean isEnabledByProperty(final MetaClass type) {
    final EnabledByProperty anno = type.getAnnotation(EnabledByProperty.class);
    final boolean propValue = Boolean.getBoolean(anno.value());
    final boolean negated = anno.negated();

    return propValue ^ negated;
  }

  private boolean hasEnablingProperty(final MetaClass type) {
    return type.isAnnotationPresent(EnabledByProperty.class);
  }

  private boolean hasAtMostOneInjectableConstructor(final MetaClass type) {
    boolean hasInjectableConstructor = false;
    final Collection<Class<? extends Annotation>> injectAnnotations = injectionContext.getAnnotationsForElementType(WiringElementType.InjectionPoint);
    for (final MetaConstructor constructor : type.getConstructors()) {
      for (final Class<? extends Annotation> injectAnnotation : injectAnnotations) {
        if (constructor.isAnnotationPresent(injectAnnotation)) {
          if (hasInjectableConstructor) {
            return false;
          } else {
            hasInjectableConstructor = true;
            break;
          }
        }
      }
    }

    return true;
  }

  private boolean isProxyable(final MetaClass type) {
    return type.isDefaultInstantiable();
  }

  private boolean isNormalScoped(final MetaClass type) {
    final Collection<Class<? extends Annotation>> normalScopes = injectionContext.getAnnotationsForElementType(WiringElementType.NormalScopedBean);
    for (final Class<? extends Annotation> scope : normalScopes) {
      if (type.isAnnotationPresent(scope)) {
        return true;
      }
    }

    return false;
  }

}
