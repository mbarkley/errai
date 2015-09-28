package org.jboss.errai.ioc.rebind.ioc.graph.impl;

import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.ioc.rebind.ioc.graph.api.Injectable;
import org.jboss.errai.ioc.rebind.ioc.graph.api.Qualifier;

/**
 * Common base class for {@link ConcreteInjectable} and
 * {@link AbstractInjectable} so that they can both be stored as links in
 * abstract injectables.
 *
 * @author Max Barkley <mbarkley@redhat.com>
 */
abstract class BaseInjectable implements Injectable {

  final MetaClass type;
  Qualifier qualifier;
  final String factoryName;

  BaseInjectable(final MetaClass type, final Qualifier qualifier, final String factoryName) {
    this.type = type;
    this.qualifier = qualifier;
    this.factoryName = factoryName;
  }

  @Override
  public String getBeanName() {
    return qualifier.getName();
  }

  @Override
  public MetaClass getInjectedType() {
    return type;
  }

  @Override
  public String toString() {
    return "class=" + getInjectedType() + ", injectorType=" + getInjectableType() + ", qualifier="
            + getQualifier().toString();
  }

  @Override
  public Qualifier getQualifier() {
    return qualifier;
  }

  @Override
  public String getFactoryName() {
    return factoryName;
  }

  @Override
  public InjectableHandle getHandle() {
    return new InjectableHandle(type, qualifier);
  }
}