package org.jboss.errai.ioc.rebind.ioc.graph;

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

  private final Map<AliasHandle, Alias> aliases = new HashMap<AliasHandle, Alias>();
  private final Multimap<MetaClass, Alias> directAliasesByAssignableTypes = HashMultimap.create();
  private final Collection<Concrete> concretes = new ArrayList<Concrete>();

  @Override
  public Injector addConcreteInjector(final MetaClass injectedType, final Qualifier qualifier, final InjectorType injectorType,
          final WiringElementType... wiringTypes) {
    final Concrete concrete = new Concrete(injectedType, qualifier, injectorType, Arrays.asList(wiringTypes));
    concretes.add(concrete);
    linkDirectAlias(concrete);

    return concrete;
  }

  private void linkDirectAlias(final Concrete concrete) {
    final Alias alias = lookupAliasAsAlias(concrete.type, concrete.qualifier);
    alias.linked.add(concrete);
    processAssignableTypes(alias);
  }

  private void processAssignableTypes(final Alias alias) {
    directAliasesByAssignableTypes.put(alias.type, alias);
    processInterfaces(alias.type, alias);
    if (!alias.type.isInterface()) {
      processSuperClasses(alias.type, alias);
    }
  }

  private void processSuperClasses(final MetaClass type, final Alias alias) {
    final MetaClass superClass = type.getSuperClass();
    if (superClass != null && !directAliasesByAssignableTypes.containsKey(superClass)) {
      directAliasesByAssignableTypes.put(superClass, alias);
      if (!superClass.getName().equals("java.lang.Object")) {
        processSuperClasses(superClass, alias);
      }
    }
  }

  private void processInterfaces(final MetaClass type, final Alias alias) {
    for (final MetaClass iface : type.getInterfaces()) {
      if (!directAliasesByAssignableTypes.containsKey(iface)) {
        directAliasesByAssignableTypes.put(iface, alias);
        processInterfaces(iface, alias);
      }
    }
  }

  @Override
  public Injector lookupAlias(final MetaClass type, final Qualifier qualifier) {
    return lookupAliasAsAlias(type, qualifier);
  }

  private Alias lookupAliasAsAlias(final MetaClass type, final Qualifier qualifier) {
    final AliasHandle handle = new AliasHandle(type, qualifier);
    Alias alias = aliases.get(handle);
    if (alias == null) {
      alias = new Alias(type, qualifier);
      aliases.put(handle, alias);
    }

    return alias;
  }

  @Override
  public void addDependency(final Injector from, final Injector to, DependencyType dependencyType) {
    assert (from instanceof Concrete);
    assert (to instanceof Alias);

    final Concrete concrete = (Concrete) from;
    final Alias alias = (Alias) to;

    concrete.dependencies.add(new Dependency(alias, dependencyType));
  }

  @Override
  public DependencyGraph createGraph() {
    linkDependencyAliases();
    resolveDependencies();

    return new DependencyGraphImpl();
  }

  private void resolveDependencies() {
    final Set<Concrete> visited = new HashSet<Concrete>();
    final Stack<DFSFrame> visiting = new Stack<DFSFrame>();

    for (final Concrete concrete : concretes) {
      visiting.push(new DFSFrame(concrete));
      do {
        final DFSFrame curFrame = visiting.peek();
        if (curFrame.dependencyIndex < curFrame.concrete.dependencies.size()) {
          final Dependency dep = curFrame.concrete.dependencies.get(curFrame.dependencyIndex);
          final Concrete resolved = resolveDependency(dep);
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

  private void validateCycle(final Stack<DFSFrame> visiting, final Concrete resolved) {
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

  private boolean canBreakCycle(final Concrete resolved) {
    for (final WiringElementType wiringType : resolved.wiringTypes) {
      if (WiringElementType.NormalScopedBean.equals(wiringType)) {
        return true;
      }
    }

    return false;
  }

  private Concrete resolveDependency(final Dependency dep) {
    if (dep.alias.resolution != null) {
      return dep.alias.resolution;
    }

    final List<Concrete> resolved = new ArrayList<Concrete>();
    final Queue<Alias> resolutionQueue = new LinkedList<Alias>();
    resolutionQueue.add(dep.alias);

    do {
      final Alias cur = resolutionQueue.poll();
      for (final Entity link : cur.linked) {
        if (link instanceof Alias) {
          resolutionQueue.add((Alias) link);
        } else if (link instanceof Concrete) {
          resolved.add((Concrete) link);
        }
      }
    } while (resolutionQueue.size() > 0);

    if (resolved.isEmpty()) {
      // TODO improve message
      throw new RuntimeException("Unsatisfied dependency: " + dep.alias.type.getName());
    } else if (resolved.size() > 1) {
      throwAmbiguousDependencyException(dep, resolved);
    }

    return resolved.get(0);
  }

  private void throwAmbiguousDependencyException(final Dependency dep, final List<Concrete> resolved) {
    final StringBuilder messageBuilder = new StringBuilder();
    messageBuilder.append("Ambiguous resolution for type " + dep.alias.type.getName() + ".\n")
                  .append("Resolved types:\n")
                  .append(resolved.get(0));
    for (int i = 1; i < resolved.size(); i++) {
      messageBuilder.append(", ")
                    .append(resolved.get(i).type.getName());
    }

    throw new RuntimeException(messageBuilder.toString());
  }

  private void linkDependencyAliases() {
    final Set<Alias> linked = new HashSet<Alias>(aliases.size());
    for (final Concrete concrete : concretes) {
      for (final Dependency dep : concrete.dependencies) {
        if (!linked.contains(dep.alias)) {
          linkAlias(dep.alias);
          linked.add(dep.alias);
        }
      }
    }
  }

  private void linkAlias(final Alias alias) {
    final Collection<Alias> candidates = directAliasesByAssignableTypes.get(alias.type);
    for (final Alias candidate : candidates) {
      if (alias.qualifier.isSatisfiedBy(candidate.qualifier)) {
        alias.linked.add(candidate);
      }
    }
  }

  static abstract class Entity implements Injector {
    final MetaClass type;
    final Qualifier qualifier;

    Entity(final MetaClass type, final Qualifier qualifier) {
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
 }

  static class Alias extends Entity {
    final Collection<Entity> linked = new ArrayList<Entity>();
    Concrete resolution;

    Alias(final MetaClass type, final Qualifier qualifier) {
      super(type, qualifier);
    }

    @Override
    public String toString() {
      return "[Alias:" + super.toString() + "]";
    }
  }

  static class Concrete extends Entity {
    final InjectorType injectorType;
    final Collection<WiringElementType> wiringTypes;
    final List<Dependency> dependencies = new ArrayList<Dependency>();

    Concrete(final MetaClass type, final Qualifier qualifier, final InjectorType injectorType, final Collection<WiringElementType> wiringTypes) {
      super(type, qualifier);
      this.wiringTypes = wiringTypes;
      this.injectorType = injectorType;
    }

    @Override
    public String toString() {
      return "[Concrete:" + super.toString() + "]";
    }
  }

  static class Dependency {
    final Alias alias;
    final DependencyType dependencyType;

    Dependency(final Alias alias, final DependencyType dependencyType) {
      this.alias = alias;
      this.dependencyType = dependencyType;
    }

    @Override
    public String toString() {
      return "[depType=" + dependencyType.toString() + ", alias=" + alias.toString() + "]";
    }
  }

  static class AliasHandle {
    final MetaClass type;
    final Qualifier qualifier;

    AliasHandle(final MetaClass type, final Qualifier qualifier) {
      this.type = type;
      this.qualifier = qualifier;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof AliasHandle))
        return false;

      final AliasHandle other = (AliasHandle) obj;
      return type.equals(other.type) && qualifier.equals(qualifier);
    }

    @Override
    public int hashCode() {
      return type.hashCode() ^ qualifier.hashCode();
    }
  }

  static class DFSFrame {
    final Concrete concrete;
    int dependencyIndex = 0;

    DFSFrame(final Concrete concrete) {
      this.concrete = concrete;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof Concrete) {
        return concrete.equals(obj);
      } else {
        return super.equals(obj);
      }
    }

    @Override
    public int hashCode() {
      return concrete.hashCode();
    }
  }

  class DependencyGraphImpl implements DependencyGraph {

    @SuppressWarnings("unchecked")
    @Override
    public Iterator<Injector> iterator() {
      return Iterator.class.cast(concretes.iterator());
    }

  }

}
