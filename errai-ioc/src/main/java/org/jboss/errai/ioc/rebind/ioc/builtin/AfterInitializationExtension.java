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

package org.jboss.errai.ioc.rebind.ioc.builtin;

import org.jboss.errai.codegen.Context;
import org.jboss.errai.codegen.Statement;
import org.jboss.errai.codegen.meta.MetaMethod;
import org.jboss.errai.codegen.util.Stmt;
import org.jboss.errai.common.client.api.extension.InitVotes;
import org.jboss.errai.ioc.client.api.AfterInitialization;
import org.jboss.errai.ioc.client.api.CodeDecorator;
import org.jboss.errai.ioc.rebind.ioc.extension.IOCDecoratorExtension;
import org.jboss.errai.ioc.rebind.ioc.injector.api.Decorable;
import org.jboss.errai.ioc.rebind.ioc.injector.api.FactoryController;
import org.jboss.errai.ioc.rebind.ioc.injector.api.InjectableInstance;

import java.util.Collections;
import java.util.List;

/**
 * @author Mike Brock
 */
@SuppressWarnings("UnusedDeclaration")
//@CodeDecorator
public class AfterInitializationExtension extends IOCDecoratorExtension<AfterInitialization> {
  public AfterInitializationExtension(Class<AfterInitialization> decoratesWith) {
    super(decoratesWith);
  }

//  @Override
//  public List<? extends Statement> generateDecorator(InjectableInstance<AfterInitialization> instance) {
//    final Context ctx = instance.getInjectionContext().getProcessingContext().getContext();
//    final MetaMethod method = instance.getMethod();
//
//    if (!method.isPublic()) {
//      instance.ensureMemberExposed();
//    }
//
//    final Statement callbackStmt = Stmt.newObject(Runnable.class).extend()
//            .publicOverridesMethod("run")
//            .append(instance.callOrBind())
//            .finish()
//            .finish();
//
//    return Collections.singletonList(Stmt.create(ctx)
//            .invokeStatic(InitVotes.class, "registerOneTimeInitCallback", callbackStmt));
//  }

  @Override
  public List<?> generateDecorator(Decorable decorable, FactoryController controller) {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
  }
}
