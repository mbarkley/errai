package org.jboss.errai.ioc.rebind.ioc.graph.impl;

import org.jboss.errai.ioc.rebind.ioc.graph.api.ProvidedInjectable;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder.InjectableType;
import org.jboss.errai.ioc.rebind.ioc.graph.api.ProvidedInjectable.InjectionSite;

class ProvidedInjectableImpl extends ConcreteInjectable implements ProvidedInjectable {

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