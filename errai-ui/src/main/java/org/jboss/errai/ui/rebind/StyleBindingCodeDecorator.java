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

package org.jboss.errai.ui.rebind;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jboss.errai.codegen.Parameter;
import org.jboss.errai.codegen.Statement;
import org.jboss.errai.codegen.builder.impl.ObjectBuilder;
import org.jboss.errai.codegen.meta.MetaClassFactory;
import org.jboss.errai.codegen.meta.MetaMethod;
import org.jboss.errai.codegen.meta.MetaParameter;
import org.jboss.errai.codegen.util.EmptyStatement;
import org.jboss.errai.codegen.util.Refs;
import org.jboss.errai.codegen.util.Stmt;
import org.jboss.errai.databinding.rebind.DataBindingUtil;
import org.jboss.errai.ioc.client.api.CodeDecorator;
import org.jboss.errai.ioc.rebind.ioc.extension.IOCDecoratorExtension;
import org.jboss.errai.ioc.rebind.ioc.injector.InjectUtil;
import org.jboss.errai.ioc.rebind.ioc.injector.api.Decorable;
import org.jboss.errai.ioc.rebind.ioc.injector.api.FactoryController;
import org.jboss.errai.ui.shared.api.annotations.style.StyleBinding;
import org.jboss.errai.ui.shared.api.style.StyleBindingChangeHandler;
import org.jboss.errai.ui.shared.api.style.StyleBindingExecutor;
import org.jboss.errai.ui.shared.api.style.StyleBindingsRegistry;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Element;

/**
 * @author Mike Brock
 */
@CodeDecorator
public class StyleBindingCodeDecorator extends IOCDecoratorExtension<StyleBinding> {
  private static final String DATA_BINDING_CONFIG_ATTR = "StyleBinding:DataBinderConfigured";
  private static final String STYLE_BINDING_HOUSEKEEPING_ATTR = "StyleBinding:HousekeepingReg";

  public StyleBindingCodeDecorator(Class<StyleBinding> decoratesWith) {
    super(decoratesWith);
  }

  private static List<? extends Statement> bindHandlingMethod(final Decorable decorable,
          final FactoryController controller, final MetaParameter parameter) {
    final Statement elementAccessor;
    if (MetaClassFactory.get(Element.class).isAssignableFrom(parameter.getType())) {
      elementAccessor = Refs.get("element");
    }
    else if (MetaClassFactory.get(Style.class).isAssignableFrom(parameter.getType())) {
      elementAccessor = Stmt.loadVariable("element").invoke("getStyle");
    }
    else {
      throw new RuntimeException("illegal target type for style binding method: " + parameter.getType() +
          "; expected Element or Style");
    }

    final ObjectBuilder bindExec = Stmt.newObject(StyleBindingExecutor.class)
        .extend()
        .publicOverridesMethod("invokeBinding", Parameter.of(Element.class, "element"))
        .append(decorable.getAccessStatement(elementAccessor))
        .finish()
        .finish();

    final List<Statement> stmts = new ArrayList<Statement>();
    stmts.add(Stmt.invokeStatic(StyleBindingsRegistry.class, "get")
            .invoke("addStyleBinding", Refs.get("instance"),
                decorable.getAnnotation().annotationType(), bindExec));
    addCleanup(decorable, controller, stmts);

    return stmts;
  }

  private static void addCleanup(final Decorable decorable, final FactoryController controller, final List<Statement> stmts) {
    final DataBindingUtil.DataBinderRef dataBinder = DataBindingUtil.lookupDataBinderRef(decorable, controller);

    if (!controller.hasAttribute(STYLE_BINDING_HOUSEKEEPING_ATTR)) {
      final Statement destructionCallback =
          InjectUtil.createDestructionCallback(decorable.getEnclosingType(), "obj",
              Arrays.<Statement> asList(
                  Stmt.invokeStatic(StyleBindingsRegistry.class, "get").invoke("cleanAllForBean",
                      Refs.get("instance")),
                  (dataBinder != null) ? Stmt.nestedCall(dataBinder.getValueAccessor()).invoke(
                      "removePropertyChangeHandler", Stmt.loadVariable("bindingChangeHandler")) : EmptyStatement.INSTANCE)

          );

      stmts.add(Stmt.loadVariable("context").invoke("addDestructionCallback",
          Refs.get("instance"),
          destructionCallback));

      controller.setAttribute(STYLE_BINDING_HOUSEKEEPING_ATTR, Boolean.TRUE);
    }
  }

  @Override
  public List<?> generateDecorator(Decorable decorable, FactoryController controller) {
    final Statement valueAccessor;

    switch (decorable.decorableType()) {
    case METHOD:
      final MetaMethod method = decorable.getAsMethod();
      final MetaParameter[] parameters = method.getParameters();
      if (!method.getReturnType().isVoid() && parameters.length == 0) {
        valueAccessor = decorable.getAccessStatement();
      }
      else if (method.getReturnType().isVoid() && parameters.length == 1) {
        // this method returns void and accepts exactly one parm. assume it's a handler method.
        return bindHandlingMethod(decorable, controller, parameters[0]);
      }
      else {
        throw new RuntimeException("problem with style binding. method is not a valid binding " + method);
      }
      break;

    case FIELD:
      valueAccessor = decorable.getAccessStatement();
      break;

    case TYPE:
      // for api annotations being on a type is allowed.
      if (decorable.getAnnotation().annotationType().getPackage().getName().startsWith("org.jboss.errai")) {
        return new ArrayList<Statement>();
      }
    default:
      throw new RuntimeException("problem with style binding. element target type is invalid: " + decorable.decorableType());
    }

    final List<Statement> stmts = new ArrayList<Statement>();

    final DataBindingUtil.DataBinderRef dataBinder = DataBindingUtil.lookupDataBinderRef(decorable, controller);

    final List<Statement> initStmts = new ArrayList<Statement>();
    if (dataBinder != null) {
      if (!controller.hasAttribute(DATA_BINDING_CONFIG_ATTR)) {
        controller.setAttribute(DATA_BINDING_CONFIG_ATTR, Boolean.TRUE);

        stmts.add(Stmt.declareFinalVariable("bindingChangeHandler", StyleBindingChangeHandler.class,
            Stmt.newObject(StyleBindingChangeHandler.class)));
        // ERRAI-817 deferred initialization
        initStmts.add(Stmt.nestedCall(
            dataBinder.getValueAccessor()).invoke("addPropertyChangeHandler",
            Stmt.loadVariable("bindingChangeHandler"))
            );
      }
    }
    // ERRAI-821 deferred initialization
    initStmts.add(Stmt.invokeStatic(StyleBindingsRegistry.class, "get")
        .invoke("addElementBinding", Refs.get("instance"),
            decorable.getAnnotation(),
            Stmt.nestedCall(valueAccessor).invoke("getElement")));

    stmts.addAll(initStmts);

    addCleanup(decorable, controller, stmts);

    return stmts;
  }
}
