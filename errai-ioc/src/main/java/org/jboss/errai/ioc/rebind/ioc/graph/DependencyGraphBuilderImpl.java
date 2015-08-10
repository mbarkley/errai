package org.jboss.errai.ioc.rebind.ioc.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.ioc.rebind.ioc.injector.api.WiringElementType;

/**
 * @author Max Barkley <mbarkley@redhat.com>
 */
public class DependencyGraphBuilderImpl implements DependencyGraphBuilder {

  private final Map<AliasHandle, Alias> aliases = new HashMap<AliasHandle, Alias>();
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
    alias.concretes.add(concrete);
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
  public DependencyGraph resolveDependencies() {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
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
    final Collection<Concrete> concretes = new ArrayList<Concrete>();

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
