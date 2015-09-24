package org.jboss.errai.ioc.rebind.ioc.graph.impl;

import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder.InjectableType;
import org.jboss.errai.ioc.rebind.ioc.graph.api.ProvidedInjectable;

/**
 * @see ProvidedInjectable
 * @author Max Barkley <mbarkley@redhat.com>
 */
class ProvidedInjectableImpl extends ConcreteInjectable implements ProvidedInjectable {

  final InjectionSite site;
  final ExtensionInjectable injectable;

  ProvidedInjectableImpl(final ExtensionInjectable injectable, final InjectionSite site) {
    super(injectable.type, injectable.qualifier, injectable.literalScope, InjectableType.ExtensionProvided, injectable.wiringTypes);
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

  @Override
  public MetaClass getInjectedType() {
    return site.getExactType();
  }

}