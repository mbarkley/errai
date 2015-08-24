package org.jboss.errai.ioc.rebind.ioc.extension.builtin;

import org.jboss.errai.ioc.client.api.IOCExtension;
import org.jboss.errai.ioc.rebind.ioc.bootstrapper.IOCProcessingContext;
import org.jboss.errai.ioc.rebind.ioc.bootstrapper.IOCProcessor;
import org.jboss.errai.ioc.rebind.ioc.extension.IOCExtensionConfigurator;
import org.jboss.errai.ioc.rebind.ioc.injector.api.InjectionContext;

@IOCExtension
public class LoggerFactoryIOCExtension implements IOCExtensionConfigurator {

  @Override
  public void configure(IOCProcessingContext context, InjectionContext injectionContext, IOCProcessor procFactory) {
  }

  @Override
  public void afterInitialization(final IOCProcessingContext context, final InjectionContext injectionContext,
          final IOCProcessor procFactory) {
    // FIXME
//    injectionContext.registerInjector(new AbstractInjector() {
//
//      @Override
//      public void renderProvider(InjectableInstance injectableInstance) {
//      }
//
//      @Override
//      public MetaClass getInjectedType() {
//        return MetaClassFactory.get(Logger.class);
//      }
//
//      @Override
//      @SuppressWarnings("unchecked")
//      public Statement getBeanInstance(InjectableInstance injectableInstance) {
//        setCreated(true);
//        setRendered(true);
//
//        final String loggerVarName = InjectUtil.getUniqueVarName();
//        final String loggerName;
//        if (injectableInstance.isAnnotationPresent(NamedLogger.class)) {
//          loggerName = ((NamedLogger) injectableInstance.getAnnotation(NamedLogger.class)).value();
//        }
//        else {
//          loggerName = injectableInstance.getEnclosingType().getFullyQualifiedName();
//        }
//
//        context.append(Stmt.declareFinalVariable(loggerVarName, Logger.class,
//                Stmt.invokeStatic(LoggerFactory.class, "getLogger", loggerName)));
//
//        if (injectionContext.isAsync()) {
//          context.append(Stmt.loadVariable(InjectUtil.getVarNameFromType(MetaClassFactory.get(Logger.class),
//                  injectableInstance)).invoke("callback", Stmt.loadVariable(loggerVarName)));
//
//          return null;
//        }
//        else {
//          return Stmt.loadVariable(loggerVarName);
//        }
//      }
//    });
  }
}
