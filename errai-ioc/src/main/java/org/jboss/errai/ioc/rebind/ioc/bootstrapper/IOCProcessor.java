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

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.inject.Provider;

import org.jboss.errai.codegen.InnerClass;
import org.jboss.errai.codegen.builder.ClassStructureBuilder;
import org.jboss.errai.codegen.builder.impl.ClassBuilder;
import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaConstructor;
import org.jboss.errai.codegen.meta.MetaField;
import org.jboss.errai.codegen.meta.MetaMethod;
import org.jboss.errai.codegen.meta.MetaParameter;
import org.jboss.errai.codegen.meta.impl.build.BuildMetaClass;
import org.jboss.errai.config.util.ClassScanner;
import org.jboss.errai.ioc.client.api.ContextualTypeProvider;
import org.jboss.errai.ioc.client.api.EnabledByProperty;
import org.jboss.errai.ioc.client.container.RuntimeInjector;
import org.jboss.errai.ioc.rebind.ioc.graph.DefaultQualifierFactory;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraph;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraphBuilder;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraphBuilder.DependencyType;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraphBuilder.InjectorType;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraphBuilderImpl;
import org.jboss.errai.ioc.rebind.ioc.graph.Injector;
import org.jboss.errai.ioc.rebind.ioc.graph.QualifierFactory;
import org.jboss.errai.ioc.rebind.ioc.injector.api.InjectionContext;
import org.jboss.errai.ioc.rebind.ioc.injector.api.WiringElementType;

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

    for (final Injector injector : dependencyGraph) {
      addRuntimeInjectorDeclaration(injector, processingContext);
    }
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

  private void addRuntimeInjectorDeclaration(final Injector injector, final IOCProcessingContext processingContext) {
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
      final Injector typeInjector = builder.addConcreteInjector(type, qualFactory.create(type), InjectorType.Type, WiringElementType.NormalScopedBean);
      processInjectionPoints(typeInjector, builder);
      processProducerMethods(typeInjector, builder);
      processProducerFields(typeInjector, builder);
      maybeProcessAsProvider(typeInjector, builder);
    }
  }

  private void maybeProcessAsProvider(final Injector typeInjector, final DependencyGraphBuilder builder) {
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

  private void addContextualProviderInjector(final Injector typeInjector, final DependencyGraphBuilder builder) {
    final MetaClass type = typeInjector.getInjectedType();
    final MetaMethod providerMethod = type.getMethod("provide", Class[].class, Annotation[].class);
    builder.addConcreteInjector(providerMethod.getReturnType(), qualFactory.universalQualifier(), InjectorType.ContextualProvider, WiringElementType.TopLevelProvider);
    final Injector alias = builder.lookupAlias(providerMethod.getReturnType(), qualFactory.universalQualifier());
    builder.addDependency(typeInjector, alias, DependencyType.ProducerParameter);
  }

  private void addProviderInjector(final Injector typeInjector, final DependencyGraphBuilder builder) {
    final MetaClass type = typeInjector.getInjectedType();
    final MetaMethod providerMethod = type.getMethod("get", new Class[0]);
    builder.addConcreteInjector(providerMethod.getReturnType(), qualFactory.universalQualifier(), InjectorType.Provider, WiringElementType.TopLevelProvider);
    final Injector alias = builder.lookupAlias(providerMethod.getReturnType(), qualFactory.universalQualifier());
    builder.addDependency(typeInjector, alias, DependencyType.ProducerParameter);
  }

  private void processProducerMethods(final Injector typeInjector, final DependencyGraphBuilder builder) {
    final MetaClass type = typeInjector.getInjectedType();
    final Collection<Class<? extends Annotation>> producerAnnos = injectionContext.getAnnotationsForElementType(WiringElementType.ProducerElement);
    for (final Class<? extends Annotation> anno : producerAnnos) {
      final List<MetaMethod> methods = type.getMethodsAnnotatedWith(anno);
      for (final MetaMethod method : methods) {
        final Injector producerInjector = builder.lookupAlias(method.getReturnType(), qualFactory.create(method));
        builder.addDependency(producerInjector, typeInjector, DependencyType.ProducerInstance);
        for (final MetaParameter param : method.getParameters()) {
          final Injector paramAlias = builder.lookupAlias(param.getType(), qualFactory.create(param));
          builder.addDependency(paramAlias, producerInjector, DependencyType.ProducerParameter);
        }
      }
    }
  }

  private void processProducerFields(final Injector typeInjector, final DependencyGraphBuilder builder) {
    final MetaClass type = typeInjector.getInjectedType();
    final Collection<Class<? extends Annotation>> producerAnnos = injectionContext.getAnnotationsForElementType(WiringElementType.ProducerElement);
    for (final Class<? extends Annotation> anno : producerAnnos) {
      final List<MetaField> params = type.getFieldsAnnotatedWith(anno);
      for (final MetaField param : params) {
        final Injector producerInjector = builder.lookupAlias(param.getType(), qualFactory.create(param));
        builder.addDependency(producerInjector, typeInjector, DependencyType.ProducerInstance);
      }
    }
  }

  private void processInjectionPoints(final Injector typeInjector, final DependencyGraphBuilder builder) {
    final MetaClass type = typeInjector.getInjectedType();
    final MetaConstructor injectableConstructor = getInjectableConstructor(type);
    if (injectableConstructor != null) {
      addConstructorInjectionPoints(typeInjector, injectableConstructor, builder);
    }
    addFieldInjectionPoints(typeInjector, builder);
  }

  private void addFieldInjectionPoints(final Injector typeInjector, final DependencyGraphBuilder builder) {
    final MetaClass type = typeInjector.getInjectedType();
    final Collection<Class<? extends Annotation>> injectAnnotations = injectionContext.getAnnotationsForElementType(WiringElementType.InjectionPoint);
    for (final Class<? extends Annotation> inject : injectAnnotations) {
      for (final MetaField field : type.getFieldsAnnotatedWith(inject)) {
        final Injector fieldAlias = builder.lookupAlias(field.getType(), qualFactory.create(field));
        builder.addDependency(typeInjector, fieldAlias, DependencyType.Field);
      }
    }
  }

  private void addConstructorInjectionPoints(final Injector typeInjector, final MetaConstructor injectableConstructor, final DependencyGraphBuilder builder) {
    for (final MetaParameter param : injectableConstructor.getParameters()) {
      final Injector paramAlias = builder.lookupAlias(param.getType(), qualFactory.create(param));
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
