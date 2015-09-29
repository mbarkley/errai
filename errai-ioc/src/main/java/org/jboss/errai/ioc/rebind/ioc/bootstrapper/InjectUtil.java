package org.jboss.errai.ioc.rebind.ioc.bootstrapper;

import static org.jboss.errai.codegen.util.Stmt.loadVariable;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Qualifier;

import org.jboss.errai.codegen.Statement;
import org.jboss.errai.codegen.builder.ContextualStatementBuilder;
import org.jboss.errai.codegen.meta.HasAnnotations;
import org.jboss.errai.codegen.meta.MetaField;
import org.jboss.errai.codegen.meta.MetaMethod;
import org.jboss.errai.codegen.util.Stmt;
import org.jboss.errai.ioc.client.container.Factory;
import org.jboss.errai.ioc.rebind.ioc.injector.api.FactoryController;

/**
 * Some useful methods for generating code to inject dependencies in {@link Factory factories}.
 *
 * @author Max Barkley <mbarkley@redhat.com>
 */
public class InjectUtil {

  public static Statement invokePublicOrPrivateMethod(final FactoryController controller, final MetaMethod method, final Statement... params) {
    if (method.isPublic()) {
      return loadVariable("instance").invoke(method, (Object[]) params);
    } else {
      return controller.exposedMethodStmt(method, params);
    }
  }

  public static List<Annotation> extractQualifiers(final HasAnnotations annotated) {
    final List<Annotation> qualifiers = new ArrayList<Annotation>();
    for (final Annotation anno : annotated.getAnnotations()) {
      if (anno.annotationType().isAnnotationPresent(Qualifier.class)) {
        qualifiers.add(anno);
      }
    }

    return qualifiers;
  }

  public static ContextualStatementBuilder getPublicOrPrivateFieldValue(final FactoryController controller, final MetaField field) {
    if (field.isPublic()) {
      return loadVariable("instance").loadField(field);
    } else {
      return controller.exposedFieldStmt(field);
    }
  }

  /**
   * Generates code to call
   * {@link Factory#getReferenceAs(Object, String, Class)} for an instance in
   * {@link Factory#createInstance(org.jboss.errai.ioc.client.container.ContextManager)}
   * and
   * {@link Factory#destroyInstance(Object, org.jboss.errai.ioc.client.container.ContextManager)}
   * methods.
   */
  public static ContextualStatementBuilder constructGetReference(final String name, final Class<?> refType) {
    return loadVariable("thisInstance").invoke("getReferenceAs", loadVariable("instance"), name, refType);
  }

  /**
   * Generates code to call {@link Factory#setReference(Object, String, Object)}
   * for an instance in the
   * {@link Factory#createInstance(org.jboss.errai.ioc.client.container.ContextManager)}
   * method.
   */
  public static ContextualStatementBuilder constructSetReference(final String name, final Statement value) {
    return loadVariable("thisInstance").invoke("setReference", Stmt.loadVariable("instance"), name, value);
  }

}