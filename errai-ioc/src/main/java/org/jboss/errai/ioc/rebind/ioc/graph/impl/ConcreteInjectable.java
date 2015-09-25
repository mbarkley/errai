package org.jboss.errai.ioc.rebind.ioc.graph.impl;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.ioc.client.api.EntryPoint;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder.Dependency;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder.InjectableType;
import org.jboss.errai.ioc.rebind.ioc.graph.api.Qualifier;
import org.jboss.errai.ioc.rebind.ioc.graph.impl.DependencyGraphBuilderImpl.DependencyGraphImpl;
import org.jboss.errai.ioc.rebind.ioc.injector.api.WiringElementType;

/**
 * Concrete here does not mean "for a concrete class". Rather, it means that
 * this injectable was for bean that we know how to produce (either because it
 * is a scoped type or from producer member). In contrast,
 * {@link AbstractInjectable abstract injectables} are used in unresolved
 * dependencies to represent a injectable that we do not yet know how to
 * construct.
 *
 * When the {@link DependencyGraphImpl} is constructed, resolution has occurred
 * and it will contain only concrete injecables.
 *
 * @author Max Barkley <mbarkley@redhat.com>
 */
class ConcreteInjectable extends BaseInjectable {
  final InjectableType injectableType;
  final Collection<WiringElementType> wiringTypes;
  final List<BaseDependency> dependencies = new ArrayList<BaseDependency>();
  final Class<? extends Annotation> literalScope;
  Boolean proxiable = null;
  boolean requiresProxy = false;

  ConcreteInjectable(final MetaClass type, final Qualifier qualifier, final Class<? extends Annotation> literalScope,
          final InjectableType injectorType, final Collection<WiringElementType> wiringTypes) {
    super(type, qualifier);
    this.literalScope = literalScope;
    this.wiringTypes = wiringTypes;
    this.injectableType = injectorType;
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
    return injectableType;
  }

  @Override
  public Collection<Dependency> getDependencies() {
    return Collections.<Dependency>unmodifiableCollection(dependencies);
  }

  @Override
  public boolean requiresProxy() {
    switch (injectableType) {
    case Abstract:
    case ContextualProvider:
    case Provider:
      return false;
    case Producer:
    case Type:
    case JsType:
    case ExtensionProvided:
      return requiresProxy || !(wiringTypes.contains(WiringElementType.DependentBean) || literalScope.equals(EntryPoint.class));
    case Extension:
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
    return InjectableType.ContextualProvider.equals(injectableType);
  }

  @Override
  public void setRequiresProxyTrue() {
    requiresProxy = true;
  }

  @Override
  public boolean isExtension() {
    return false;
  }
}