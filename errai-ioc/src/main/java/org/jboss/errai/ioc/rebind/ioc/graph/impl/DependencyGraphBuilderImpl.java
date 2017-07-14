/*
 * Copyright (C) 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.errai.ioc.rebind.ioc.graph.impl;

import static java.util.stream.Collectors.toCollection;
import static org.jboss.errai.ioc.rebind.ioc.graph.impl.ResolutionPriority.getMatchingPriority;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.enterprise.inject.Produces;

import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaClassMember;
import org.jboss.errai.codegen.meta.MetaField;
import org.jboss.errai.codegen.meta.MetaMethod;
import org.jboss.errai.codegen.meta.MetaParameter;
import org.jboss.errai.ioc.client.api.EntryPoint;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraph;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder;
import org.jboss.errai.ioc.rebind.ioc.graph.api.Injectable;
import org.jboss.errai.ioc.rebind.ioc.graph.api.InjectionSite;
import org.jboss.errai.ioc.rebind.ioc.graph.api.Qualifier;
import org.jboss.errai.ioc.rebind.ioc.graph.api.QualifierFactory;
import org.jboss.errai.ioc.rebind.ioc.injector.api.InjectableProvider;
import org.jboss.errai.ioc.rebind.ioc.injector.api.WiringElementType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * @see DependencyGraphBuilder
 * @author Max Barkley <mbarkley@redhat.com>
 */
public final class DependencyGraphBuilderImpl implements DependencyGraphBuilder {

  private static final Logger logger = LoggerFactory.getLogger(DependencyGraphBuilder.class);

  private final QualifierFactory qualFactory;
  private final Map<InjectableHandle, InjectableHandle> cachedHandles = new HashMap<>();
  private final Multimap<MetaClass, Injectable> injectablesByType = HashMultimap.create();
  private final Multimap<Injectable, Produced> producedByProducer = HashMultimap.create();
  private final Map<String, Injectable> injectablesByName = new HashMap<>();
  private final List<InjectableImpl> specializations = new ArrayList<>();
  private final FactoryNameGenerator nameGenerator = new FactoryNameGenerator();
  private final boolean async;

  public DependencyGraphBuilderImpl(final QualifierFactory qualFactory, final boolean async) {
    this.qualFactory = qualFactory;
    this.async = async;
  }

  @Override
  public Injectable addInjectable(final MetaClass injectedType, final Qualifier qualifier,
          final Predicate<InjectableHandle> matchPredicate, final Class<? extends Annotation> literalScope,
          final InjectableType injectableType, final WiringElementType... wiringTypes) {
    final InjectableImpl injectable = new InjectableImpl(injectedType, qualifier, matchPredicate,
            nameGenerator.generateFor(injectedType, qualifier, injectableType), literalScope, injectableType,
            Arrays.asList(wiringTypes));

    return registerNewInjectable(injectable);
  }

  private Injectable registerNewInjectable(final InjectableImpl injectable) {
    logAddedInjectable(injectable);
    final String factoryName = injectable.getFactoryName();
    if (injectablesByName.containsKey(factoryName)) {
      GraphUtil.throwDuplicateConcreteInjectableException(factoryName, injectablesByName.get(factoryName), injectable);
    }
    injectablesByName.put(factoryName, injectable);
    if (injectable.wiringTypes.contains(WiringElementType.Specialization)) {
      specializations.add(injectable);
    }
    mapToAssignableTypes(injectable);

    return injectable;
  }

  private void logAddedInjectable(final Injectable injectable) {
    logger.debug("Adding new injectable: {}", injectable);
    if (logger.isTraceEnabled()) {
      logger.trace("Injectable type: {}", injectable.getInjectableType());
      logger.trace("Injectable wiring types: {}", injectable.getWiringElementTypes());
    }
  }

  @Override
  public Injectable addExtensionInjectable(final MetaClass injectedType, final Qualifier qualifier,
          final Predicate<InjectableHandle> matchPredicate, final InjectableProvider provider,
          final WiringElementType... wiringTypes) {
    final InjectableImpl injectable = new ExtensionInjectable(injectedType, qualifier, matchPredicate,
            nameGenerator.generateFor(injectedType, qualifier, InjectableType.Extension), null,
            InjectableType.Extension, Arrays.asList(wiringTypes), provider);
    return registerNewInjectable(injectable);
  }

  private void mapToAssignableTypes(final Injectable inj) {
    for (final MetaClass assignable : inj.getInjectedType().getAllSuperTypesAndInterfaces()) {
      try {
        injectablesByType.put(assignable.getErased(), inj);
      } catch (final Throwable t) {
        throw new RuntimeException("Error occurred adding the assignable type " + assignable.getFullyQualifiedName(), t);
      }
    }
  }

  private InjectableHandle lookupInjectableHandle(final MetaClass type, final Qualifier qualifier) {
    final InjectableHandle lookupHandle = new InjectableHandle(type, qualifier);
    return cachedHandles.computeIfAbsent(lookupHandle, Function.identity());
  }

  private void addDependency(final Injectable injectable, final Dependency dependency) {
    assert (injectable instanceof InjectableImpl);
    if (InjectableType.Disabled.equals(injectable.getInjectableType())
            && (!DependencyType.ProducerMember.equals(dependency.getDependencyType())
                    || !injectable.getDependencies().isEmpty())) {
      throw new RuntimeException("The injectable, " + injectable + ", is disabled."
              + " A disabled injectable may only have a single dependency if it is produced by a disabled bean.");
    }

    final InjectableImpl injectableAsImpl = (InjectableImpl) injectable;
    injectableAsImpl.dependencies.add(dependency);
  }

  @Override
  public DependencyGraph createGraph(final ReachabilityStrategy strategy) {
    logger.debug("Creating dependency graph...");
    resolveSpecializations();
    final Map<Dependency, Injectable> resolved = resolveDependencies();
    final DependencyGraphImpl graph = new DependencyGraphImpl(injectablesByName, resolved);
    validateInjectables(graph);
    removeUnreachableInjectables(graph, strategy);
    logger.debug("Finished creating dependency graph.");

    return graph;
  }

  private Collection<Validator> createValidators() {
    final Collection<Validator> validators = new ArrayList<>();
    validators.add(new CycleValidator());
    if (async) {
      validators.add(new AsyncValidator());
    }

    return validators;
  }

  private void resolveSpecializations() {
    logger.debug("Processing {} specializations...", specializations.size());
    final Set<Injectable> toBeRemoved = new HashSet<>();
    final List<Runnable> updateQualifiers = new ArrayList<>();
    GraphUtil.sortSuperTypesBeforeSubtypes(specializations);
    for (final InjectableImpl specialization : specializations) {
      if (specialization.injectableType.equals(InjectableType.Producer)) {
        resolveProducerSpecialization(specialization, toBeRemoved, updateQualifiers);
      } else {
        resolveTypeSpecialization(specialization, toBeRemoved, updateQualifiers);
      }
    }
    new ArrayList<>(toBeRemoved)
    .stream()
    .flatMap(inj -> producedByProducer.get(inj).stream())
    .map(produced -> produced.getProduced())
    .collect(toCollection(() -> toBeRemoved));

    logger.debug("Removed {} beans that were specialized.", toBeRemoved.size());
    logger.trace("Types removed by specialization: {}", toBeRemoved);
    injectablesByName.values().removeAll(toBeRemoved);
    injectablesByType.values().removeAll(toBeRemoved);
    producedByProducer.keys().removeAll(toBeRemoved);
    updateQualifiers.forEach(task -> task.run());
  }

  private void resolveProducerSpecialization(final InjectableImpl specialization, final Set<Injectable> toBeRemoved,
          final List<Runnable> updateQualifiers) {
    final ProducerMemberDependency producerMemberDep = GraphUtil.findProducerInstanceDep(specialization);
    final Injectable[] specializedInjectable = new Injectable[] { null };
    if (producerMemberDep.getProducingMember() instanceof MetaMethod) {
      final MetaMethod specializedMethod = GraphUtil.getOverridenMethod((MetaMethod) producerMemberDep.getProducingMember());
      final MetaClass specializedType = specializedMethod.getReturnType();
      if (specializedMethod != null && specializedMethod.isAnnotationPresent(Produces.class)) {
        injectablesByType
          .get(specializedType)
          .stream()
          .filter(inj -> inj.getInjectableType().equals(InjectableType.Producer))
          .filter(inj -> {
            final ProducerMemberDependency producerDep = GraphUtil.findProducerInstanceDep(inj);

            return producerDep.getProducingMember().equals(specializedMethod);
          })
          .findFirst()
          .ifPresent(inj -> {
            toBeRemoved.add(inj);
            specializedInjectable[0] = inj;
          });
        if (specializedInjectable[0] != null) {
          updateQualifiers.add(() -> specialization.qualifier = qualFactory.combine(specialization.qualifier,
                  specializedInjectable[0].getQualifier()));
        }
      }
    } else {
      throw new RuntimeException("Specialized producers can only be methods. Found " + producerMemberDep.getProducingMember()
              + " in " + producerMemberDep.getProducingMember().getDeclaringClassName());
    }
  }

  private void resolveTypeSpecialization(final InjectableImpl specialization, final Set<Injectable> toBeRemoved,
          final List<Runnable> updateQualifiers) {
    final MetaClass specializedType = specialization.type.getSuperClass().getErased();
    final Injectable[] specializedInjectable = new Injectable[] { null };
    injectablesByType
      .get(specializedType)
      .stream()
      .filter(inj -> inj.getInjectableType().equals(InjectableType.Type))
      .filter(inj -> inj.getInjectedType().equals(specializedType))
      .filter(inj -> !inj.equals(specialization))
      .findFirst()
      .ifPresent(inj -> {
        toBeRemoved.add(inj);
        specializedInjectable[0] = inj;
      });

    if (specializedInjectable[0] != null) {
      updateQualifiers.add(() -> specialization.qualifier = qualFactory.combine(specialization.qualifier,
              specializedInjectable[0].getQualifier()));
    }
  }

  private void validateInjectables(final DependencyGraph graph) {
    logger.debug("Validating dependency graph...");
    final Collection<String> problems = new ArrayList<>();
    final Collection<Validator> validators = createValidators();
    for (final Injectable injectable : injectablesByName.values()) {
      for (final Validator validator : validators) {
        if (validator.canValidate(graph, injectable)) {
          validator.validate(graph, injectable, problems);
        }
      }
    }
    if (!problems.isEmpty()) {
      throw new RuntimeException(GraphUtil.combineProblemMessages(problems));
    }
  }

  private void removeUnreachableInjectables(final DependencyGraph graph, final ReachabilityStrategy strategy) {
    logger.debug("Removing unreachable injectables from dependency graph using {} strategy.", strategy);
    final Set<String> reachableNames = new HashSet<>();
    final Queue<Injectable> processingQueue = new LinkedList<>();
    final Predicate<Injectable> reachabilityRoot = reachabilityRootPredicate(strategy);
    for (final Injectable injectable : injectablesByName.values()) {
      if (reachabilityRoot.test(injectable)
              && !reachableNames.contains(injectable.getFactoryName())
              && !InjectableType.Disabled.equals(injectable.getInjectableType())) {
        processingQueue.add(injectable);
        do {
          final Injectable processedInjectable = processingQueue.poll();
          reachableNames.add(processedInjectable.getFactoryName());
          logger.trace("Marked as reachable: {}", processedInjectable);
          for (final Dependency dep : processedInjectable.getDependencies()) {
            final Injectable resolvedDep = graph.getResolved(dep);
            if (resolvedDep != null && !reachableNames.contains(resolvedDep.getFactoryName())) {
              processingQueue.add(resolvedDep);
            }
          }
        } while (processingQueue.size() > 0);
      }
    }

    final int initialSize = injectablesByName.size();
    injectablesByName.keySet().retainAll(reachableNames);
    logger.debug("Removed {} unreachable injectables.", initialSize - injectablesByName.size());
  }

  private Predicate<Injectable> reachabilityRootPredicate(final ReachabilityStrategy strategy) {
    switch (strategy) {
    case All:
      return inj -> true;
    case Annotated:
      return inj -> !inj.getWiringElementTypes().contains(WiringElementType.Simpleton);
    case Aggressive:
      return inj -> EntryPoint.class.equals(inj.getScope()) || inj.getWiringElementTypes().contains(WiringElementType.JsType);
    default:
      throw new RuntimeException("Unrecognized reachability strategy, " + strategy.toString());
    }
  }

  private Map<Dependency, Injectable> resolveDependencies() {
    logger.debug("Resolving dependencies for {} injectables...", injectablesByName.size());

    final Map<Dependency, Injectable> resolved = new HashMap<>();
    final Set<String> transientInjectableNames = new HashSet<>();
    final List<String> dependencyProblems = new ArrayList<>();
    final Map<String, Injectable> customProvidedInjectables = new IdentityHashMap<>();

    resolveProducerDependencies(resolved);
    for (final Injectable injectable : injectablesByName.values()) {
      if (injectable.isExtension()) {
        transientInjectableNames.add(injectable.getFactoryName());
      }
      logger.debug("Resolving {} dependencies for: {}", injectable.getDependencies().size(), injectable);
      for (final Dependency dep : injectable.getDependencies()) {
        resolveDependency(dep, injectable, dependencyProblems, customProvidedInjectables, resolved);
      }
    }

    injectablesByName.keySet().removeAll(transientInjectableNames);
    injectablesByName.putAll(customProvidedInjectables);

    if (!dependencyProblems.isEmpty()) {
      throw new RuntimeException(GraphUtil.buildMessageFromProblems(dependencyProblems));
    }

    return resolved;
  }

  private void resolveProducerDependencies(final Map<Dependency, Injectable> resolved) {
    producedByProducer
      .entries()
      .stream()
      .forEach(e -> resolved.put(e.getValue().getDep(), e.getKey()));
  }

  private Injectable resolveDependency(final Dependency dependency, final Injectable depOwner,
          final Collection<String> problems, final Map<String, Injectable> customProvidedInjectables,
          final Map<Dependency, Injectable> resolved) {
    return resolved.computeIfAbsent(dependency,
            dep -> doResolveDependency(dep, depOwner, problems, customProvidedInjectables, resolved));
  }

  private Injectable doResolveDependency(final Dependency dep, final Injectable depOwner, final Collection<String> problems,
          final Map<String, Injectable> customProvidedInjectables, final Map<Dependency, Injectable> allResolved) {
    logger.trace("Resolving dependency: {}", dep);
    if (InjectableType.Producer.equals(depOwner.getInjectableType())
            && DependencyType.ProducerMember.equals(dep.getDependencyType())
            && ((ProducerMemberDependency) dep).getProducingMember().isStatic()) {
      return null;
    }

    final Multimap<ResolutionPriority, Injectable> resolvedByPriority = gatherMatches(dep.getHandle());

    final Iterable<ResolutionPriority> priorities;
    final boolean reportProblems;
    if (InjectableType.Disabled.equals(depOwner.getInjectableType())) {
      priorities = Collections.singleton(ResolutionPriority.Disabled);
      reportProblems = false;
    }
    else if (InjectableType.Producer.equals(depOwner.getInjectableType())
            && DependencyType.ProducerMember.equals(dep.getDependencyType())
            && ((ProducerMemberDependency) dep).getProducingMember().isStatic()) {
      priorities = Collections.emptyList();
      reportProblems = false;
    }
    else {
      priorities = ResolutionPriority.enabledValues();
      reportProblems = true;
    }

    // Iterates through priorities from highest to lowest.
    for (final ResolutionPriority priority : priorities) {
      if (resolvedByPriority.containsKey(priority)) {
        final Collection<Injectable> resolved = resolvedByPriority.get(priority);
        if (resolved.size() > 1) {
          if (reportProblems) {
            problems.add(GraphUtil.ambiguousDependencyMessage(dep, depOwner, new ArrayList<>(resolved)));
          }

          return null;
        } else {
          final Injectable injectable = maybeProcessAsExtension(dep, depOwner, customProvidedInjectables, resolvedByPriority, resolved);
          logger.trace("Resolved dependency: {}", injectable);

          return injectable;
        }
      }
    }

    if (reportProblems) {
      final Collection<Injectable> resolvedDisabledInjectables =
              resolvedByPriority
                .get(ResolutionPriority.Disabled)
                .stream()
                .map(inj -> getRootDisabledInjectable(inj, problems, customProvidedInjectables, allResolved))
                .collect(Collectors.toList());

      problems.add(GraphUtil.unsatisfiedDependencyMessage(dep, depOwner, resolvedDisabledInjectables));
    }
    return null;
  }

  private Injectable maybeProcessAsExtension(final Dependency dep, final Injectable depOwner,
          final Map<String, Injectable> customProvidedInjectables,
          final Multimap<ResolutionPriority, Injectable> resolvedByPriority,
          final Collection<Injectable> resolved) {
    Injectable injectable = resolved.iterator().next();
    if (injectable.isExtension()) {
      final ExtensionInjectable providedInjectable = (ExtensionInjectable) injectable;
      final Collection<Injectable> otherResolvedInjectables = new ArrayList<>(resolvedByPriority.values());
      otherResolvedInjectables.remove(injectable);

      final InjectionSite site = new InjectionSite(depOwner.getInjectedType(), dep, otherResolvedInjectables);
      injectable = providedInjectable.provider.getInjectable(site, nameGenerator);
      customProvidedInjectables.put(injectable.getFactoryName(), injectable);
    }

    return injectable;
  }

  private Multimap<ResolutionPriority, Injectable> gatherMatches(final InjectableHandle injectableHandle) {
    final Multimap<ResolutionPriority, Injectable> resolvedByPriority = HashMultimap.create();
    injectablesByType
      .get(injectableHandle.getType().getErased())
      .stream()
      .filter(inj -> GraphUtil.candidateSatisfiesInjectable(injectableHandle, inj))
      .filter(inj -> inj.getMatchPredicate().test(injectableHandle))
      .forEach(inj -> resolvedByPriority.put(getMatchingPriority(inj), inj));

    return resolvedByPriority;
  }

  private Injectable getRootDisabledInjectable(Injectable inj, final Collection<String> problems,
          final Map<String, Injectable> customProvidedInjectables, final Map<Dependency, Injectable> allResolved) {
    while (inj.getDependencies().size() == 1) {
      final Dependency dep = inj.getDependencies().iterator().next();
      if (DependencyType.ProducerMember.equals(dep.getDependencyType())) {
        inj = resolveDependency(dep, inj, problems, customProvidedInjectables, allResolved);
      }
    }

    return inj;
  }

  @Override
  public void addFieldDependency(final Injectable concreteInjectable, final MetaClass type, final Qualifier qualifier,
          final MetaField dependentField) {
    final InjectableHandle injectableReference = lookupInjectableHandle(type, qualifier);
    final FieldDependency dep = new FieldDependencyImpl(injectableReference, dependentField);
    addDependency(concreteInjectable, dep);
  }

  @Override
  public void addConstructorDependency(final Injectable concreteInjectable, final MetaClass type,
          final Qualifier qualifier, final int paramIndex, final MetaParameter param) {
    final InjectableHandle injectableReference = lookupInjectableHandle(type, qualifier);
    final ParamDependency dep = new ParamDependencyImpl(injectableReference, DependencyType.Constructor, paramIndex,
            param);
    addDependency(concreteInjectable, dep);
  }

  @Override
  public void addProducerParamDependency(final Injectable concreteInjectable, final MetaClass type,
          final Qualifier qualifier, final int paramIndex, final MetaParameter param) {
    final InjectableHandle injectableReference = lookupInjectableHandle(type, qualifier);
    final ParamDependency dep = new ParamDependencyImpl(injectableReference, DependencyType.ProducerParameter,
            paramIndex, param);
    addDependency(concreteInjectable, dep);
  }

  @Override
  public void addStaticProducerMemberDependency(final Injectable concreteInjectable, final MetaClassMember producingMember) {
    if (!producingMember.isStatic()) {
      throw new IllegalArgumentException(
              "Cannot use non-static member, " + producingMember + ", for static producer member dependency.");
    }
    final InjectableHandle injectableReference = lookupInjectableHandle(producingMember.getDeclaringClass(),
            qualFactory.forUniversallyQualified());
    final ProducerMemberDependency dep = new ProducerMemberDependencyImpl(injectableReference,
            DependencyType.ProducerMember, producingMember);
    addDependency(concreteInjectable, dep);
  }

  @Override
  public void addProducerMemberDependency(final Injectable concreteInjectable, final MetaClassMember producingMember,
          final Injectable producer) {
    if (producingMember.isStatic()) {
      throw new IllegalArgumentException(
              "Cannot use static member, " + producingMember + ", for instance producer member dependency.");
    }
    final InjectableHandle injectableReference = lookupInjectableHandle(producer.getInjectedType(), producer.getQualifier());
    final ProducerMemberDependency dep = new ProducerMemberDependencyImpl(injectableReference,
            DependencyType.ProducerMember, producingMember);
    addDependency(concreteInjectable, dep);
    producedByProducer.put(producer, new Produced(concreteInjectable, dep));
  }

  @Override
  public void addSetterMethodDependency(final Injectable concreteInjectable, final MetaClass type,
          final Qualifier qualifier, final MetaMethod setter) {
    final InjectableHandle injectableReference = lookupInjectableHandle(type, qualifier);
    final SetterParameterDependency dep = new SetterParameterDependencyImpl(injectableReference, setter);
    addDependency(concreteInjectable, dep);
  }

  @Override
  public void addDisposesMethodDependency(final Injectable concreteInjectable, final MetaClass type, final Qualifier qualifier, final MetaMethod disposer) {
    final InjectableHandle injectableReference = lookupInjectableHandle(type, qualifier);
    final DisposerMethodDependency dep = new DisposerMethodDependencyImpl(injectableReference, disposer);
    addDependency(concreteInjectable, dep);
  }

  @Override
  public void addDisposesParamDependency(final Injectable concreteInjectable, final MetaClass type, final Qualifier qualifier,
          final Integer index, final MetaParameter param) {
    final InjectableHandle injectableReference = lookupInjectableHandle(type, qualifier);
    final ParamDependency dep = new ParamDependencyImpl(injectableReference, DependencyType.DisposerParameter, index,
            param);
    addDependency(concreteInjectable, dep);
  }

  private static class Produced {
    private final Injectable produced;
    private final ProducerMemberDependency dep;
    Produced(final Injectable produced, final ProducerMemberDependency dep) {
      this.produced = produced;
      this.dep = dep;
    }
    public Injectable getProduced() {
      return produced;
    }
    public ProducerMemberDependency getDep() {
      return dep;
    }
    @Override
    public String toString() {
      return "Produced [produced=" + produced + ", dep=" + dep + "]";
    }
  }

}
