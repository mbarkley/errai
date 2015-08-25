/*
 * Copyright 2012 JBoss, by Red Hat, Inc
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

package org.jboss.errai.ioc.async.test.decorator.rebind;

import java.util.Collections;
import java.util.List;

import org.jboss.errai.codegen.Statement;
import org.jboss.errai.codegen.util.Refs;
import org.jboss.errai.codegen.util.Stmt;
import org.jboss.errai.ioc.async.test.decorator.client.res.LogCall;
import org.jboss.errai.ioc.async.test.decorator.client.res.TestDataCollector;
import org.jboss.errai.ioc.client.api.CodeDecorator;
import org.jboss.errai.ioc.rebind.ioc.extension.IOCDecoratorExtension;
import org.jboss.errai.ioc.rebind.ioc.injector.api.Decorable;
import org.jboss.errai.ioc.rebind.ioc.injector.api.FactoryController;

/**
 * @author Mike Brock
 */

@CodeDecorator
public class LogCallDecorator extends IOCDecoratorExtension<LogCall> {
  public LogCallDecorator(Class<LogCall> decoratesWith) {
    super(decoratesWith);
  }

  @Override
  public List<? extends Statement> generateDecorator(Decorable decorable, FactoryController controller) {
    controller.addInvokeBefore(decorable.getAsMethod(),
        Stmt.invokeStatic(TestDataCollector.class, "beforeInvoke", Refs.get("a0"), Refs.get("a1")));

    controller.addInvokeAfter(decorable.getAsMethod(),
        Stmt.invokeStatic(TestDataCollector.class, "afterInvoke", Refs.get("a0"), Refs.get("a1")));

    final Statement foobar
        = controller.addProxyProperty("foobar", String.class, Stmt.load("foobie!"));

    controller.addInvokeAfter(decorable.getAsMethod(),
        Stmt.invokeStatic(TestDataCollector.class, "property", "foobar", foobar)
    );

    return Collections.emptyList();
  }
}
