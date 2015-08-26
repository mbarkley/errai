/**
 * JBoss, Home of Professional Open Source
 * Copyright 2014, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.errai.security.rebind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.errai.codegen.Statement;
import org.jboss.errai.codegen.util.Refs;
import org.jboss.errai.codegen.util.Stmt;
import org.jboss.errai.ioc.client.api.CodeDecorator;
import org.jboss.errai.ioc.client.container.IOC;
import org.jboss.errai.ioc.rebind.ioc.extension.IOCDecoratorExtension;
import org.jboss.errai.ioc.rebind.ioc.injector.api.Decorable;
import org.jboss.errai.ioc.rebind.ioc.injector.api.FactoryController;
import org.jboss.errai.security.client.local.nav.PageRoleLifecycleListener;
import org.jboss.errai.security.client.local.roles.ClientRequiredRolesExtractorImpl;
import org.jboss.errai.security.shared.api.annotation.RestrictedAccess;
import org.jboss.errai.ui.nav.client.local.Page;

/**
 * Register page lifecycle listeners when {@linkplain Page pages} are created.
 *
 * @author Max Barkley <mbarkley@redhat.com>
 */
@CodeDecorator
public class PageSecurityCodeDecorator extends IOCDecoratorExtension<Page> {

  public PageSecurityCodeDecorator(Class<Page> decoratesWith) {
    super(decoratesWith);
  }

  @Override
  public List<? extends Statement> generateDecorator(final Decorable decorable, final FactoryController controller) {
    final List<Statement> statements = new ArrayList<Statement>();

    if (decorable.getEnclosingType().isAnnotationPresent(RestrictedAccess.class)) {
      final RestrictedAccess annotation = decorable.getEnclosingType().getAnnotation(RestrictedAccess.class);
      final String roleListenerVar = "roleListener";

      statements.add(controller.constructSetReference(roleListenerVar,
              Stmt.newObject(PageRoleLifecycleListener.class, annotation,
                      Stmt.newObject(ClientRequiredRolesExtractorImpl.class))));

      Statement roleListenerRef = controller.constructGetReference(roleListenerVar, PageRoleLifecycleListener.class);
      statements.add(Stmt.invokeStatic(IOC.class, "registerLifecycleListener", Refs.get("instance"),
              roleListenerRef));

      controller.addInitializationStatements(statements);

      controller.addDestructionStatements(Collections.<Statement> singletonList(Stmt.invokeStatic(IOC.class,
              "unregisterLifecycleListener", Refs.get("instance"), roleListenerRef)));
    }

    return Collections.emptyList();
  }

}
