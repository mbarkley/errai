package org.jboss.errai.ioc.rebind.ioc.graph.impl;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.ioc.rebind.ioc.graph.api.Qualifier;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder.Dependency;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder.InjectableType;
import org.jboss.errai.ioc.rebind.ioc.injector.api.WiringElementType;

class AbstractInjectable extends BaseInjectable {
  // TODO needs to be renamed and not be an Injectable
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