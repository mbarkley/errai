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

import static org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder.DependencyType.Reachability;

import java.util.Optional;

import javax.enterprise.inject.Instance;

import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.ioc.client.api.BeanDefProvider;
import org.jboss.errai.ioc.client.api.IOCExtension;
import org.jboss.errai.ioc.client.api.ManagedInstance;
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
public class InstanceReachabilityExtension implements IOCExtensionConfigurator {

  @Override
  public void configure(final IOCProcessingContext context, final InjectionContext injectionContext) {
  }

  @Override
  public void afterInitialization(final IOCProcessingContext context, final InjectionContext injectionContext) {
    injectionContext.registerDependencyCallback(new DependencyCallback() {
      @Override
      public boolean test(final MetaClass type, final Qualifier qualifier, final DependencyType kind) {
        return !Reachability.equals(kind) && (type.getFullyQualifiedName().equals(Instance.class.getName())
                || type.getFullyQualifiedName().equals(ManagedInstance.class.getName())
                || type.getFullyQualifiedName().equals(BeanDefProvider.class.getName()));
      }

      @Override
      public void callback(final DependencyGraphBuilder builder, final Injectable injectable, final MetaClass depType,
              final Qualifier depQualifier, final DependencyType type) {
        Optional
          .ofNullable(depType.getParameterizedType())
          .filter(pt -> pt.getTypeParameters().length == 1)
          .map(pt -> pt.getTypeParameters()[0])
          .flatMap(mt -> {
            if (mt instanceof MetaClass) {
              return Optional.<MetaClass>of((MetaClass) mt);
            }
            else {
              return Optional.<MetaClass>empty();
            }
          })
          .ifPresent(mc -> builder.addDynamicallyReachableDependency(injectable, mc, depQualifier));
      }
    });
  }

}
