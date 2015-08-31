package org.jboss.errai.ioc.rebind.ioc.graph;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import javax.enterprise.context.Dependent;

import org.jboss.errai.codegen.meta.HasAnnotations;
import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaClassMember;
import org.jboss.errai.codegen.meta.MetaField;
import org.jboss.errai.codegen.meta.MetaMethod;
import org.jboss.errai.codegen.meta.MetaParameter;
import org.jboss.errai.codegen.meta.MetaParameterizedType;
import org.jboss.errai.ioc.client.api.EntryPoint;
import org.jboss.errai.ioc.rebind.ioc.graph.ProvidedInjectable.InjectionSite;
import org.jboss.errai.ioc.rebind.ioc.injector.api.WiringElementType;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;

/**
 * @author Max Barkley <mbarkley@redhat.com>
 */
public class DependencyGraphBuilderImpl implements DependencyGraphBuilder {

  private static final String SHORT_NAMES_PROP = "errai.graph_builder.short_factory_names";
  private static final boolean SHORT_NAMES = Boolean.getBoolean(SHORT_NAMES_PROP);

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
    final ConcreteInjectable concrete = new TransientInjectable(injectedType, qualifier, literalScope, InjectableType.Abstract, Arrays.asList(wiringTypes));
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
    if (removeUnreachable) {
      removeUnreachableConcreteInjectables();
    }

    return new DependencyGraphImpl();
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
            final ConcreteInjectable resolvedDep = resolveDependency(dep, processedInjectable);
            if (!reachableNames.contains(resolvedDep.getFactoryName())) {
              processingQueue.add(resolvedDep);
            }
          }
        } while (processingQueue.size() > 0);
      }
    }

    concretesByName.keySet().retainAll(reachableNames);
  }

  private void resolveDependencies() {
    final Set<ConcreteInjectable> visited = new HashSet<ConcreteInjectable>();
    final Stack<DFSFrame> visiting = new Stack<DFSFrame>();
    final Set<String> transientInjectableNames = new HashSet<String>();
    final Map<String, ConcreteInjectable> customProvideds = new HashMap<String, ConcreteInjectable>();

    for (final ConcreteInjectable concrete : concretesByName.values()) {
      if (concrete.isTransient()) {
        transientInjectableNames.add(concrete.getFactoryName());
      }
      if (!visited.contains(concrete)) {
        visiting.push(new DFSFrame(concrete));
      }
      while (visiting.size() > 0) {
        final DFSFrame curFrame = visiting.peek();
        if (curFrame.dependencyIndex < curFrame.concrete.dependencies.size()) {
          final BaseDependency dep = curFrame.concrete.dependencies.get(curFrame.dependencyIndex);
          ConcreteInjectable resolved = resolveDependency(dep, curFrame.concrete);
          if (resolved.isTransient()) {
            final TransientInjectable providedInjectable = (TransientInjectable) resolved;
            final InjectionSite site = new InjectionSite(curFrame.concrete.type, getAnnotated(dep));
            resolved = new ProvidedInjectableImpl(providedInjectable, site);
            customProvideds.put(resolved.getFactoryName(), resolved);
            dep.injectable = copyAbstractInjectable(dep.injectable);
            dep.injectable.resolution = resolved;
          }
          final DFSFrame newFrame = new DFSFrame(resolved, dep);
          if (visiting.contains(newFrame)) {
            validateCycle(visiting, newFrame);
          } else if (!visited.contains(resolved)) {
            visiting.push(newFrame);
          }
          curFrame.dependencyIndex += 1;
        } else {
          visited.add(visiting.pop().concrete);
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

  private void validateCycle(final Stack<DFSFrame> visiting, final DFSFrame newFrame) {
    boolean hasCycleBreaker = canBreakCycle(newFrame.concrete);

    int i;
    for (i = visiting.size() - 1; !visiting.get(i).equals(newFrame); i--) {
      hasCycleBreaker = hasCycleBreaker || canBreakCycle(visiting.get(i).concrete);
      final DFSFrame curFrame = visiting.get(i);
      if (curFrame.dep != null && curFrame.dep.dependencyType.equals(DependencyType.Constructor)) {
        curFrame.concrete.setRequiresProxyTrue();
        if (!(curFrame.concrete.type.isInterface() || curFrame.concrete.type.isDefaultInstantiable())) {
          throwCycleErrorMessage(visiting, visiting.indexOf(newFrame),
                  "the type " + curFrame.concrete.type.getFullyQualifiedName()
                          + " is part of a cycle via constructor injection, but is not proxiable");
        }
      }
    }

    if (!hasCycleBreaker) {
      throwCycleErrorMessage(visiting, i, "it contains no normal scoped, proxiable beans");
    }
  }

  private void throwCycleErrorMessage(final Stack<DFSFrame> visiting, final int startIndex, final String cause) {
    final StringBuilder messageBuilder = new StringBuilder();
    messageBuilder.append("The following cycle cannot be wired because " + cause + ":\n");
    for (int i = startIndex; i < visiting.size(); i++) {
      final MetaClass injectedType = visiting.get(i).concrete.getInjectedType();
      messageBuilder.append(injectedType.getName())
                    .append(" -> ");
    }
    messageBuilder.append(visiting.get(startIndex).concrete.getInjectedType().getName());

    throw new RuntimeException(messageBuilder.toString());
  }

  private boolean canBreakCycle(final ConcreteInjectable resolved) {
    for (final WiringElementType wiringType : resolved.wiringTypes) {
      if (WiringElementType.NormalScopedBean.equals(wiringType) && resolved.getInjectedType().isDefaultInstantiable()) {
        return true;
      }
    }

    return false;
  }

  private ConcreteInjectable resolveDependency(final BaseDependency dep, final ConcreteInjectable concrete) {
    if (dep.injectable.resolution != null) {
      return dep.injectable.resolution;
    }

    final List<ConcreteInjectable> resolved = new ArrayList<ConcreteInjectable>();
    final Queue<AbstractInjectable> resolutionQueue = new LinkedList<AbstractInjectable>();
    resolutionQueue.add(dep.injectable);

    do {
      final AbstractInjectable cur = resolutionQueue.poll();
      for (final BaseInjectable link : cur.linked) {
        if (link instanceof AbstractInjectable) {
          resolutionQueue.add((AbstractInjectable) link);
        } else if (link instanceof ConcreteInjectable) {
          resolved.add((ConcreteInjectable) link);
        }
      }
    } while (resolutionQueue.size() > 0);

    if (resolved.isEmpty()) {
      throwUnsatisfiedDependencyException(dep, concrete);
    } else if (resolved.size() > 1) {
      final List<ConcreteInjectable> providers = getProviders(resolved);
      if (providers.isEmpty()) {
        final List<ConcreteInjectable> alternatives = getAlternatives(resolved);
        if (alternatives.isEmpty()) {
          final List<ConcreteInjectable> nonSimpletons = getNonSimpletons(resolved);
          if (nonSimpletons.isEmpty()) {
            throwAmbiguousDependencyException(dep, concrete, resolved);
          } else if (nonSimpletons.size() > 1) {
            throwAmbiguousDependencyException(dep, concrete, nonSimpletons);
          } else {
            resolved.clear();
            resolved.addAll(nonSimpletons);
          }
        } else if (alternatives.size() > 1) {
          throwAmbiguousDependencyException(dep, concrete, alternatives);
        } else {
          resolved.clear();
          resolved.add(alternatives.get(0));
        }
      } else if (providers.size() > 1) {
        throwAmbiguousDependencyException(dep, concrete, providers);
      } else {
        resolved.clear();
        resolved.add(providers.get(0));
      }
    }

    return (dep.injectable.resolution = resolved.get(0));
  }

  private List<ConcreteInjectable> getNonSimpletons(final List<ConcreteInjectable> resolved) {
    final List<ConcreteInjectable> nonSimpletons = new ArrayList<ConcreteInjectable>();
    for (final ConcreteInjectable injectable : resolved) {
      if (!injectable.wiringTypes.contains(WiringElementType.Simpleton)) {
        nonSimpletons.add(injectable);
      }
    }

    return nonSimpletons;
  }

  private List<ConcreteInjectable> getProviders(final List<ConcreteInjectable> resolved) {
    final List<ConcreteInjectable> providers = new ArrayList<ConcreteInjectable>();
    for (final ConcreteInjectable injectable : resolved) {
      if (InjectableType.Provider.equals(injectable.factoryType) || InjectableType.ContextualProvider.equals(injectable.factoryType)) {
        providers.add(injectable);
      }
    }

    return providers;
  }

  private void throwUnsatisfiedDependencyException(final BaseDependency dep, final ConcreteInjectable concrete) {
    final String message = "Unsatisfied " + dep.dependencyType.toString().toLowerCase() + " dependency " + dep.injectable + " for " + concrete;

    throw new RuntimeException(message);
  }

  private List<ConcreteInjectable> getAlternatives(final List<ConcreteInjectable> resolved) {
    final List<ConcreteInjectable> alternatives = new ArrayList<ConcreteInjectable>();
    for (final ConcreteInjectable injectable : resolved) {
      if (injectable.wiringTypes.contains(WiringElementType.AlternativeBean)) {
        alternatives.add(injectable);
      }
    }

    return alternatives;
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

  private FieldDependency createFieldDependency(final Injectable abstractInjectable, final MetaField dependentField) {
    return new FieldDependencyImpl(AbstractInjectable.class.cast(abstractInjectable), dependentField);
  }

  private SetterParameterDependency createSetterMethodDependency(final Injectable abstractInjectable, final MetaMethod setter) {
    return new SetterParameterDependencyImpl(AbstractInjectable.class.cast(abstractInjectable), setter);
  }

  private ParamDependency createConstructorDependency(final Injectable abstractInjectable, final int paramIndex, final MetaParameter param) {
    return new ParamDependencyImpl(AbstractInjectable.class.cast(abstractInjectable), DependencyType.Constructor, paramIndex, param);
  }

  private ParamDependency createProducerParamDependency(final Injectable abstractInjectable, final int paramIndex, final MetaParameter param) {
    return new ParamDependencyImpl(AbstractInjectable.class.cast(abstractInjectable), DependencyType.ProducerParameter, paramIndex, param);
  }

  private ProducerInstanceDependency createProducerInstanceDependency(final Injectable abstractInjectable, final MetaClassMember member) {
    return new ProducerInstanceDependencyImpl(AbstractInjectable.class.cast(abstractInjectable), DependencyType.ProducerMember, member);
  }

  static abstract class BaseInjectable implements Injectable {
    private static final Multiset<String> allFactoryNames = HashMultiset.create();

    final MetaClass type;
    final Qualifier qualifier;
    String factoryName;

    BaseInjectable(final MetaClass type, final Qualifier qualifier) {
      this.type = type;
      this.qualifier = qualifier;
    }

    @Override
    public MetaClass getInjectedType() {
      return type;
    }

    @Override
    public String toString() {
      return "class=" + type + ", injectorType=" + getInjectableType() + ", qualifier=" + qualifier.toString();
    }

    @Override
    public Qualifier getQualifier() {
      return qualifier;
    }

    @Override
    public String getFactoryName() {
      if (factoryName == null) {
        final String typeName = type.getFullyQualifiedName().replace('.', '_').replace('$', '_');
        final String qualNames = qualifier.getIdentifierSafeString();
        if (SHORT_NAMES) {
          final String rawName = getInjectableType() + "_factory__" + shorten(typeName) + "__quals__" + shorten(qualNames);

          final int collisions = allFactoryNames.count(rawName);
          if (collisions > 0) {
            factoryName = rawName + "_" + String.valueOf(collisions);
          } else {
            factoryName = rawName;
          }
          allFactoryNames.add(rawName);
        } else {
          factoryName = getInjectableType() + "_factory_for__" + typeName + "__with_qualifiers__" + qualNames;
        }
      }

      return factoryName;
    }

    private String shorten(final String compoundName) {
      final String[] names = compoundName.split("__");
      final StringBuilder builder = new StringBuilder();
      for (final String name : names) {
        builder.append(shortenName(name))
               .append('_');
      }
      builder.delete(builder.length()-1, builder.length());

      return builder.toString();
    }

    private String shortenName(final String name) {
      final String[] parts = name.split("_");
      final StringBuilder builder = new StringBuilder();
      boolean haveSeenUpperCase = false;
      for (final String part : parts) {
        if (haveSeenUpperCase || Character.isUpperCase(part.charAt(0))) {
          builder.append(part);
          haveSeenUpperCase = true;
        } else {
          builder.append(part.charAt(0));
        }
        builder.append('_');
      }
      builder.delete(builder.length()-1, builder.length());

      return builder.toString();
    }

    @Override
    public InjectableHandle getHandle() {
      return new InjectableHandle(type, qualifier);
    }
 }

  static class AbstractInjectable extends BaseInjectable {
    // TODO review getDependencies and similar to see if they should throw errors.
    // They should probably only be called on ConcreteInjectables

    final Collection<BaseInjectable> linked = new HashSet<BaseInjectable>();
    ConcreteInjectable resolution;

    AbstractInjectable(final MetaClass type, final Qualifier qualifier) {
      super(type, qualifier);
    }

    @Override
    public String toString() {
      return "[AbstractInjectable:" + super.toString() + "]";
    }

    @Override
    public Class<? extends Annotation> getScope() {
      return null;
    }

    @Override
    public InjectableType getInjectableType() {
      return InjectableType.Abstract;
    }

    @Override
    public Collection<Dependency> getDependencies() {
      if (resolution == null) {
        return Collections.emptyList();
      } else {
        return resolution.getDependencies();
      }
    }

    @Override
    public boolean requiresProxy() {
      if (resolution == null) {
        return false;
      } else {
        return resolution.requiresProxy();
      }
    }

    @Override
    public void setRequiresProxyTrue() {
      throw new RuntimeException("Should not be callled on " + AbstractInjectable.class.getSimpleName());
    }

    @Override
    public Collection<WiringElementType> getWiringElementTypes() {
      return Collections.emptyList();
    }

    @Override
    public boolean isContextual() {
      return resolution != null && resolution.isContextual();
    }

    @Override
    public boolean isTransient() {
      return false;
    }
  }

  static class ConcreteInjectable extends BaseInjectable {
    final InjectableType factoryType;
    final Collection<WiringElementType> wiringTypes;
    final List<BaseDependency> dependencies = new ArrayList<BaseDependency>();
    final Class<? extends Annotation> literalScope;
    boolean requiresProxy = false;

    ConcreteInjectable(final MetaClass type, final Qualifier qualifier, final Class<? extends Annotation> literalScope,
            final InjectableType injectorType, final Collection<WiringElementType> wiringTypes) {
      super(type, qualifier);
      this.literalScope = literalScope;
      this.wiringTypes = wiringTypes;
      this.factoryType = injectorType;
    }

    @Override
    public String toString() {
      return "[Concrete:" + super.toString() + "]";
    }

    @Override
    public Class<? extends Annotation> getScope() {
      return literalScope;
    }

    @Override
    public InjectableType getInjectableType() {
      return factoryType;
    }

    @Override
    public Collection<Dependency> getDependencies() {
      return Collections.<Dependency>unmodifiableCollection(dependencies);
    }

    @Override
    public boolean requiresProxy() {
      switch (factoryType) {
      case Abstract:
      case ContextualProvider:
      case Provider:
        return false;
      case Producer:
      case Type:
        return requiresProxy || !(literalScope.equals(Dependent.class) || literalScope.equals(EntryPoint.class));
      default:
        throw new RuntimeException("Not yet implemented!");
      }
    }

    @Override
    public Collection<WiringElementType> getWiringElementTypes() {
      return Collections.unmodifiableCollection(wiringTypes);
    }

    @Override
    public boolean isContextual() {
      return InjectableType.ContextualProvider.equals(factoryType);
    }

    @Override
    public void setRequiresProxyTrue() {
      requiresProxy = true;
    }

    @Override
    public boolean isTransient() {
      return false;
    }
  }

  static class TransientInjectable extends ConcreteInjectable {

    final Collection<InjectionSite> injectionSites = new ArrayList<InjectionSite>();

    TransientInjectable(final MetaClass type, final Qualifier qualifier,
            final Class<? extends Annotation> literalScope, final InjectableType injectorType,
            final Collection<WiringElementType> wiringTypes) {
      super(type, qualifier, literalScope, injectorType, wiringTypes);
    }

    public Collection<InjectionSite> getInjectionSites() {
      return Collections.unmodifiableCollection(injectionSites);
    }

    public String getFactoryNameForInjectionSite(final InjectionSite site) {
      return getFactoryName() + "__within__" + site.getEnclosingType().getName();
    }

    @Override
    public boolean isTransient() {
      return true;
    }

  }

  static class ProvidedInjectableImpl extends ConcreteInjectable implements ProvidedInjectable {

    final InjectionSite site;
    final TransientInjectable injectable;

    ProvidedInjectableImpl(final TransientInjectable injectable, final InjectionSite site) {
      super(injectable.type, injectable.qualifier, injectable.literalScope, InjectableType.Extension, injectable.wiringTypes);
      this.site = site;
      this.injectable = injectable;
    }

    @Override
    public String getFactoryName() {
      return injectable.getFactoryNameForInjectionSite(site);
    }

    @Override
    public InjectionSite getInjectionSite() {
      return site;
    }

  }

  static class BaseDependency implements Dependency {
    AbstractInjectable injectable;
    final DependencyType dependencyType;

    BaseDependency(final AbstractInjectable abstractInjectable, final DependencyType dependencyType) {
      this.injectable = abstractInjectable;
      this.dependencyType = dependencyType;
    }

    @Override
    public String toString() {
      return "[depType=" + dependencyType.toString() + ", abstractInjectable=" + injectable.toString() + "]";
    }

    @Override
    public Injectable getInjectable() {
      return injectable.resolution;
    }

    @Override
    public DependencyType getDependencyType() {
      return dependencyType;
    }
  }

  static class ParamDependencyImpl extends BaseDependency implements ParamDependency {

    private final int paramIndex;
    private final MetaParameter parameter;

    ParamDependencyImpl(final AbstractInjectable abstractInjectable, final DependencyType dependencyType, final int paramIndex, final MetaParameter parameter) {
      super(abstractInjectable, dependencyType);
      this.paramIndex = paramIndex;
      this.parameter = parameter;
    }

    @Override
    public int getParamIndex() {
      return paramIndex;
    }

    @Override
    public MetaParameter getParameter() {
      return parameter;
    }

  }

  static class FieldDependencyImpl extends BaseDependency implements FieldDependency {

    private final MetaField field;

    FieldDependencyImpl(final AbstractInjectable abstractInjectable, final MetaField field) {
      super(abstractInjectable, DependencyType.Field);
      this.field = field;
    }

    @Override
    public MetaField getField() {
      return field;
    }

  }

  static class SetterParameterDependencyImpl extends BaseDependency implements SetterParameterDependency {

    private final MetaMethod method;

    SetterParameterDependencyImpl(final AbstractInjectable abstractInjectable, final MetaMethod method) {
      super(abstractInjectable, DependencyType.SetterParameter);
      this.method = method;
    }

    @Override
    public MetaMethod getMethod() {
      return method;
    }

  }

  static class ProducerInstanceDependencyImpl extends BaseDependency implements ProducerInstanceDependency {

    private final MetaClassMember producingMember;

    ProducerInstanceDependencyImpl(final AbstractInjectable abstractInjectable, final DependencyType dependencyType, final MetaClassMember producingMember) {
      super(abstractInjectable, dependencyType);
      this.producingMember = producingMember;
    }

    @Override
    public MetaClassMember getProducingMember() {
      return producingMember;
    }

  }

  static class DFSFrame {
    final ConcreteInjectable concrete;
    final BaseDependency dep;
    int dependencyIndex = 0;

    DFSFrame(final ConcreteInjectable concrete) {
      this(concrete, null);
    }

    DFSFrame(final ConcreteInjectable concrete, final BaseDependency dep) {
      this.concrete = concrete;
      this.dep = dep;
    }

    @Override
    public int hashCode() {
      return concrete.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof DFSFrame) {
        return concrete.equals(((DFSFrame) obj).concrete);
      } else {
        return false;
      }
    }

    @Override
    public String toString() {
      return "<concrete=" + concrete.toString() + ", index=" + dependencyIndex + ">";
    }
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

  @Override
  public void addFieldDependency(final Injectable concreteInjectable, final MetaClass type, final Qualifier qualifier,
          final MetaField dependentField) {
    final Injectable abstractInjectable = lookupAbstractInjectable(type, qualifier);
    final FieldDependency dep = createFieldDependency(abstractInjectable, dependentField);
    addDependency(concreteInjectable, dep);
  }

  @Override
  public void addConstructorDependency(Injectable concreteInjectable, MetaClass type, Qualifier qualifier,
          int paramIndex, MetaParameter param) {
    final Injectable abstractInjectable = lookupAbstractInjectable(type, qualifier);
    final ParamDependency dep = createConstructorDependency(abstractInjectable, paramIndex, param);
    addDependency(concreteInjectable, dep);
  }

  @Override
  public void addProducerParamDependency(Injectable concreteInjectable, MetaClass type, Qualifier qualifier,
          int paramIndex, MetaParameter param) {
    final Injectable abstractInjectable = lookupAbstractInjectable(type, qualifier);
    final ParamDependency dep = createProducerParamDependency(abstractInjectable, paramIndex, param);
    addDependency(concreteInjectable, dep);
  }

  @Override
  public void addProducerInstanceDependency(Injectable concreteInjectable, MetaClass type, Qualifier qualifier,
          MetaClassMember producingMember) {
    final Injectable abstractInjectable = lookupAbstractInjectable(type, qualifier);
    final ProducerInstanceDependency dep = createProducerInstanceDependency(abstractInjectable, producingMember);
    addDependency(concreteInjectable, dep);
  }

  @Override
  public void addSetterMethodDependency(Injectable concreteInjectable, MetaClass type, Qualifier qualifier,
          MetaMethod setter) {
    final Injectable abstractInjectable = lookupAbstractInjectable(type, qualifier);
    final SetterParameterDependency dep = createSetterMethodDependency(abstractInjectable, setter);
    addDependency(concreteInjectable, dep);
  }

}
