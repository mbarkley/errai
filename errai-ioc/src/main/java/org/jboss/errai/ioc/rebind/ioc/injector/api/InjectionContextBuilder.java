package org.jboss.errai.ioc.rebind.ioc.injector.api;

import org.jboss.errai.config.rebind.ReachableTypes;
import org.jboss.errai.ioc.rebind.ioc.bootstrapper.spi.IOCProcessingContext;

public interface InjectionContextBuilder {

  InjectionContextBuilder processingContext(IOCProcessingContext processingContext);

  InjectionContextBuilder enabledAlternative(String fqcn);

  InjectionContextBuilder addToWhitelist(String item);

  InjectionContextBuilder addToBlacklist(String item);

  InjectionContextBuilder reachableTypes(ReachableTypes reachableTypes);

  InjectionContextBuilder asyncBootstrap(boolean async);

  InjectionContext build();

}