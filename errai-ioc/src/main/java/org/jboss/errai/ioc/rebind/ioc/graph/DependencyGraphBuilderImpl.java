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

import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaField;
import org.jboss.errai.ioc.client.api.EntryPoint;
import org.jboss.errai.ioc.rebind.ioc.injector.api.WiringElementType;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * @author Max Barkley <mbarkley@redhat.com>
 */
public class DependencyGraphBuilderImpl implements DependencyGraphBuilder {

  private final Map<AbstractInjectableHandle, AbstractInjectable> abstractInjectables = new HashMap<AbstractInjectableHandle, AbstractInjectable>();
  private final Multimap<MetaClass, AbstractInjectable> directAbstractInjectablesByAssignableTypes = HashMultimap.create();
  private final Map<String, ConcreteInjectable> concretesByName = new HashMap<String, ConcreteInjectable>();

  @Override
  public Injectable addConcreteInjectable(final MetaClass injectedType, final Qualifier qualifier, Class<? extends Annotation> literalScope,
          final InjectorType injectorType, final WiringElementType... wiringTypes) {
    final ConcreteInjectable concrete = new ConcreteInjectable(injectedType, qualifier, literalScope, injectorType, Arrays.asList(wiringTypes));
    concretesByName.put(concrete.getInjectorClassSimpleName(), concrete);
    linkDirectAbstractInjectable(concrete);

    return concrete;
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

  @Override
  public Injectable lookupAbstractInjectable(final MetaClass type, final Qualifier qualifier) {
    return lookupAsAbstractInjectable(type, qualifier);
  }

  private AbstractInjectable lookupAsAbstractInjectable(final MetaClass type, final Qualifier qualifier) {
    final AbstractInjectableHandle handle = new AbstractInjectableHandle(type, qualifier);
    AbstractInjectable abstractInjectable = abstractInjectables.get(handle);
    if (abstractInjectable == null) {
      abstractInjectable = new AbstractInjectable(type, qualifier);
      abstractInjectables.put(handle, abstractInjectable);
    }

    return abstractInjectable;
  }

  @Override
  public void addDependency(final Injectable concreteInjectable, Dependency dependency) {
    assert (concreteInjectable instanceof ConcreteInjectable);

    final ConcreteInjectable concrete = (ConcreteInjectable) concreteInjectable;

    concrete.dependencies.add(BaseDependency.class.cast(dependency));
  }

  @Override
  public DependencyGraph createGraph() {
    linkAbstractInjectables();
    resolveDependencies();

    return new DependencyGraphImpl();
  }

  private void resolveDependencies() {
    final Set<ConcreteInjectable> visited = new HashSet<ConcreteInjectable>();
    final Stack<DFSFrame> visiting = new Stack<DFSFrame>();

    for (final ConcreteInjectable concrete : concretesByName.values()) {
      visiting.push(new DFSFrame(concrete));
      do {
        final DFSFrame curFrame = visiting.peek();
        if (curFrame.dependencyIndex < curFrame.concrete.dependencies.size()) {
          final BaseDependency dep = curFrame.concrete.dependencies.get(curFrame.dependencyIndex);
          final ConcreteInjectable resolved = resolveDependency(dep, curFrame.concrete);
          if (visited.contains(resolved)) {
            if (visiting.contains(resolved)) {
              validateCycle(visiting, resolved);
            }
          } else {
            curFrame.dependencyIndex += 1;
            visiting.push(new DFSFrame(resolved));
          }
        } else {
          visiting.pop();
        }
      } while (visiting.size() > 0);
    }
  }

  private void validateCycle(final Stack<DFSFrame> visiting, final ConcreteInjectable resolved) {
    if (canBreakCycle(resolved)) {
      return;
    }

    int i;
    for (i = visiting.size() - 1; !visiting.get(i).equals(resolved); i--) {
      if (canBreakCycle(visiting.get(i).concrete)) {
        return;
      }
    }

    throwCycleErrorMessage(visiting, i);
  }

  private void throwCycleErrorMessage(final Stack<DFSFrame> visiting, final int startIndex) {
    final StringBuilder messageBuilder = new StringBuilder();
    messageBuilder.append("The following cycle cannot be wired because it contains no normal scoped beans:\n");
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
      if (WiringElementType.NormalScopedBean.equals(wiringType)) {
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
      // TODO improve message
      throw new RuntimeException("Unsatisfied dependency " + dep.injectable.type.getName() + " in " + concrete.getInjectedType().getName());
    } else if (resolved.size() > 1) {
      final List<ConcreteInjectable> alternatives = getAlternatives(resolved);
      if (alternatives.isEmpty()) {
        throwAmbiguousDependencyException(dep, resolved);
      } else if (alternatives.size() > 1) {
        throwAmbiguousDependencyException(dep, alternatives);
      } else {
        resolved.clear();
        resolved.add(alternatives.get(0));
      }
    }

    return (dep.injectable.resolution = resolved.get(0));
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

  private void throwAmbiguousDependencyException(final BaseDependency dep, final List<ConcreteInjectable> resolved) {
    final StringBuilder messageBuilder = new StringBuilder();
    messageBuilder.append("Ambiguous resolution for type " + dep.injectable.type.getName() + ".\n")
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
              && isAssignable(candidate.getInjectedType(), abstractInjectable.type)
              && !candidate.equals(abstractInjectable)) {
        abstractInjectable.linked.add(candidate);
      }
    }
  }

  private boolean isAssignable(final MetaClass fromType, final MetaClass toType) {
    return toType.isAssignableFrom(fromType);
  }

  @Override
  public FieldDependency createFieldDependency(final Injectable abstractInjectable, final MetaField dependentField) {
    return new FieldDependencyImpl(AbstractInjectable.class.cast(abstractInjectable), DependencyType.Field, dependentField);
  }

  @Override
  public ParamDependency createConstructorDependency(final Injectable abstractInjectable, final int paramIndex) {
    return new ParamDependencyImpl(AbstractInjectable.class.cast(abstractInjectable), DependencyType.Constructor, paramIndex);
  }

  @Override
  public ParamDependency createProducerParamDependency(final Injectable abstractInjectable, final int paramIndex) {
    return new ParamDependencyImpl(AbstractInjectable.class.cast(abstractInjectable), DependencyType.ProducerParameter, paramIndex);
  }

  @Override
  public Dependency createProducerInstanceDependency(final Injectable abstractInjectable) {
    return new BaseDependency(AbstractInjectable.class.cast(abstractInjectable), DependencyType.ProducerInstance);
  }

  static abstract class BaseInjectable implements Injectable {
    final MetaClass type;
    final Qualifier qualifier;

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
      return "class=" + type + ", injectorType=" + getInjectorType() + ", qualifier=" + qualifier.toString();
    }

    @Override
    public Qualifier getQualifier() {
      return qualifier;
    }

    @Override
    public String getInjectorClassSimpleName() {
      return type.getFullyQualifiedName().replace('.', '_') + "_" + qualifier.getIdentifierSafeString();
    }
 }

  static class AbstractInjectable extends BaseInjectable {
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
    public InjectorType getInjectorType() {
      return InjectorType.Abstract;
    }

    @Override
    public Collection<Dependency> getDependencies() {
      return Collections.emptyList();
    }

    @Override
    public boolean requiresProxy() {
      return false;
    }
  }

  static class ConcreteInjectable extends BaseInjectable {
    final InjectorType injectorType;
    final Collection<WiringElementType> wiringTypes;
    final List<BaseDependency> dependencies = new ArrayList<BaseDependency>();
    final Class<? extends Annotation> literalScope;

    ConcreteInjectable(final MetaClass type, final Qualifier qualifier, final Class<? extends Annotation> literalScope,
            final InjectorType injectorType, final Collection<WiringElementType> wiringTypes) {
      super(type, qualifier);
      this.literalScope = literalScope;
      this.wiringTypes = wiringTypes;
      this.injectorType = injectorType;
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
    public InjectorType getInjectorType() {
      return injectorType;
    }

    @Override
    public Collection<Dependency> getDependencies() {
      return Collections.<Dependency>unmodifiableCollection(dependencies);
    }

    @Override
    public boolean requiresProxy() {
      switch (injectorType) {
      case Abstract:
      case ContextualProvider:
      case Provider:
        return false;
      case Producer:
      case Type:
        return !(literalScope.equals(Dependent.class) || literalScope.equals(EntryPoint.class));
      default:
        throw new RuntimeException("Not yet implemented!");
      }
    }
  }

  static class BaseDependency implements Dependency {
    final AbstractInjectable injectable;
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

    ParamDependencyImpl(final AbstractInjectable abstractInjectable, final DependencyType dependencyType, final int paramIndex) {
      super(abstractInjectable, dependencyType);
      this.paramIndex = paramIndex;
    }

    @Override
    public int getParamIndex() {
      return paramIndex;
    }

  }

  static class FieldDependencyImpl extends BaseDependency implements FieldDependency {

    private final MetaField field;

    FieldDependencyImpl(final AbstractInjectable abstractInjectable, final DependencyType dependencyType, final MetaField field) {
      super(abstractInjectable, dependencyType);
      this.field = field;
    }

    @Override
    public MetaField getField() {
      return field;
    }

  }

  static class AbstractInjectableHandle {
    final MetaClass type;
    final Qualifier qualifier;

    AbstractInjectableHandle(final MetaClass type, final Qualifier qualifier) {
      this.type = type;
      this.qualifier = qualifier;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof AbstractInjectableHandle))
        return false;

      final AbstractInjectableHandle other = (AbstractInjectableHandle) obj;
      return type.equals(other.type) && qualifier.equals(qualifier);
    }

    @Override
    public int hashCode() {
      return type.hashCode() ^ qualifier.hashCode();
    }

    @Override
    public String toString() {
      return "[AbstractInjectableHandle:" + type.getName() + "$" + qualifier.toString() + "]";
    }
  }

  static class DFSFrame {
    final ConcreteInjectable concrete;
    int dependencyIndex = 0;

    DFSFrame(final ConcreteInjectable concrete) {
      this.concrete = concrete;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof ConcreteInjectable) {
        return concrete.equals(obj);
      } else {
        return super.equals(obj);
      }
    }

    @Override
    public int hashCode() {
      return concrete.hashCode();
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

}
