package org.jboss.errai.ioc.rebind.ioc.injector.api;

import org.jboss.errai.ioc.rebind.ioc.bootstrapper.FactoryBodyGenerator;
import org.jboss.errai.ioc.rebind.ioc.graph.ProvidedInjectable.InjectionSite;

public interface InjectableProvider {

  FactoryBodyGenerator getGenerator(InjectionSite injectionSite);

}
