package org.jboss.errai.ioc.rebind.ioc.injector.api;

import org.jboss.errai.ioc.client.container.Factory;
import org.jboss.errai.ioc.rebind.ioc.bootstrapper.FactoryBodyGenerator;
import org.jboss.errai.ioc.rebind.ioc.extension.IOCExtensionConfigurator;
import org.jboss.errai.ioc.rebind.ioc.graph.api.ProvidedInjectable.InjectionSite;

/**
 * This allows {@link IOCExtensionConfigurator IOC extensions} to generate
 * custom {@link Factory factories} per injection site using
 * {@link InjectionContext#registerInjectableProvider(org.jboss.errai.ioc.rebind.ioc.graph.impl.InjectableHandle, InjectableProvider)}
 * and
 * {@link InjectionContext#registerSubTypeMatchingInjectableProvider(org.jboss.errai.ioc.rebind.ioc.graph.impl.InjectableHandle, InjectableProvider)}
 * .
 *
 * @author Max Barkley <mbarkley@redhat.com>
 */
public interface InjectableProvider {

  /**
   * @param injectionSite Metadata for an injection site.
   * @return A {@link FactoryBodyGenerator} for the given injeciton site.
   */
  FactoryBodyGenerator getGenerator(InjectionSite injectionSite);

}
