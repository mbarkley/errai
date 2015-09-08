package org.jboss.errai.ioc.rebind.ioc.graph.impl;

import static org.jboss.errai.ioc.rebind.ioc.graph.impl.ResolutionPriority.getMatchingPriority;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

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
import org.jboss.errai.ioc.rebind.ioc.injector.api.WiringElementType;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;

/**
 * @author Max Barkley <mbarkley@redhat.com>
 */
public class DependencyGraphBuilderImpl implements DependencyGraphBuilder {

  private final Map<InjectableHandle, AbstractInjectable> abstractInjectables = new HashMap<InjectableHandle, AbstractInjectable>();
  private final Multimap<MetaClass, AbstractInjectable> directAbstractInjectablesByAssignableTypes = HashMultimap.create();
  private final Map<String, ConcreteInjectable> concretesByName = new HashMap<String, ConcreteInjectable>();

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

    return concrete;
  }

  @Override
  public Injectable addTransientInjectable(final MetaClass injectedType, final Qualifier qualifier,
          final Class<? extends Annotation> literalScope, final WiringElementType... wiringTypes) {
    final ConcreteInjectable concrete = new TransientInjectable(injectedType, qualifier, literalScope, InjectableType.Transient, Arrays.asList(wiringTypes));
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
    final AbstractInjectable abstractInjectable = lookupAsAbstractInjectable(concreteInjectable.type, concreteInjectable.qualifier);
    abstractInjectable.linked.add(concreteInjectable);
    processAssignableTypes(abstractInjectable);
  }

  private void processAssignableTypes(final AbstractInjectable abstractInjectable) {
    directAbstractInjectablesByAssignableTypes.put(abstractInjectable.type.getErased(), abstractInjectable);
    processInterfaces(abstractInjectable.type, abstractInjectable);
    if (!abstractInjectable.type.isInterface()) {
      processSuperClasses(abstractInjectable.type, abstractInjectable);
    }
  }

  private void processSuperClasses(final MetaClass type, final AbstractInjectable abstractInjectable) {
    final MetaClass superClass = type.getSuperClass();
    if (superClass != null && !directAbstractInjectablesByAssignableTypes.containsKey(superClass.getErased())) {
      directAbstractInjectablesByAssignableTypes.put(superClass.getErased(), abstractInjectable);
      if (!superClass.getName().equals("java.lang.Object")) {
        processSuperClasses(superClass, abstractInjectable);
      }
    }
  }

  private void processInterfaces(final MetaClass type, final AbstractInjectable abstractInjectable) {
    for (final MetaClass iface : type.getInterfaces()) {
      directAbstractInjectablesByAssignableTypes.put(iface.getErased(), abstractInjectable);
      processInterfaces(iface, abstractInjectable);
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
    linkAbstractInjectables();
    resolveDependencies();
    validateDependentScopedInjectables();
    if (removeUnreachable) {
      removeUnreachableConcreteInjectables();
    }

    return new DependencyGraphImpl();
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
          resolveDependency(dep, concrete, customProvideds);
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
      return setterParamDep.method;
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

    final ListMultimap<ResolutionPriority, ConcreteInjectable> resolvedByPriority = ArrayListMultimap.create();
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
        final List<ConcreteInjectable> resolved = resolvedByPriority.get(priority);
        if (resolved.size() > 1) {
          throwAmbiguousDependencyException(dep, concrete, resolved);
        } else {
          ConcreteInjectable injectable = resolved.get(0);
          if (injectable.isTransient()) {
            final TransientInjectable providedInjectable = (TransientInjectable) injectable;
            final InjectionSite site = new InjectionSite(concrete.type, getAnnotated(dep));
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
      for (final MetaClass iface : fromType.getInterfaces()) {
        if (iface.getFullyQualifiedName().equals(toType.getFullyQualifiedName())) {
          return iface.getParameterizedType();
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
  public void addConstructorDependency(Injectable concreteInjectable, MetaClass type, Qualifier qualifier,
          int paramIndex, MetaParameter param) {
    final Injectable abstractInjectable = lookupAbstractInjectable(type, qualifier);
    final int paramIndex1 = paramIndex;
    final MetaParameter param1 = param;
    final ParamDependency dep = new ParamDependencyImpl(AbstractInjectable.class.cast(abstractInjectable), DependencyType.Constructor, paramIndex1, param1);
    addDependency(concreteInjectable, dep);
  }

  @Override
  public void addProducerParamDependency(Injectable concreteInjectable, MetaClass type, Qualifier qualifier,
          int paramIndex, MetaParameter param) {
    final Injectable abstractInjectable = lookupAbstractInjectable(type, qualifier);
    final int paramIndex1 = paramIndex;
    final MetaParameter param1 = param;
    final ParamDependency dep = new ParamDependencyImpl(AbstractInjectable.class.cast(abstractInjectable), DependencyType.ProducerParameter, paramIndex1, param1);
    addDependency(concreteInjectable, dep);
  }

  @Override
  public void addProducerMemberDependency(Injectable concreteInjectable, MetaClass type, Qualifier qualifier,
          MetaClassMember producingMember) {
    final Injectable abstractInjectable = lookupAbstractInjectable(type, qualifier);
    final MetaClassMember member = producingMember;
    final ProducerInstanceDependency dep = new ProducerInstanceDependencyImpl(AbstractInjectable.class.cast(abstractInjectable), DependencyType.ProducerMember, member);
    addDependency(concreteInjectable, dep);
  }

  @Override
  public void addSetterMethodDependency(Injectable concreteInjectable, MetaClass type, Qualifier qualifier,
          MetaMethod setter) {
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
