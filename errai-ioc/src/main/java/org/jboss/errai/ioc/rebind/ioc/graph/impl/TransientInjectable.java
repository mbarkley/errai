package org.jboss.errai.ioc.rebind.ioc.graph.impl;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.ioc.rebind.ioc.graph.api.Qualifier;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder.InjectableType;
import org.jboss.errai.ioc.rebind.ioc.graph.api.ProvidedInjectable.InjectionSite;
import org.jboss.errai.ioc.rebind.ioc.injector.api.WiringElementType;

class TransientInjectable extends ConcreteInjectable {

  final Collection<InjectionSite> injectionSites = new ArrayList<InjectionSite>();

  TransientInjectable(final MetaClass type, final Qualifier qualifier,
          final Class<? extends Annotation> literalScope, final InjectableType injectorType,
          final Collection<WiringElementType> wiringTypes) {
    super(type, qualifier, literalScope, injectorType, wiringTypes);
  }

  public Collection<InjectionSite> getInjectionSites() {
    return Collections.unmodifiableCollection(injectionSites);
  }

  public String getFactoryNameForInjectionSite(final InjectionSite site) {
    return getFactoryName() + "__within__" + site.getEnclosingType().getName();
  }

  @Override
  public boolean isTransient() {
    return true;
  }

}