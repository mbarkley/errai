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

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;

import javax.inject.Provider;

import org.jboss.errai.codegen.meta.HasAnnotations;
import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaConstructor;
import org.jboss.errai.codegen.meta.MetaField;
import org.jboss.errai.codegen.meta.MetaMethod;
import org.jboss.errai.codegen.meta.MetaParameter;
import org.jboss.errai.ioc.client.api.ContextualTypeProvider;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraph;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraphBuilder;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraphBuilder.DependencyType;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraphBuilder.InjectorType;
import org.jboss.errai.ioc.rebind.ioc.graph.Injector;
import org.jboss.errai.ioc.rebind.ioc.graph.QualifierFactory;
import org.jboss.errai.ioc.rebind.ioc.injector.api.InjectionContext;
import org.jboss.errai.ioc.rebind.ioc.injector.api.WiringElementType;

/**
 * The main IOC configuration processor. This class is responsible for scanning the classpath, finding beans,
 * reading configuration, and then configuring the IOC code generator to emit the bootstrapper code.
 */
public class IOCConfigProcessor {

  private final InjectionContext injectionContext;
  private final QualifierFactory qualFactory;

  public IOCConfigProcessor(final InjectionContext injectionContext) {
    this.injectionContext = injectionContext;
    this.qualFactory = null;
  }

  public void process(IOCProcessingContext processingContext) {
  }

  private DependencyGraph processDependencies(final Collection<MetaClass> types, final DependencyGraphBuilder builder) {
    for (final MetaClass type : types) {
      processType(type, builder);
    }

    return builder.resolveDependencies();
  }

  private void processType(final MetaClass type, final DependencyGraphBuilder builder) {
    if (isInjectable(type)) {
      final Injector typeInjector = builder.addConcreteInjector(type, qualFactory.create(type), InjectorType.Type, WiringElementType.NormalScopedBean);
      processInjectionPoints(typeInjector, builder);
      processProducerMethods(typeInjector, builder);
      processProducerFields(typeInjector, builder);
      addInjectorIfProvider(typeInjector, builder);
    }
  }

  private void addInjectorIfProvider(final Injector typeInjector, final DependencyGraphBuilder builder) {
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
    final MetaMethod providerMethod = type.getMethod("provider", Class[].class, Annotation[].class);
    builder.addConcreteInjector(providerMethod.getReturnType(), qualFactory.unqualified(), InjectorType.ContextualProvider, WiringElementType.TopLevelProvider);
    final Injector alias = builder.lookupAlias(providerMethod.getReturnType(), qualFactory.unqualified(), WiringElementType.TopLevelProvider);
    builder.addDependency(typeInjector, alias, DependencyType.ProducerParameter);
  }

  private void addProviderInjector(final Injector typeInjector, final DependencyGraphBuilder builder) {
    final MetaClass type = typeInjector.getInjectedType();
    final MetaMethod providerMethod = type.getMethod("get", new Class[0]);
    builder.addConcreteInjector(providerMethod.getReturnType(), qualFactory.unqualified(), InjectorType.Provider, WiringElementType.TopLevelProvider);
    final Injector alias = builder.lookupAlias(providerMethod.getReturnType(), qualFactory.unqualified(), WiringElementType.TopLevelProvider);
    builder.addDependency(typeInjector, alias, DependencyType.ProducerParameter);
  }

  private void processProducerMethods(final Injector typeInjector, final DependencyGraphBuilder builder) {
    final MetaClass type = typeInjector.getInjectedType();
    final Collection<Class<? extends Annotation>> producerAnnos = injectionContext.getAnnotationsForElementType(WiringElementType.ProducerElement);
    for (final Class<? extends Annotation> anno : producerAnnos) {
      final List<MetaMethod> methods = type.getMethodsAnnotatedWith(anno);
      for (final MetaMethod method : methods) {
        final Injector producerInjector = builder.lookupAlias(method.getReturnType(), qualFactory.create(method), getScope(method));
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
        final Injector producerInjector = builder.lookupAlias(param.getType(), qualFactory.create(param), getScope(param));
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
        final Injector fieldAlias = builder.lookupAlias(field.getType(), qualFactory.create(field), getScope(field));
        builder.addDependency(typeInjector, fieldAlias, DependencyType.Field);
      }
    }
  }

  private void addConstructorInjectionPoints(final Injector typeInjector, final MetaConstructor injectableConstructor, final DependencyGraphBuilder builder) {
    for (final MetaParameter param : injectableConstructor.getParameters()) {
      final WiringElementType scope = getScope(param);
      final Injector paramAlias = builder.lookupAlias(param.getType(), qualFactory.create(param), scope);
      builder.addDependency(typeInjector, paramAlias, DependencyType.Constructor);
    }
  }

  private WiringElementType getScope(final HasAnnotations annotated) {
    if (isNormalScoped(annotated)) {
      return WiringElementType.NormalScopedBean;
    } else {
      return WiringElementType.DependentBean;
    }
  }

  private boolean isNormalScoped(HasAnnotations annotated) {
    final Collection<Class<? extends Annotation>> annotations = injectionContext.getAnnotationsForElementType(WiringElementType.NormalScopedBean);
    for (final Class<? extends Annotation> anno : annotations) {
      if (annotated.isAnnotationPresent(anno)) {
        return true;
      }
    }

    return false;
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

  private boolean isInjectable(final MetaClass type) {
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
    return (type.getConstructor(new MetaClass[0]) != null);
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
