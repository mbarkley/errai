/**
 * Copyright (C) 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.errai.ui.nav.rebind;

import static java.util.Collections.singleton;
import static org.jboss.errai.codegen.Parameter.finalOf;
import static org.jboss.errai.codegen.util.Stmt.castTo;
import static org.jboss.errai.codegen.util.Stmt.declareFinalVariable;
import static org.jboss.errai.codegen.util.Stmt.invokeStatic;
import static org.jboss.errai.codegen.util.Stmt.loadVariable;
import static org.jboss.errai.codegen.util.Stmt.newObject;
import static org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder.InjectableType.ExtensionProvided;
import static org.jboss.errai.ioc.rebind.ioc.injector.api.WiringElementType.DependentBean;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.Dependent;

import org.jboss.errai.codegen.Statement;
import org.jboss.errai.codegen.builder.ClassStructureBuilder;
import org.jboss.errai.codegen.builder.ContextualStatementBuilder;
import org.jboss.errai.codegen.builder.impl.ObjectBuilder;
import org.jboss.errai.codegen.builder.impl.StatementBuilder;
import org.jboss.errai.codegen.meta.MetaClassFactory;
import org.jboss.errai.common.client.dom.Anchor;
import org.jboss.errai.common.client.dom.Event;
import org.jboss.errai.common.client.dom.EventListener;
import org.jboss.errai.common.client.dom.Window;
import org.jboss.errai.ioc.client.api.IOCExtension;
import org.jboss.errai.ioc.client.container.IOC;
import org.jboss.errai.ioc.rebind.ioc.bootstrapper.AbstractBodyGenerator;
import org.jboss.errai.ioc.rebind.ioc.bootstrapper.FactoryBodyGenerator;
import org.jboss.errai.ioc.rebind.ioc.bootstrapper.IOCProcessingContext;
import org.jboss.errai.ioc.rebind.ioc.extension.IOCExtensionConfigurator;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraph;
import org.jboss.errai.ioc.rebind.ioc.graph.api.Injectable;
import org.jboss.errai.ioc.rebind.ioc.graph.api.InjectionSite;
import org.jboss.errai.ioc.rebind.ioc.graph.api.QualifierFactory;
import org.jboss.errai.ioc.rebind.ioc.graph.impl.DefaultCustomFactoryInjectable;
import org.jboss.errai.ioc.rebind.ioc.graph.impl.FactoryNameGenerator;
import org.jboss.errai.ioc.rebind.ioc.graph.impl.InjectableHandle;
import org.jboss.errai.ioc.rebind.ioc.injector.api.InjectionContext;
import org.jboss.errai.ui.nav.client.local.Navigation;
import org.jboss.errai.ui.nav.client.local.api.TransitionToRole;
import org.jboss.errai.ui.nav.client.local.UniquePageRole;
import org.jboss.errai.ui.nav.client.local.api.TransitionTo;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

/**
 *
 * @author Max Barkley <mbarkley@redhat.com>
 */
@IOCExtension
public class TransitionProviderIOCExtension implements IOCExtensionConfigurator {

  private static final TransitionTo TRANSITION_TO = new TransitionTo() {

      @Override
      public Class<? extends Annotation> annotationType() {
        return TransitionTo.class;
      }

      @Override
      public Class<?> value() {
        return Void.class;
      }
    };

  private static final TransitionToRole TRANSITION_TO_ROLE = new TransitionToRole() {

      @Override
      public Class<? extends Annotation> annotationType() {
        return TransitionToRole.class;
      }

      @Override
      public Class<? extends UniquePageRole> value() {
        return UniquePageRole.class;
      }
    };

  private final Map<Class<?>, DefaultCustomFactoryInjectable> injectables = new HashMap<>();

  @Override
  public void configure(final IOCProcessingContext context, final InjectionContext injectionContext) {
  }

  @Override
  public void afterInitialization(final IOCProcessingContext context, final InjectionContext injectionContext) {
    final QualifierFactory qualifierFactory = injectionContext.getQualifierFactory();
    final InjectableHandle transitionToHandle = new InjectableHandle(MetaClassFactory.get(Anchor.class),
            qualifierFactory.forSource(() -> new Annotation[] { TRANSITION_TO }));
    final InjectableHandle transitionToRoleHandle = new InjectableHandle(MetaClassFactory.get(Anchor.class),
            qualifierFactory.forSource(() -> new Annotation[] { TRANSITION_TO_ROLE }));

    registerProvider(injectionContext, transitionToHandle);
    registerProvider(injectionContext, transitionToRoleHandle);
  }

  private void registerProvider(final InjectionContext injectionContext, final InjectableHandle transitionToHandle) {
    injectionContext.registerExactTypeInjectableProvider(transitionToHandle, (injectionSite, nameGenerator) ->
      getOrCreateInjectable(transitionToHandle, injectionSite, nameGenerator)
    );
  }

  private DefaultCustomFactoryInjectable getOrCreateInjectable(final InjectableHandle handle, final InjectionSite injectionSite,
          final FactoryNameGenerator nameGenerator) {
    final Class<?> targetType = getTargetType(injectionSite);
    DefaultCustomFactoryInjectable injectable = injectables.get(targetType);
    if (injectable == null) {
      injectable = new DefaultCustomFactoryInjectable(handle, nameGenerator.generateFor(handle, ExtensionProvided),
            Dependent.class, singleton(DependentBean), createGenerator(targetType));
      injectables.put(targetType, injectable);
    }
    return injectable;
  }

  private Class<?> getTargetType(final InjectionSite injectionSite) {
    if (injectionSite.isAnnotationPresent(TransitionTo.class)) {
      return injectionSite.getAnnotation(TransitionTo.class).value();
    }
    else if (injectionSite.isAnnotationPresent(TransitionToRole.class)) {
      return injectionSite.getAnnotation(TransitionToRole.class).value();
    }
    else {
      throw new IllegalStateException(
              String.format("This extension provider should only be called for anchors with %s or %s.",
                      TransitionTo.class.getSimpleName(), TransitionToRole.class.getSimpleName()));
    }
  }

  private FactoryBodyGenerator createGenerator(final Class<?> targetType) {
    return new AbstractBodyGenerator() {

      @Override
      protected List<Statement> generateCreateInstanceStatements(final ClassStructureBuilder<?> bodyBlockBuilder,
              final Injectable injectable, final DependencyGraph graph, final InjectionContext injectionContext) {
        final StatementBuilder anchorDeclaration = declareFinalVariable(
                        "anchor",
                        Anchor.class,
                        castTo(Anchor.class, invokeStatic(Window.class, "getDocument")
                          .invoke("createElement", "a")));

        final ObjectBuilder clickListener =
                newObject(EventListener.class)
                  .extend()
                  .publicOverridesMethod("call", finalOf(Event.class, "event"))
                    .append(navigationGoToInvocation(targetType))
                    .finish()
                  .finish();
        final ContextualStatementBuilder setClickListener = loadVariable("anchor").invoke("setOnclick", clickListener);
        final Statement returnValue = loadVariable("anchor").returnValue();

        return Arrays.asList(
                anchorDeclaration,
                setClickListener,
                returnValue
                );
      }

      private ContextualStatementBuilder navigationGoToInvocation(final Class<?> targetType) {
        final String methodName;
        final Object[] args;
        final ContextualStatementBuilder getBeanManager = invokeStatic(IOC.class, "getBeanManager");
        final ContextualStatementBuilder getNavigation = getBeanManager
                  .invoke("lookupBean", Navigation.class)
                  .invoke("getInstance");

        if (UniquePageRole.class.isAssignableFrom(targetType)) {
          methodName = "goToWithRole";
          args = new Object[] { targetType };
        }
        else {
          methodName = "goTo";
          args = new Object[] {
              targetType,
              castTo(Multimap.class,
                      invokeStatic(ImmutableMultimap.class, "of"))
          };
        }
        return castTo(Navigation.class, getNavigation)
                  .invoke(methodName, args);
      }
    };
  }

}
