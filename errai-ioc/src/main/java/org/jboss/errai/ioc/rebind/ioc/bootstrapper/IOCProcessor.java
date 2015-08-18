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

import static org.jboss.errai.codegen.builder.impl.ObjectBuilder.newInstanceOf;
import static org.jboss.errai.codegen.meta.MetaClassFactory.parameterizedAs;
import static org.jboss.errai.codegen.meta.MetaClassFactory.typeParametersOf;
import static org.jboss.errai.codegen.util.Stmt.invokeStatic;
import static org.jboss.errai.codegen.util.Stmt.loadVariable;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.NormalScope;
import javax.enterprise.inject.Alternative;
import javax.inject.Provider;
import javax.inject.Scope;

import org.jboss.errai.codegen.InnerClass;
import org.jboss.errai.codegen.builder.BlockBuilder;
import org.jboss.errai.codegen.builder.ClassStructureBuilder;
import org.jboss.errai.codegen.builder.impl.ClassBuilder;
import org.jboss.errai.codegen.builder.impl.ObjectBuilder;
import org.jboss.errai.codegen.meta.HasAnnotations;
import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaClassFactory;
import org.jboss.errai.codegen.meta.MetaConstructor;
import org.jboss.errai.codegen.meta.MetaField;
import org.jboss.errai.codegen.meta.MetaMethod;
import org.jboss.errai.codegen.meta.MetaParameter;
import org.jboss.errai.codegen.meta.impl.build.BuildMetaClass;
import org.jboss.errai.codegen.util.Stmt;
import org.jboss.errai.config.rebind.EnvUtil;
import org.jboss.errai.config.util.ClassScanner;
import org.jboss.errai.ioc.client.api.ContextualTypeProvider;
import org.jboss.errai.ioc.client.api.EnabledByProperty;
import org.jboss.errai.ioc.client.api.ScopeContext;
import org.jboss.errai.ioc.client.container.Context;
import org.jboss.errai.ioc.client.container.ContextManager;
import org.jboss.errai.ioc.client.container.ContextManagerImpl;
import org.jboss.errai.ioc.client.container.Injector;
import org.jboss.errai.ioc.rebind.ioc.graph.DefaultQualifierFactory;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraph;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraphBuilder;
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
  private Collection<String> alternatives;

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
    @SuppressWarnings("rawtypes")
    final BlockBuilder registerInjectorsBody = addScopeContextsToRegisterInjectorsMethod(processingContext, scopeContexts);

    declareAndRegisterInjectors(processingContext, dependencyGraph, scopeContexts, registerInjectorsBody);
    final String contextManagerFieldName = declareContextManagerField(processingContext);
    addContextsToContextManager(scopeContexts.values(), contextManagerFieldName, registerInjectorsBody);

    registerInjectorsBody.finish();

    processingContext.getBlockBuilder()._(loadVariable("this").invoke("registerInjectors"));
  }

  private void addContextsToContextManager(final Collection<MetaClass> scopeContextImpls,
          final String contextManagerFieldName, @SuppressWarnings("rawtypes") final BlockBuilder registerInjectorsBody) {
    for (final MetaClass scopeContextImpl : scopeContextImpls) {
      registerInjectorsBody._(loadVariable(contextManagerFieldName).invoke("addContext", loadVariable(getContextVarName(scopeContextImpl))));
    }
  }

  @SuppressWarnings("unchecked")
  private String declareContextManagerField(final IOCProcessingContext processingContext) {
    final String contextManagerFieldName = "contextManager";
    processingContext.getBootstrapBuilder()
      .privateField(contextManagerFieldName, ContextManager.class)
      .initializesWith(ObjectBuilder.newInstanceOf(ContextManagerImpl.class))
      .finish();

    return contextManagerFieldName;
  }

  private void declareAndRegisterInjectors(final IOCProcessingContext processingContext,
          final DependencyGraph dependencyGraph, final Map<Class<? extends Annotation>, MetaClass> scopeContexts,
          @SuppressWarnings("rawtypes") final BlockBuilder registerInjectorsBody) {
    for (final Injectable injectable : dependencyGraph) {
      final MetaClass injectorClass = addRuntimeInjectorDeclaration(injectable, processingContext);
      registerInjectorWithContext(injectable, injectorClass, scopeContexts, registerInjectorsBody);
    }
  }

  @SuppressWarnings("rawtypes")
  private BlockBuilder addScopeContextsToRegisterInjectorsMethod(final IOCProcessingContext processingContext,
          final Map<Class<? extends Annotation>, MetaClass> scopeContexts) {
    final Set<String> namesAlreadyAdded = new HashSet<String>();
    @SuppressWarnings({ "unchecked" })
    final BlockBuilder methodBody = processingContext.getBootstrapBuilder().privateMethod(void.class, "registerInjectors").body();
    for (final Class<? extends Annotation> scope : injectionContext.getAnnotationsForElementType(WiringElementType.NormalScopedBean)) {
      if (scopeContexts.containsKey(scope)) {
        final MetaClass scopeContextImpl = scopeContexts.get(scope);
        if (!namesAlreadyAdded.contains(scopeContextImpl.getName())) {
          if (!scopeContextImpl.isDefaultInstantiable()) {
            throw new RuntimeException("The @ScopeContext " + scopeContextImpl.getName() + " must have a public, no-args constructor.");
          }

          methodBody._(Stmt.declareFinalVariable(getContextVarName(scopeContextImpl), Context.class, newInstanceOf(scopeContextImpl)));
          namesAlreadyAdded.add(scopeContextImpl.getName());
        }
      }
    }
    final MetaClass dependentContext = scopeContexts.get(Dependent.class);
    methodBody._(Stmt.declareFinalVariable(getContextVarName(dependentContext), Context.class, newInstanceOf(dependentContext)));

    return methodBody;
  }

  private void registerInjectorWithContext(final Injectable injectable, MetaClass injectorClass,
          final Map<Class<? extends Annotation>, MetaClass> scopeContexts,
          @SuppressWarnings("rawtypes") final BlockBuilder registerInjectorsBody) {
    final String contextVarName = getContextVarName(scopeContexts.get(injectable.getScope()));
    registerInjectorsBody._(loadVariable(contextVarName).invoke("registerInjector",
            Stmt.castTo(parameterizedAs(Injector.class, typeParametersOf(injectable.getInjectedType())),
                    invokeStatic(GWT.class, "create", injectorClass))));
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
    allMetaClasses.addAll(MetaClassFactory.getAllCachedClasses());
    allMetaClasses.remove(MetaClassFactory.get(Object.class));

    return allMetaClasses;
  }

  private MetaClass addRuntimeInjectorDeclaration(final Injectable injector, final IOCProcessingContext processingContext) {
    final ClassStructureBuilder<?> builder = processingContext.getBootstrapBuilder();
    final BuildMetaClass runtimeInjector = ClassBuilder.define(injector.getInjectorClassSimpleName())
                                                       .publicScope()
                                                       .abstractClass()
                                                       .implementsInterface(parameterizedAs(Injector.class, typeParametersOf(injector.getInjectedType())))
                                                       .body()
                                                       .getClassDefinition();
    builder.declaresInnerClass(new InnerClass(runtimeInjector));

    return runtimeInjector;
  }

  private DependencyGraph processDependencies(final Collection<MetaClass> types, final DependencyGraphBuilder builder) {
    for (final MetaClass type : types) {
      processType(type, builder);
    }

    return builder.createGraph();
  }

  private void processType(final MetaClass type, final DependencyGraphBuilder builder) {
    if (isTypeInjectableCandidate(type)) {
      if (isSimpleton(type)) {
        builder.addConcreteInjectable(type, qualFactory.forConcreteInjectable(type), Dependent.class, InjectorType.Type,
                WiringElementType.DependentBean, WiringElementType.Simpleton);
      } else if (isTypeInjectable(type)) {
        final Class<? extends Annotation> directScope = getDirectScope(type);
        final Injectable typeInjector = builder.addConcreteInjectable(type, qualFactory.forConcreteInjectable(type),
                directScope, InjectorType.Type, getWiringTypes(type, directScope));
        processInjectionPoints(typeInjector, builder);
        processProducerMethods(typeInjector, builder);
        processProducerFields(typeInjector, builder);
        maybeProcessAsProvider(typeInjector, builder);
      }
    }
  }

  private boolean isTypeInjectableCandidate(MetaClass type) {
    final boolean isMemberClass = (type.asClass() != null && type.asClass().isMemberClass());
    return type.isPublic() && type.isConcrete() && !isMemberClass;
  }

  private boolean isSimpleton(final MetaClass type) {
    if (type.isAnnotationPresent(Alternative.class) || hasEnablingProperty(type)) {
      return false;
    }
    final Class<? extends Annotation> scope = getDirectScope(type);
    if (!(scope.equals(Dependent.class) && !type.isAnnotationPresent(Dependent.class))) {
      return false;
    }
    if (!getInjectableConstructors(type).isEmpty()) {
      return false;
    }
    final Collection<Class<? extends Annotation>> injectAnnos = injectionContext.getAnnotationsForElementType(WiringElementType.InjectionPoint);
    for (final Class<? extends Annotation> anno : injectAnnos) {
      if (!type.getFieldsAnnotatedWith(anno).isEmpty()) {
        return false;
      }
    }

    return type.isDefaultInstantiable();
  }

  private WiringElementType[] getWiringTypes(final MetaClass type, final Class<? extends Annotation> directScope) {
    final List<WiringElementType> wiringTypes = new ArrayList<WiringElementType>();
    if (Dependent.class.equals(directScope)) {
      wiringTypes.add(WiringElementType.DependentBean);
    } else {
      wiringTypes.add(WiringElementType.NormalScopedBean);
    }

    if (type.isAnnotationPresent(Alternative.class)) {
      wiringTypes.add(WiringElementType.AlternativeBean);
    }

    return wiringTypes.toArray(new WiringElementType[wiringTypes.size()]);
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
    final Injectable providedInjectable = builder.addConcreteInjectable(providedType, qualFactory.forUniversallyQualified(),
            Dependent.class, InjectorType.ContextualProvider, WiringElementType.TopLevelProvider);
    final Injectable abstractProviderInjectable = builder.lookupAbstractInjectable(providerImpl, providerInjectable.getQualifier());
    builder.addDependency(providedInjectable, builder.createProducerInstanceDependency(abstractProviderInjectable));
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
    final Injectable providedInjectable = builder.addConcreteInjectable(providedType, qualFactory.forUniversallyQualified(),
            Dependent.class, InjectorType.Provider, WiringElementType.TopLevelProvider);
    final Injectable abstractProviderImplInjectable = builder.lookupAbstractInjectable(providerImplInjectable.getInjectedType(), providerImplInjectable.getQualifier());
    builder.addDependency(providedInjectable, builder.createProducerInstanceDependency(abstractProviderImplInjectable));
  }

  private void processProducerMethods(final Injectable typeInjectable, final DependencyGraphBuilder builder) {
    final MetaClass type = typeInjectable.getInjectedType();
    final Collection<Class<? extends Annotation>> producerAnnos = injectionContext.getAnnotationsForElementType(WiringElementType.ProducerElement);
    for (final Class<? extends Annotation> anno : producerAnnos) {
      final List<MetaMethod> methods = type.getMethodsAnnotatedWith(anno);
      for (final MetaMethod method : methods) {
        final Class<? extends Annotation> directScope = getDirectScope(method);
        final WiringElementType wiringTypes = getWiringTypesForScopeAnnotation(directScope);
        final Injectable producedInjectable = builder.addConcreteInjectable(method.getReturnType(),
                qualFactory.forConcreteInjectable(method), directScope, InjectorType.Producer, wiringTypes);
        final Injectable abstractProducerInjectable = builder.lookupAbstractInjectable(typeInjectable.getInjectedType(), typeInjectable.getQualifier());
        builder.addDependency(producedInjectable, builder.createProducerInstanceDependency(abstractProducerInjectable));
        final MetaParameter[] params = method.getParameters();
        for (int i = 0; i < params.length; i++) {
          final MetaParameter param = params[i];
          final Injectable abstractParamInjectable = builder.lookupAbstractInjectable(param.getType(), qualFactory.forAbstractInjectable(param));
          builder.addDependency(producedInjectable, builder.createProducerParamDependency(abstractParamInjectable, i));
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

  private void processProducerFields(final Injectable concreteInjectable, final DependencyGraphBuilder builder) {
    final MetaClass type = concreteInjectable.getInjectedType();
    final Collection<Class<? extends Annotation>> producerAnnos = injectionContext.getAnnotationsForElementType(WiringElementType.ProducerElement);
    for (final Class<? extends Annotation> anno : producerAnnos) {
      final List<MetaField> fields = type.getFieldsAnnotatedWith(anno);
      for (final MetaField field : fields) {
        final Injectable producedInjectable = builder.addConcreteInjectable(field.getType(),
                qualFactory.forConcreteInjectable(field), anno, InjectorType.Producer,
                getWiringTypesForScopeAnnotation(anno));
        final Injectable abstractProducerInjectable = builder
                .lookupAbstractInjectable(concreteInjectable.getInjectedType(), concreteInjectable.getQualifier());
        builder.addDependency(producedInjectable, builder.createProducerInstanceDependency(abstractProducerInjectable));
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
        final Injectable abstractInjectable = builder.lookupAbstractInjectable(field.getType(), qualFactory.forAbstractInjectable(field));
        builder.addDependency(typeInjector, builder.createFieldDependency(abstractInjectable, field));
      }
    }
  }

  private void addConstructorInjectionPoints(final Injectable concreteInjectable, final MetaConstructor injectableConstructor, final DependencyGraphBuilder builder) {
    final MetaParameter[] params = injectableConstructor.getParameters();
    for (int i = 0; i < params.length; i++) {
      final MetaParameter param = params[i];
      final Injectable abstractInjectable = builder.lookupAbstractInjectable(param.getType(), qualFactory.forAbstractInjectable(param));
      builder.addDependency(concreteInjectable, builder.createConstructorDependency(abstractInjectable, i));
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
    return type.isConcrete() && type.isPublic() &&  isEnabled(type) && isConstructable(type);
  }

  private boolean isConstructable(final MetaClass type) {
    final List<MetaConstructor> injectableConstructors = getInjectableConstructors(type);

    if (injectableConstructors.size() > 1) {
      throw new RuntimeException(type.getFullyQualifiedName() + " has " + injectableConstructors.size() + " constructors annotated with @Inject.");
    } else if (injectableConstructors.size() == 1) {
      // Needs to be proxiable is normal scoped.
      return getDirectScope(type).equals(Dependent.class) || type.isDefaultInstantiable();
    } else {
      return type.isDefaultInstantiable();
    }
  }

  private List<MetaConstructor> getInjectableConstructors(final MetaClass type) {
    final Collection<Class<? extends Annotation>> injectAnnotations = injectionContext.getAnnotationsForElementType(WiringElementType.InjectionPoint);
    final List<MetaConstructor> cons = new ArrayList<MetaConstructor>();
    for (final MetaConstructor con : type.getConstructors()) {
      for (final Class<? extends Annotation> anno : injectAnnotations) {
        if (con.isAnnotationPresent(anno)) {
          cons.add(con);
        }
      }
    }

    return cons;
  }

  private boolean isEnabled(final MetaClass type) {
    final boolean hasEnablingProperty = hasEnablingProperty(type);

    return (hasEnablingProperty && isEnabledByProperty(type)) || (!hasEnablingProperty && isActive(type));
  }

  private boolean isActive(final MetaClass type) {
    if (type.isAnnotationPresent(Alternative.class)) {
      return isAlternativeEnabled(type);
    } else {
      return true;
    }
  }

  private boolean isAlternativeEnabled(final MetaClass type) {
    if (alternatives == null) {
      final String userDefinedAlternatives = EnvUtil.getEnvironmentConfig().getFrameworkOrSystemProperty("errai.ioc.enabled.alternatives");
      if (userDefinedAlternatives != null) {
        alternatives = new HashSet<String>(Arrays.asList(userDefinedAlternatives.split("\\s+")));
      } else {
        alternatives = Collections.emptyList();
      }
    }

    return alternatives.contains(type.getFullyQualifiedName());
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

}
