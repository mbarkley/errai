/*
 * Copyright (C) 2017 Red Hat, Inc. and/or its affiliates.
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

package org.jboss.errai.ioc.rebind.ioc.extension.builtin;

import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaClassFactory;
import org.jboss.errai.ioc.client.api.ActivatedBy;
import org.jboss.errai.ioc.client.api.IOCExtension;
import org.jboss.errai.ioc.client.container.BeanActivator;
import org.jboss.errai.ioc.rebind.ioc.bootstrapper.IOCProcessingContext;
import org.jboss.errai.ioc.rebind.ioc.extension.IOCExtensionConfigurator;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder.DependencyCallback;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder.DependencyType;
import org.jboss.errai.ioc.rebind.ioc.graph.api.Injectable;
import org.jboss.errai.ioc.rebind.ioc.graph.api.Qualifier;
import org.jboss.errai.ioc.rebind.ioc.injector.api.InjectionContext;

/**
 *
 * @author Max Barkley <mbarkley@redhat.com>
 */
@IOCExtension
public class ActivatorReachabilityExtension implements IOCExtensionConfigurator {

  @Override
  public void configure(final IOCProcessingContext context, final InjectionContext injectionContext) {
  }

  @Override
  public void afterInitialization(final IOCProcessingContext context, final InjectionContext injectionContext) {
    final Qualifier defaultQual = injectionContext.getQualifierFactory().forDefault();
    injectionContext.registerExtensionTypeCallback(candidateType -> {
      if (candidateType.isAnnotationPresent(ActivatedBy.class)) {
        final Class<? extends BeanActivator> activatorClass = candidateType.getAnnotation(ActivatedBy.class).value();
        final MetaClass activatorMC = MetaClassFactory.get(activatorClass);

        final Qualifier candidateQualifier = injectionContext.getQualifierFactory().forSource(candidateType);
        injectionContext.registerDependencyCallback(new DependencyCallback() {

          @Override
          public boolean test(final MetaClass type, final Qualifier qualifier, final DependencyType kind) {
            return type.isAssignableFrom(candidateType) && qualifier.isSatisfiedBy(candidateQualifier);
          }

          @Override
          public void callback(final DependencyGraphBuilder builder, final Injectable injectable,
                  final MetaClass depType, final Qualifier depQualifier, final DependencyType kind) {
            builder.addDynamicallyReachableDependency(injectable, activatorMC, defaultQual);
          }
        });
      }
    });
  }

}
