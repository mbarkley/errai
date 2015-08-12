package org.jboss.errai.ioc.rebind.ioc.graph;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.ioc.rebind.ioc.injector.api.WiringElementType;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * @author Max Barkley <mbarkley@redhat.com>
 */
public class DependencyGraphBuilderImpl implements DependencyGraphBuilder {

  private final Map<AbstractInjectableHandle, AbstractInjectable> abstractInjectables = new HashMap<AbstractInjectableHandle, AbstractInjectable>();
  private final Multimap<MetaClass, AbstractInjectable> directAbstractInjectablesByAssignableTypes = HashMultimap.create();
  private final Collection<ConcreteInjectable> concretes = new ArrayList<ConcreteInjectable>();

  @Override
  public Injectable addConcreteInjectable(final MetaClass injectedType, final Qualifier qualifier, Class<? extends Annotation> literalScope,
          final InjectorType injectorType, final WiringElementType... wiringTypes) {
    final ConcreteInjectable concrete = new ConcreteInjectable(injectedType, qualifier, literalScope, injectorType, Arrays.asList(wiringTypes));
    concretes.add(concrete);
    linkDirectAbstractInjectable(concrete);

    return concrete;
  }

  private void linkDirectAbstractInjectable(final ConcreteInjectable concreteInjectable) {
    final AbstractInjectable abstractInjectable = lookupAsAbstractInjectable(concreteInjectable.type, concreteInjectable.qualifier);
    abstractInjectable.linked.add(concreteInjectable);
    processAssignableTypes(abstractInjectable);
  }

  private void processAssignableTypes(final AbstractInjectable abstractInjectable) {
    directAbstractInjectablesByAssignableTypes.put(abstractInjectable.type, abstractInjectable);
    processInterfaces(abstractInjectable.type, abstractInjectable);
    if (!abstractInjectable.type.isInterface()) {
      processSuperClasses(abstractInjectable.type, abstractInjectable);
    }
  }

  private void processSuperClasses(final MetaClass type, final AbstractInjectable abstractInjectable) {
    final MetaClass superClass = type.getSuperClass();
    if (superClass != null && !directAbstractInjectablesByAssignableTypes.containsKey(superClass)) {
      directAbstractInjectablesByAssignableTypes.put(superClass, abstractInjectable);
      if (!superClass.getName().equals("java.lang.Object")) {
        processSuperClasses(superClass, abstractInjectable);
      }
    }
  }

  private void processInterfaces(final MetaClass type, final AbstractInjectable abstractInjectable) {
    for (final MetaClass iface : type.getInterfaces()) {
      if (!directAbstractInjectablesByAssignableTypes.containsKey(iface)) {
        directAbstractInjectablesByAssignableTypes.put(iface, abstractInjectable);
        processInterfaces(iface, abstractInjectable);
      }
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
  public void addDependency(final Injectable from, final Injectable to, DependencyType dependencyType) {
    assert (from instanceof ConcreteInjectable);
    assert (to instanceof AbstractInjectable);

    final ConcreteInjectable concrete = (ConcreteInjectable) from;
    final AbstractInjectable abstractInjectable = (AbstractInjectable) to;

    concrete.dependencies.add(new Dependency(abstractInjectable, dependencyType));
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

    for (final ConcreteInjectable concrete : concretes) {
      visiting.push(new DFSFrame(concrete));
      do {
        final DFSFrame curFrame = visiting.peek();
        if (curFrame.dependencyIndex < curFrame.concrete.dependencies.size()) {
          final Dependency dep = curFrame.concrete.dependencies.get(curFrame.dependencyIndex);
          final ConcreteInjectable resolved = resolveDependency(dep);
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

  private ConcreteInjectable resolveDependency(final Dependency dep) {
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
      throw new RuntimeException("Unsatisfied dependency: " + dep.injectable.type.getName());
    } else if (resolved.size() > 1) {
      throwAmbiguousDependencyException(dep, resolved);
    }

    return resolved.get(0);
  }

  private void throwAmbiguousDependencyException(final Dependency dep, final List<ConcreteInjectable> resolved) {
    final StringBuilder messageBuilder = new StringBuilder();
    messageBuilder.append("Ambiguous resolution for type " + dep.injectable.type.getName() + ".\n")
                  .append("Resolved types:\n")
                  .append(resolved.get(0));
    for (int i = 1; i < resolved.size(); i++) {
      messageBuilder.append(", ")
                    .append(resolved.get(i).type.getName());
    }

    throw new RuntimeException(messageBuilder.toString());
  }

  private void linkAbstractInjectables() {
    final Set<AbstractInjectable> linked = new HashSet<AbstractInjectable>(abstractInjectables.size());
    for (final ConcreteInjectable concrete : concretes) {
      for (final Dependency dep : concrete.dependencies) {
        if (!linked.contains(dep.injectable)) {
          linkAbstractInjectable(dep.injectable);
          linked.add(dep.injectable);
        }
      }
    }
  }

  private void linkAbstractInjectable(final AbstractInjectable abstractInjectable) {
    final Collection<AbstractInjectable> candidates = directAbstractInjectablesByAssignableTypes.get(abstractInjectable.type);
    for (final AbstractInjectable candidate : candidates) {
      if (abstractInjectable.qualifier.isSatisfiedBy(candidate.qualifier) && !candidate.equals(abstractInjectable)) {
        abstractInjectable.linked.add(candidate);
      }
    }
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
      return type.getName() + "$" + qualifier.toString();
    }

    @Override
    public Qualifier getQualifier() {
      return qualifier;
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
  }

  static class ConcreteInjectable extends BaseInjectable {
    final InjectorType injectorType;
    final Collection<WiringElementType> wiringTypes;
    final List<Dependency> dependencies = new ArrayList<Dependency>();
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
  }

  static class Dependency {
    final AbstractInjectable injectable;
    final DependencyType dependencyType;

    Dependency(final AbstractInjectable abstractInjectable, final DependencyType dependencyType) {
      this.injectable = abstractInjectable;
      this.dependencyType = dependencyType;
    }

    @Override
    public String toString() {
      return "[depType=" + dependencyType.toString() + ", abstractInjectable=" + injectable.toString() + "]";
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
      return Iterator.class.cast(concretes.iterator());
    }

  }

}
