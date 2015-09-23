package org.jboss.errai.ioc.rebind.ioc.graph.impl;

import static org.jboss.errai.ioc.rebind.ioc.graph.impl.ResolutionPriority.getMatchingPriority;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javax.enterprise.inject.Produces;

import org.apache.commons.lang3.Validate;
import org.jboss.errai.codegen.meta.HasAnnotations;
import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaClassMember;
import org.jboss.errai.codegen.meta.MetaField;
import org.jboss.errai.codegen.meta.MetaMethod;
import org.jboss.errai.codegen.meta.MetaParameter;
import org.jboss.errai.codegen.meta.MetaParameterizedType;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraph;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder;
import org.jboss.errai.ioc.rebind.ioc.graph.api.Injectable;
import org.jboss.errai.ioc.rebind.ioc.graph.api.ProvidedInjectable.InjectionSite;
import org.jboss.errai.ioc.rebind.ioc.graph.api.Qualifier;
import org.jboss.errai.ioc.rebind.ioc.graph.api.QualifierFactory;
import org.jboss.errai.ioc.rebind.ioc.injector.api.WiringElementType;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * @author Max Barkley <mbarkley@redhat.com>
 */
public class DependencyGraphBuilderImpl implements DependencyGraphBuilder {

  private final QualifierFactory qualFactory;
  private final Map<InjectableHandle, AbstractInjectable> abstractInjectables = new HashMap<InjectableHandle, AbstractInjectable>();
  private final Multimap<MetaClass, AbstractInjectable> directAbstractInjectablesByAssignableTypes = HashMultimap.create();
  private final Collection<AbstractInjectable> subTypeMatchingInjectables = new ArrayList<AbstractInjectable>();
  private final Map<String, ConcreteInjectable> concretesByName = new HashMap<String, ConcreteInjectable>();
  private final List<ConcreteInjectable> specializations = new ArrayList<ConcreteInjectable>();

  public DependencyGraphBuilderImpl(final QualifierFactory qualFactory) {
    this.qualFactory = qualFactory;
  }

  @Override
  public Injectable addConcreteInjectable(final MetaClass injectedType, final Qualifier qualifier, Class<? extends Annotation> literalScope,
          final InjectableType factoryType, final WiringElementType... wiringTypes) {
    final ConcreteInjectable concrete = new ConcreteInjectable(injectedType, qualifier, literalScope, factoryType, Arrays.asList(wiringTypes));
    return registerNewConcreteInjectable(concrete);
  }

  private Injectable registerNewConcreteInjectable(final ConcreteInjectable concrete) {
    final String factoryName = concrete.getFactoryName();
    if (concretesByName.containsKey(factoryName)) {
      throwDuplicateConcreteInjectableException(factoryName, concretesByName.get(factoryName), concrete);
    }
    concretesByName.put(factoryName, concrete);
    linkDirectAbstractInjectable(concrete);
    if (concrete.wiringTypes.contains(WiringElementType.Specialization)) {
      specializations.add(concrete);
    }

    return concrete;
  }

  @Override
  public Injectable addTransientInjectable(final MetaClass injectedType, final Qualifier qualifier,
          final Class<? extends Annotation> literalScope, final WiringElementType... wiringTypes) {
    final ConcreteInjectable concrete = new TransientInjectable(injectedType, qualifier, literalScope, InjectableType.Extension, Arrays.asList(wiringTypes));
    return registerNewConcreteInjectable(concrete);
  }

  private void throwDuplicateConcreteInjectableException(final String name, final ConcreteInjectable first,
          final ConcreteInjectable second) {
    final String message = "Two concrete injectables exist with the same name (" + name + "):\n"
                            + "\t" + first + "\n"
                            + "\t" + second;

    throw new RuntimeException(message);
  }

  private void linkDirectAbstractInjectable(final ConcreteInjectable concreteInjectable) {
    if (concreteInjectable.wiringTypes.contains(WiringElementType.SubTypeMatching)) {
      final AbstractInjectable subTypeMatchingInjectable = new AbstractInjectable(concreteInjectable.type, concreteInjectable.qualifier);
      subTypeMatchingInjectables.add(subTypeMatchingInjectable);
      subTypeMatchingInjectable.linked.add(concreteInjectable);
    } else if (concreteInjectable.wiringTypes.contains(WiringElementType.ExactTypeMatching)) {
      final AbstractInjectable exactTypeMatchingInjectable = new AbstractInjectable(concreteInjectable.type, concreteInjectable.qualifier);
      exactTypeMatchingInjectable.linked.add(concreteInjectable);
      directAbstractInjectablesByAssignableTypes.put(concreteInjectable.type.getErased(), exactTypeMatchingInjectable);
    } else {
      final AbstractInjectable abstractInjectable = lookupAsAbstractInjectable(concreteInjectable.type, concreteInjectable.qualifier);
      abstractInjectable.linked.add(concreteInjectable);
    }
  }

  private void processAssignableTypes(final AbstractInjectable abstractInjectable) {
    for (final MetaClass assignable : abstractInjectable.type.getAllSuperTypesAndInterfaces()) {
      directAbstractInjectablesByAssignableTypes.put(assignable.getErased(), abstractInjectable);
    }
  }

  private Injectable lookupAbstractInjectable(final MetaClass type, final Qualifier qualifier) {
    return lookupAsAbstractInjectable(type, qualifier);
  }

  private AbstractInjectable lookupAsAbstractInjectable(final MetaClass type, final Qualifier qualifier) {
    final InjectableHandle handle = new InjectableHandle(type, qualifier);
    AbstractInjectable abstractInjectable = abstractInjectables.get(handle);
    if (abstractInjectable == null) {
      abstractInjectable = new AbstractInjectable(type, qualifier);
      abstractInjectables.put(handle, abstractInjectable);
      processAssignableTypes(abstractInjectable);
    }

    return abstractInjectable;
  }

  private void addDependency(final Injectable concreteInjectable, Dependency dependency) {
    assert (concreteInjectable instanceof ConcreteInjectable);

    final ConcreteInjectable concrete = (ConcreteInjectable) concreteInjectable;

    concrete.dependencies.add(BaseDependency.class.cast(dependency));
  }

  @Override
  public DependencyGraph createGraph(boolean removeUnreachable) {
    resolveSpecializations();
    linkAbstractInjectables();
    resolveDependencies();
    validateDependentScopedInjectables();
    if (removeUnreachable) {
      removeUnreachableConcreteInjectables();
    }

    return new DependencyGraphImpl();
  }

  private void resolveSpecializations() {
    final Set<ConcreteInjectable> toBeRemoved = new HashSet<ConcreteInjectable>();
    moveSuperTypesBeforeSubTypes(specializations);
    for (final ConcreteInjectable specialization : specializations) {
      if (specialization.injectableType.equals(InjectableType.Producer)) {
        resolveProducerSpecialization(specialization, toBeRemoved);
      } else {
        resolveTypeSpecialization(specialization, toBeRemoved);
      }
    }
    concretesByName.values().removeAll(toBeRemoved);
  }

  private ProducerInstanceDependencyImpl findProducerInstanceDep(final ConcreteInjectable concrete) {
    for (final BaseDependency dep : concrete.dependencies) {
      if (dep.dependencyType.equals(DependencyType.ProducerMember)) {
        return (ProducerInstanceDependencyImpl) dep;
      }
    }
    throw new RuntimeException("Could not find producer member.");
  }

  private void resolveProducerSpecialization(final ConcreteInjectable specialization, final Set<ConcreteInjectable> toBeRemoved) {
    final ProducerInstanceDependencyImpl producerMemberDep = findProducerInstanceDep(specialization);
    if (producerMemberDep.producingMember instanceof MetaMethod) {
      final MetaMethod specializedMethod = getOverridenMethod((MetaMethod) producerMemberDep.producingMember);
      final MetaClass specializingType = producerMemberDep.producingMember.getDeclaringClass();
      if (specializedMethod != null && specializedMethod.isAnnotationPresent(Produces.class)) {
        updateLinksToSpecialized(specialization, toBeRemoved, specializedMethod, specializingType);
      }
    } else {
      throw new RuntimeException("Specialized producers can only be methods. Found " + producerMemberDep.producingMember
              + " in " + producerMemberDep.producingMember.getDeclaringClassName());
    }
  }

  private void updateLinksToSpecialized(final ConcreteInjectable specialization, final Set<ConcreteInjectable> toBeRemoved,
          final MetaMethod specializedMethod, final MetaClass specializingType) {
    final MetaClass enclosingType = specializedMethod.getDeclaringClass();
    final MetaClass producedType = specializedMethod.getReturnType().getErased();
    for (final AbstractInjectable injectable : directAbstractInjectablesByAssignableTypes.get(producedType)) {
      if (injectable.type.equals(producedType)) {
        final Iterator<BaseInjectable> linkedIter = injectable.linked.iterator();
        while (linkedIter.hasNext()) {
          final BaseInjectable link = linkedIter.next();
          if (link instanceof ConcreteInjectable) {
            final ConcreteInjectable concreteLink = (ConcreteInjectable) link;
            removeSpecializedAndSpecializingLinks(specialization, toBeRemoved, specializingType, enclosingType, linkedIter, concreteLink);
          }
        }
        injectable.linked.add(lookupAsAbstractInjectable(specialization.type, specialization.qualifier));
      }
    }
  }

  private void removeSpecializedAndSpecializingLinks(final ConcreteInjectable specialization, final Set<ConcreteInjectable> toBeRemoved,
          final MetaClass specializingType, final MetaClass enclosingType, final Iterator<BaseInjectable> linkedIter,
          final ConcreteInjectable concreteLink) {
    if (concreteLink.injectableType.equals(InjectableType.Producer)) {
      final MetaClass foundProducerType = findProducerInstanceDep(concreteLink).injectable.type.getErased();
      if (foundProducerType.equals(enclosingType.getErased())
              || foundProducerType.equals(specializingType.getErased())) {
        linkedIter.remove();
      }
      if (foundProducerType.equals(enclosingType.getErased())) {
        toBeRemoved.add(concreteLink);
        specialization.qualifier = qualFactory.combine(specialization.qualifier, concreteLink.qualifier);
      }
    }
  }

  private MetaMethod getOverridenMethod(final MetaMethod specializingMethod) {
    final MetaClass[] producerParams = getParameterTypes(specializingMethod);
    MetaClass enclosingType = specializingMethod.getDeclaringClass();
    MetaMethod specializedMethod = null;
    while (specializedMethod == null && enclosingType.getSuperClass() != null) {
      enclosingType = enclosingType.getSuperClass();
      specializedMethod = enclosingType.getDeclaredMethod(specializingMethod.getName(), producerParams);
    }

    return specializedMethod;
  }

  private MetaClass[] getParameterTypes(final MetaMethod producerMethod) {
    final MetaClass[] paramTypes = new MetaClass[producerMethod.getParameters().length];
    for (int i = 0; i < paramTypes.length; i++) {
      paramTypes[i] = producerMethod.getParameters()[i].getType();
    }

    return paramTypes;
  }

  private void resolveTypeSpecialization(final ConcreteInjectable specialization, final Set<ConcreteInjectable> toBeRemoved) {
    final MetaClass specializedType = specialization.type.getSuperClass().getErased();
    for (final AbstractInjectable injectable : directAbstractInjectablesByAssignableTypes.get(specializedType)) {
      if (injectable.type.equals(specializedType)) {
        updateSpecializedInjectableLinks(specialization, toBeRemoved, injectable);
        break;
      }
    }
  }

  private void updateSpecializedInjectableLinks(final ConcreteInjectable specialization, final Set<ConcreteInjectable> toBeRemoved,
          final AbstractInjectable injectable) {
    assert injectable.linked.size() == 1;
    final ConcreteInjectable specialized = (ConcreteInjectable) injectable.linked.iterator().next();
    specialization.qualifier = qualFactory.combine(specialization.qualifier, specialized.qualifier);
    toBeRemoved.add(specialized);
    injectable.linked.clear();
    injectable.linked.add(lookupAsAbstractInjectable(specialization.type, specialization.qualifier));
    removeLinksToProducedTypes(specialized, toBeRemoved);
  }

  private void removeLinksToProducedTypes(final ConcreteInjectable specialized, final Set<ConcreteInjectable> toBeRemoved) {
    final Collection<AbstractInjectable> producedReferences = new ArrayList<AbstractInjectable>();
    for (final MetaMethod method : specialized.type.getDeclaredMethodsAnnotatedWith(Produces.class)) {
      producedReferences.add(lookupAsAbstractInjectable(method.getReturnType(), qualFactory.forSource(method)));
    }
    for (final MetaField field : specialized.type.getDeclaredFields()) {
      if (field.isAnnotationPresent(Produces.class)) {
        producedReferences.add(lookupAsAbstractInjectable(field.getType(), qualFactory.forSource(field)));
      }
    }

    for (final AbstractInjectable reference : producedReferences) {
      final Iterator<BaseInjectable> linkIter = reference.linked.iterator();
      while (linkIter.hasNext()) {
        final BaseInjectable link = linkIter.next();
        if (link instanceof ConcreteInjectable && ((ConcreteInjectable) link).injectableType.equals(InjectableType.Producer)) {
          final ConcreteInjectable concreteLink = (ConcreteInjectable) link;
          final ProducerInstanceDependencyImpl producerMemberDep = findProducerInstanceDep(concreteLink);
          if (producerMemberDep.producingMember.getDeclaringClass().equals(specialized.type)) {
            linkIter.remove();
            toBeRemoved.add(concreteLink);
          }
        }
      }
    }
  }

  /**
   * Required so that subtypes get all the qualifiers of supertypes when there
   * are multiple @Specializes in the hierarchy.
   */
  private void moveSuperTypesBeforeSubTypes(final List<ConcreteInjectable> specializations) {
    Collections.sort(specializations, new Comparator<ConcreteInjectable>() {
      @Override
      public int compare(final ConcreteInjectable c1, final ConcreteInjectable c2) {
        return getScore(c1) - getScore(c2);
      }

      private int getScore(final ConcreteInjectable c) {
        if (c.injectableType.equals(InjectableType.Producer)) {
          return getDistanceFromObject(findProducerInstanceDep(c).producingMember.getDeclaringClass());
        } else {
          return getDistanceFromObject(c.type);
        }
      }

      private int getDistanceFromObject(MetaClass type) {
        int distance = 0;
        for (; type.getSuperClass() != null; type = type.getSuperClass()) {
          distance++;
        }

        return distance;
      }
    });
  }

  private void validateDependentScopedInjectables() {
    final Set<ConcreteInjectable> visiting = new LinkedHashSet<ConcreteInjectable>();
    final Set<ConcreteInjectable> visited = new HashSet<ConcreteInjectable>();
    final Collection<String> problems = new ArrayList<String>();
    for (final ConcreteInjectable injectable : concretesByName.values()) {
      if (injectable.wiringTypes.contains(WiringElementType.DependentBean) && !visited.contains(injectable)) {
        validateDependentScopedInjectable(injectable, visiting, visited, problems);
      }
    }
    if (!problems.isEmpty()) {
      throw new RuntimeException(combineProblemMessages(problems));
    }
  }

  private String combineProblemMessages(final Collection<String> problems) {
    final StringBuilder builder = new StringBuilder();
    for (final String problem : problems) {
      builder.append(problem)
             .append("\n");
    }

    return builder.toString();
  }

  private void validateDependentScopedInjectable(final ConcreteInjectable injectable, final Set<ConcreteInjectable> visiting,
          final Set<ConcreteInjectable> visited, final Collection<String> problems) {
    if (visiting.contains(injectable)) {
      problems.add(createDependentCycleMessage(visiting, injectable));
      return;
    }

    visiting.add(injectable);
    for (final BaseDependency dep : injectable.dependencies) {
      final ConcreteInjectable resolved = getResolvedDependency(dep, injectable);
      if (resolved.wiringTypes.contains(WiringElementType.DependentBean) && !visited.contains(resolved)) {
        validateDependentScopedInjectable(resolved, visiting, visited, problems);
      }
    }
    visiting.remove(injectable);
    visited.add(injectable);
  }

  private String createDependentCycleMessage(Set<ConcreteInjectable> visiting, ConcreteInjectable injectable) {
    final StringBuilder builder = new StringBuilder();
    boolean cycleStarted = false;

    builder.append("Dependent scoped cycle found:\n");
    for (final ConcreteInjectable visitingInjectable : visiting) {
      if (visitingInjectable.equals(injectable)) {
        cycleStarted = true;
      }
      if (cycleStarted) {
        builder.append("\t")
               .append(visitingInjectable.type.getFullyQualifiedName())
               .append("\n");
      }
    }

    return builder.toString();
  }

  private void removeUnreachableConcreteInjectables() {
    final Set<String> reachableNames = new HashSet<String>();
    final Queue<ConcreteInjectable> processingQueue = new LinkedList<ConcreteInjectable>();
    for (final ConcreteInjectable injectable : concretesByName.values()) {
      if (!injectable.wiringTypes.contains(WiringElementType.Simpleton) && !reachableNames.contains(injectable.getFactoryName())) {
        processingQueue.add(injectable);
        do {
          final ConcreteInjectable processedInjectable = processingQueue.poll();
          reachableNames.add(processedInjectable.getFactoryName());
          for (final BaseDependency dep : processedInjectable.dependencies) {
            final ConcreteInjectable resolvedDep = getResolvedDependency(dep, processedInjectable);
            if (!reachableNames.contains(resolvedDep.getFactoryName())) {
              processingQueue.add(resolvedDep);
            }
          }
        } while (processingQueue.size() > 0);
      }
    }

    concretesByName.keySet().retainAll(reachableNames);
  }

  private ConcreteInjectable getResolvedDependency(final BaseDependency dep, final ConcreteInjectable processedInjectable) {
    return Validate.notNull(dep.injectable.resolution, "The dependency %s in %s should have already been resolved.", dep, processedInjectable);
  }

  private void resolveDependencies() {
    final Set<ConcreteInjectable> visited = new HashSet<ConcreteInjectable>();
    final Set<String> transientInjectableNames = new HashSet<String>();
    final Map<String, ConcreteInjectable> customProvideds = new HashMap<String, ConcreteInjectable>();

    for (final ConcreteInjectable concrete : concretesByName.values()) {
      if (concrete.isTransient()) {
        transientInjectableNames.add(concrete.getFactoryName());
      }
      if (!visited.contains(concrete)) {
        for (final BaseDependency dep : concrete.dependencies) {
          final ConcreteInjectable resolved = resolveDependency(dep, concrete, customProvideds);
          if (dep.dependencyType.equals(DependencyType.Constructor)) {
            resolved.setRequiresProxyTrue();
          }
        }
      }
    }

    concretesByName.keySet().removeAll(transientInjectableNames);
    concretesByName.putAll(customProvideds);
  }

  private AbstractInjectable copyAbstractInjectable(final AbstractInjectable injectable) {
    final AbstractInjectable retVal = new AbstractInjectable(injectable.type, injectable.qualifier);
    retVal.linked.addAll(injectable.linked);

    return retVal;
  }

  private HasAnnotations getAnnotated(BaseDependency dep) {
    switch (dep.dependencyType) {
    case Field:
      final FieldDependencyImpl fieldDep = (FieldDependencyImpl) dep;
      return fieldDep.field;
    case ProducerParameter:
    case Constructor:
      final ParamDependencyImpl paramDep = (ParamDependencyImpl) dep;
      return paramDep.parameter;
    case SetterParameter:
      final SetterParameterDependencyImpl setterParamDep = (SetterParameterDependencyImpl) dep;
      return setterParamDep.method.getParameters()[0];
    case ProducerMember:
    default:
      throw new RuntimeException("Not yet implemented!");
    }
  }

  private ConcreteInjectable resolveDependency(final BaseDependency dep, final ConcreteInjectable concrete,
          final Map<String, ConcreteInjectable> customProvideds) {
    if (dep.injectable.resolution != null) {
      return dep.injectable.resolution;
    }

    final Multimap<ResolutionPriority, ConcreteInjectable> resolvedByPriority = HashMultimap.create();
    final Queue<AbstractInjectable> resolutionQueue = new LinkedList<AbstractInjectable>();
    resolutionQueue.add(dep.injectable);

    do {
      final AbstractInjectable cur = resolutionQueue.poll();
      for (final BaseInjectable link : cur.linked) {
        if (link instanceof AbstractInjectable) {
          resolutionQueue.add((AbstractInjectable) link);
        } else if (link instanceof ConcreteInjectable) {
          resolvedByPriority.put(getMatchingPriority(link), (ConcreteInjectable) link);
        }
      }
    } while (resolutionQueue.size() > 0);

    // Iterates through priorities from highest to lowest.
    for (final ResolutionPriority priority : ResolutionPriority.values()) {
      if (resolvedByPriority.containsKey(priority)) {
        final Collection<ConcreteInjectable> resolved = resolvedByPriority.get(priority);
        if (resolved.size() > 1) {
          throwAmbiguousDependencyException(dep, concrete, new ArrayList<ConcreteInjectable>(resolved));
        } else {
          ConcreteInjectable injectable = resolved.iterator().next();
          if (injectable.isTransient()) {
            final TransientInjectable providedInjectable = (TransientInjectable) injectable;
            final MetaClass injectedType;
            if (concrete.wiringTypes.contains(WiringElementType.SubTypeMatching)) {
              injectedType = dep.injectable.type;
            } else {
              injectedType = concrete.type;
            }
            final InjectionSite site = new InjectionSite(injectedType, getAnnotated(dep));
            injectable = new ProvidedInjectableImpl(providedInjectable, site);
            customProvideds.put(injectable.getFactoryName(), injectable);
            dep.injectable = copyAbstractInjectable(dep.injectable);
          }
          return (dep.injectable.resolution = injectable);
        }
      }
    }

    throwUnsatisfiedDependencyException(dep, concrete);

    throw new RuntimeException("This line can never be reached but is required for the method to compile.");
  }

  private void throwUnsatisfiedDependencyException(final BaseDependency dep, final ConcreteInjectable concrete) {
    final String message = "Unsatisfied " + dep.dependencyType.toString().toLowerCase() + " dependency " + dep.injectable + " for " + concrete;

    throw new RuntimeException(message);
  }

  private void throwAmbiguousDependencyException(final BaseDependency dep, final ConcreteInjectable concrete, final List<ConcreteInjectable> resolved) {
    final StringBuilder messageBuilder = new StringBuilder();
    messageBuilder.append("Ambiguous resolution for ")
                  .append(dep.dependencyType.toString().toLowerCase())
                  .append(" ")
                  .append(dep.injectable)
                  .append(" in ")
                  .append(concrete)
                  .append(".\n")
                  .append("Resolved types:\n")
                  .append(resolved.get(0));
    for (int i = 1; i < resolved.size(); i++) {
      messageBuilder.append(", ")
                    .append(resolved.get(i));
    }

    throw new RuntimeException(messageBuilder.toString());
  }

  private void linkAbstractInjectables() {
    linkAbstractInjectablsFromDependencies();
    linkSubTypeMatchingAbstractInjectables();
  }

  private void linkAbstractInjectablsFromDependencies() {
    final Set<AbstractInjectable> linked = new HashSet<AbstractInjectable>(abstractInjectables.size());
    for (final ConcreteInjectable concrete : concretesByName.values()) {
      for (final BaseDependency dep : concrete.dependencies) {
        if (!linked.contains(dep.injectable)) {
          linkAbstractInjectable(dep.injectable);
          linked.add(dep.injectable);
        }
      }
    }
  }

  private void linkSubTypeMatchingAbstractInjectables() {
    for (final AbstractInjectable subTypeMatching : subTypeMatchingInjectables) {
      final Collection<AbstractInjectable> candidates = directAbstractInjectablesByAssignableTypes.get(subTypeMatching.type.getErased());
      for (final AbstractInjectable candidate : candidates) {
        if (candidate.qualifier.isSatisfiedBy(subTypeMatching.qualifier)
                && !subTypeMatching.equals(candidate)) {
          candidate.linked.add(subTypeMatching);
        }
      }
    }
  }

  private void linkAbstractInjectable(final AbstractInjectable abstractInjectable) {
    final Collection<AbstractInjectable> candidates = directAbstractInjectablesByAssignableTypes.get(abstractInjectable.type.getErased());
    for (final AbstractInjectable candidate : candidates) {
      if (abstractInjectable.qualifier.isSatisfiedBy(candidate.qualifier)
              && hasAssignableTypeParameters(candidate.getInjectedType(), abstractInjectable.type)
              && !candidate.equals(abstractInjectable)) {
        abstractInjectable.linked.add(candidate);
      }
    }
  }

  private boolean hasAssignableTypeParameters(final MetaClass fromType, final MetaClass toType) {
    final MetaParameterizedType toParamType = toType.getParameterizedType();
    final MetaParameterizedType fromParamType = getFromTypeParams(fromType, toType);

    return toParamType == null || toParamType.isAssignableFrom(fromParamType);
  }

  private MetaParameterizedType getFromTypeParams(final MetaClass fromType, final MetaClass toType) {
    if (toType.isInterface()) {
      if (fromType.getFullyQualifiedName().equals(toType.getFullyQualifiedName())) {
        return fromType.getParameterizedType();
      }
      for (final MetaClass type : fromType.getAllSuperTypesAndInterfaces()) {
        if (type.isInterface() && type.getFullyQualifiedName().equals(toType.getFullyQualifiedName())) {
          return type.getParameterizedType();
        }
      }
      throw new RuntimeException("Could not find interface " + toType.getFullyQualifiedName() + " through type " + fromType.getFullyQualifiedName());
    } else {
      MetaClass clazz = fromType;
      do {
        if (clazz.getFullyQualifiedName().equals(toType.getFullyQualifiedName())) {
          return clazz.getParameterizedType();
        }
        clazz = clazz.getSuperClass();
      } while (!clazz.getFullyQualifiedName().equals("java.lang.Object"));
      throw new RuntimeException("Could not find class " + toType.getFullyQualifiedName() + " through type " + fromType.getFullyQualifiedName());
    }
  }

  @Override
  public void addFieldDependency(final Injectable concreteInjectable, final MetaClass type, final Qualifier qualifier,
          final MetaField dependentField) {
    final Injectable abstractInjectable = lookupAbstractInjectable(type, qualifier);
    final FieldDependency dep = new FieldDependencyImpl(AbstractInjectable.class.cast(abstractInjectable), dependentField);
    addDependency(concreteInjectable, dep);
  }

  @Override
  public void addConstructorDependency(final Injectable concreteInjectable, final MetaClass type,
          final Qualifier qualifier, final int paramIndex, final MetaParameter param) {
    final Injectable abstractInjectable = lookupAbstractInjectable(type, qualifier);
    final int paramIndex1 = paramIndex;
    final MetaParameter param1 = param;
    final ParamDependency dep = new ParamDependencyImpl(AbstractInjectable.class.cast(abstractInjectable), DependencyType.Constructor, paramIndex1, param1);
    addDependency(concreteInjectable, dep);
  }

  @Override
  public void addProducerParamDependency(final Injectable concreteInjectable, final MetaClass type,
          final Qualifier qualifier, final int paramIndex, final MetaParameter param) {
    final Injectable abstractInjectable = lookupAbstractInjectable(type, qualifier);
    final int paramIndex1 = paramIndex;
    final MetaParameter param1 = param;
    final ParamDependency dep = new ParamDependencyImpl(AbstractInjectable.class.cast(abstractInjectable), DependencyType.ProducerParameter, paramIndex1, param1);
    addDependency(concreteInjectable, dep);
  }

  @Override
  public void addProducerMemberDependency(final Injectable concreteInjectable, final MetaClass type,
          final Qualifier qualifier, final MetaClassMember producingMember) {
    final Injectable abstractInjectable = lookupAbstractInjectable(type, qualifier);
    final MetaClassMember member = producingMember;
    final ProducerInstanceDependency dep = new ProducerInstanceDependencyImpl(
            AbstractInjectable.class.cast(abstractInjectable), DependencyType.ProducerMember, member);
    addDependency(concreteInjectable, dep);
  }

  @Override
  public void addSetterMethodDependency(final Injectable concreteInjectable, final MetaClass type,
          final Qualifier qualifier, final MetaMethod setter) {
    final Injectable abstractInjectable = lookupAbstractInjectable(type, qualifier);
    final MetaMethod setter1 = setter;
    final SetterParameterDependency dep = new SetterParameterDependencyImpl(AbstractInjectable.class.cast(abstractInjectable), setter1);
    addDependency(concreteInjectable, dep);
  }

  @Override
  public void addDisposesMethodDependency(final Injectable concreteInjectable, final MetaClass type, final Qualifier qualifier, final MetaMethod disposer) {
    final Injectable abstractInjectable = lookupAbstractInjectable(type, qualifier);
    final DisposerMethodDependency dep = new DisposerMethodDependencyImpl(AbstractInjectable.class.cast(abstractInjectable), disposer);
    addDependency(concreteInjectable, dep);
  }

  @Override
  public void addDisposesParamDependency(final Injectable concreteInjectable, final MetaClass type, final Qualifier qualifier,
          final Integer index, final MetaParameter param) {
    final Injectable abstractInjectable = lookupAbstractInjectable(type, qualifier);
    final ParamDependency dep = new ParamDependencyImpl(AbstractInjectable.class.cast(abstractInjectable), DependencyType.DisposerParameter, index, param);
    addDependency(concreteInjectable, dep);
  }

  class DependencyGraphImpl implements DependencyGraph {

    @SuppressWarnings("unchecked")
    @Override
    public Iterator<Injectable> iterator() {
      return Iterator.class.cast(concretesByName.values().iterator());
    }

    @Override
    public Injectable getConcreteInjectable(final String injectableName) {
      return concretesByName.get(injectableName);
    }

  }

}
