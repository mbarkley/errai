/*
 * Copyright 2011 JBoss, by Red Hat, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.errai.ioc.support.bus.rebind;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

import org.jboss.errai.bus.client.ErraiBus;
import org.jboss.errai.bus.client.api.Local;
import org.jboss.errai.bus.client.api.Subscription;
import org.jboss.errai.bus.server.annotations.Service;
import org.jboss.errai.codegen.Statement;
import org.jboss.errai.codegen.util.Stmt;
import org.jboss.errai.ioc.client.api.CodeDecorator;
import org.jboss.errai.ioc.rebind.ioc.extension.IOCDecoratorExtension;
import org.jboss.errai.ioc.rebind.ioc.injector.InjectUtil;
import org.jboss.errai.ioc.rebind.ioc.injector.api.Decorable;
import org.jboss.errai.ioc.rebind.ioc.injector.api.FactoryController;

@CodeDecorator
public class ServiceCodeDecorator extends IOCDecoratorExtension<Service> {
  public ServiceCodeDecorator(final Class<Service> decoratesWith) {
    super(decoratesWith);
  }

  @Override
  public List<? extends Statement> generateDecorator(final Decorable decorable, final FactoryController controller) {
    Service serviceAnno = (Service) decorable.getAnnotation();
    /**
     * Figure out the service name;
     */
    final String svcName = serviceAnno.value().equals("") ? decorable.getMemberName() : serviceAnno.value();

    boolean local = false;
    for (final Annotation a : InjectUtil.extractQualifiers(decorable.get())) {
      if (Local.class.equals(a.annotationType())) {
        local = true;
      }
    }

    final String varName = decorable.getAsMethod().getName() + "ServiceSub";

    final Statement subscribeStatement;

    if (local) {
      subscribeStatement = Stmt.invokeStatic(ErraiBus.class, "get")
              .invoke("subscribeLocal", svcName, decorable.getAccessStatement());
    }
    else {
      subscribeStatement = Stmt.invokeStatic(ErraiBus.class, "get")
              .invoke("subscribe", svcName, decorable.getAccessStatement());
    }

    controller.addInitializationStatements(Collections.singletonList(controller.constructSetReference(varName, subscribeStatement)));
    controller.addDestructionStatements(Collections.<Statement> singletonList(
            Stmt.nestedCall(controller.constructGetReference(varName, Subscription.class)).invoke("remove")));

    return Collections.emptyList();
  }
}