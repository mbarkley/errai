package org.jboss.errai.ioc.rebind.ioc.extension.builtin;

import java.util.Collections;
import java.util.List;

import org.jboss.errai.codegen.Statement;
import org.jboss.errai.codegen.builder.ClassStructureBuilder;
import org.jboss.errai.codegen.meta.MetaClassFactory;
import org.jboss.errai.codegen.util.Stmt;
import org.jboss.errai.common.client.api.annotations.NamedLogger;
import org.jboss.errai.ioc.client.api.IOCExtension;
import org.jboss.errai.ioc.rebind.ioc.bootstrapper.AbstractBodyGenerator;
import org.jboss.errai.ioc.rebind.ioc.bootstrapper.FactoryBodyGenerator;
import org.jboss.errai.ioc.rebind.ioc.bootstrapper.IOCProcessingContext;
import org.jboss.errai.ioc.rebind.ioc.bootstrapper.IOCProcessor;
import org.jboss.errai.ioc.rebind.ioc.extension.IOCExtensionConfigurator;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraph;
import org.jboss.errai.ioc.rebind.ioc.graph.Injectable;
import org.jboss.errai.ioc.rebind.ioc.graph.InjectableHandle;
import org.jboss.errai.ioc.rebind.ioc.graph.ProvidedInjectable.InjectionSite;
import org.jboss.errai.ioc.rebind.ioc.injector.api.InjectableProvider;
import org.jboss.errai.ioc.rebind.ioc.injector.api.InjectionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@IOCExtension
public class LoggerFactoryIOCExtension implements IOCExtensionConfigurator {

  @Override
  public void configure(IOCProcessingContext context, InjectionContext injectionContext, IOCProcessor procFactory) {
  }

  @Override
  public void afterInitialization(final IOCProcessingContext context, final InjectionContext injectionContext,
          final IOCProcessor procFactory) {
    final InjectableHandle handle = new InjectableHandle(MetaClassFactory.get(Logger.class),
            injectionContext.getQualifierFactory().forUniversallyQualified());
    injectionContext.registerInjectableProvider(handle, new InjectableProvider() {
      @Override
      public FactoryBodyGenerator getGenerator(final InjectionSite injectionSite) {
        final Statement loggerValue;
        if (injectionSite.isAnnotationPresent(NamedLogger.class)) {
          final String loggerName = injectionSite.getAnnotation(NamedLogger.class).value();
          loggerValue = Stmt.invokeStatic(LoggerFactory.class, "getLogger", loggerName);
        }
        else {
          loggerValue = Stmt.invokeStatic(LoggerFactory.class, "getLogger",
                  injectionSite.getEnclosingType().asClass());
        }

        return new AbstractBodyGenerator() {
          @Override
          protected List<Statement> generateCreateInstanceStatements(final ClassStructureBuilder<?> bodyBlockBuilder,
                  final Injectable injectable, final DependencyGraph graph, final InjectionContext injectionContext) {
            return Collections.singletonList(Stmt.nestedCall(loggerValue).returnValue());
          }
        };
      }
    });
  }
}
