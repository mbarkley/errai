package org.jboss.errai.ioc.rebind.ioc.extension.builtin;

import static org.jboss.errai.codegen.util.Stmt.nestedCall;
import static org.jboss.errai.codegen.util.Stmt.newObject;

import java.util.Collections;
import java.util.List;

import org.jboss.errai.codegen.Statement;
import org.jboss.errai.codegen.builder.ClassStructureBuilder;
import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaClassFactory;
import org.jboss.errai.ioc.client.api.IOCExtension;
import org.jboss.errai.ioc.rebind.ioc.bootstrapper.AbstractBodyGenerator;
import org.jboss.errai.ioc.rebind.ioc.bootstrapper.FactoryBodyGenerator;
import org.jboss.errai.ioc.rebind.ioc.bootstrapper.IOCProcessingContext;
import org.jboss.errai.ioc.rebind.ioc.bootstrapper.IOCProcessor;
import org.jboss.errai.ioc.rebind.ioc.extension.IOCExtensionConfigurator;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraph;
import org.jboss.errai.ioc.rebind.ioc.graph.api.Injectable;
import org.jboss.errai.ioc.rebind.ioc.graph.api.ProvidedInjectable.InjectionSite;
import org.jboss.errai.ioc.rebind.ioc.graph.impl.InjectableHandle;
import org.jboss.errai.ioc.rebind.ioc.injector.api.InjectableProvider;
import org.jboss.errai.ioc.rebind.ioc.injector.api.InjectionContext;

import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * Creates injectables for all {@link Widget} subtypes. Without this extension,
 * injecting some widget types (such as {@link TextBox}) would result in an
 * ambigous resolution because of subtypes (such as {@link PasswordTextBox})
 * that are also type injectable.
 *
 * This extension creates a new instance of the exact {@link Widget} subtype for every injection point.
 *
 * @author Max Barkley <mbarkley@redhat.com>
 */
@IOCExtension
public class WidgetIOCExtension implements IOCExtensionConfigurator {

  @Override
  public void configure(IOCProcessingContext context, InjectionContext injectionContext, IOCProcessor procFactory) {
  }

  @Override
  public void afterInitialization(IOCProcessingContext context, InjectionContext injectionContext,
          IOCProcessor procFactory) {
    injectionContext.registerSubTypeMatchingInjectableProvider(
            new InjectableHandle(MetaClassFactory.get(Widget.class), injectionContext.getQualifierFactory().forDefault()), new InjectableProvider() {

              @Override
              public FactoryBodyGenerator getGenerator(final InjectionSite injectionSite) {
                final MetaClass type = injectionSite.getExactType();
                if (!(type.isPublic() && type.isDefaultInstantiable())) {
                  throw new RuntimeException("Cannot generate default instance for type " + type.getFullyQualifiedName());
                }
                return new AbstractBodyGenerator() {
                  @Override
                  protected List<Statement> generateCreateInstanceStatements(ClassStructureBuilder<?> bodyBlockBuilder,
                          Injectable injectable, DependencyGraph graph, InjectionContext injectionContext) {
                    return Collections
                            .<Statement> singletonList(nestedCall(newObject(type, new Object[0])).returnValue());
                  }
                };
              }
            });
  }

}
