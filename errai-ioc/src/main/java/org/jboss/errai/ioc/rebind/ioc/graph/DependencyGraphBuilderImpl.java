package org.jboss.errai.ioc.rebind.ioc.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

  static class Entity implements Injector {
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
 }

  static class Alias extends Entity {
    final Collection<Entity> linked = new ArrayList<Entity>();

    Alias(final MetaClass type, final Qualifier qualifier) {
      super(type, qualifier);
    }
  }

  static class Concrete extends Entity {
    final InjectorType injectorType;
    final Collection<WiringElementType> wiringTypes;
    final Collection<Dependency> dependencies = new ArrayList<Dependency>();

    Concrete(final MetaClass type, final Qualifier qualifier, final InjectorType injectorType, final Collection<WiringElementType> wiringTypes) {
      super(type, qualifier);
      this.wiringTypes = wiringTypes;
      this.injectorType = injectorType;
    }
  }

  static class Dependency {
    final Alias alias;
    final DependencyType dependencyType;

    Dependency(final Alias alias, final DependencyType dependencyType) {
      this.alias = alias;
      this.dependencyType = dependencyType;
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

}
