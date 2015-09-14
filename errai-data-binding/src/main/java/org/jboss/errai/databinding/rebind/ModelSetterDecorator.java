package org.jboss.errai.databinding.rebind;

import org.jboss.errai.codegen.Cast;
import org.jboss.errai.codegen.Statement;
import org.jboss.errai.codegen.exception.GenerationException;
import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.util.Refs;
import org.jboss.errai.codegen.util.Stmt;
import org.jboss.errai.databinding.client.api.DataBinder;
import org.jboss.errai.databinding.client.api.InitialState;
import org.jboss.errai.ioc.client.api.CodeDecorator;
import org.jboss.errai.ioc.rebind.ioc.extension.IOCDecoratorExtension;
import org.jboss.errai.ioc.rebind.ioc.injector.api.Decorable;
import org.jboss.errai.ioc.rebind.ioc.injector.api.FactoryController;
import org.jboss.errai.ui.shared.api.annotations.ModelSetter;

/**
 * Causes the generation of a proxy that overrides a method annotated with {@link ModelSetter}. The
 * overridden method will update the model object managed by a {@link DataBinder} and pass the proxy
 * returned by {@link DataBinder#getModel()} to the actual implementation.
 *
 * @author Mike Brock
 * @author Christian Sadilek <csadilek@redhat.com>
 */
@CodeDecorator
public class ModelSetterDecorator extends IOCDecoratorExtension<ModelSetter> {

  public ModelSetterDecorator(Class<ModelSetter> decoratesWith) {
    super(decoratesWith);
  }

  @Override
  public void generateDecorator(final Decorable decorable, final FactoryController controller) {
    if (decorable.getAsMethod().getParameters() == null || decorable.getAsMethod().getParameters().length != 1)
      throw new GenerationException("@ModelSetter method needs to have exactly one parameter: " + decorable.getAsMethod());

    final MetaClass modelType =
        (MetaClass) controller.getAttribute(DataBindingUtil.BINDER_MODEL_TYPE_VALUE);
    if (!decorable.getAsMethod().getParameters()[0].getType().equals(modelType)) {
      throw new GenerationException("@ModelSetter method parameter must be of type: " + modelType);
    }

    final Statement dataBinder = controller.getReferenceStmt(DataBindingUtil.BINDER_VAR_NAME, DataBinder.class);
    final Statement proxyProperty =
          controller.addProxyProperty("dataBinder", DataBinder.class, dataBinder);

    controller.addInvokeBefore(decorable.getAsMethod(),
          Stmt.nestedCall(proxyProperty)
              .invoke("setModel", Refs.get("a0"), Stmt.loadStatic(InitialState.class, "FROM_MODEL")));

    controller.addInvokeBefore(
          decorable.getAsMethod(),
          Stmt.loadVariable("a0").assignValue(
              Cast.to(decorable.getAsMethod().getParameters()[0].getType(), Stmt.nestedCall(
                  proxyProperty).invoke("getModel"))));
  }
}
