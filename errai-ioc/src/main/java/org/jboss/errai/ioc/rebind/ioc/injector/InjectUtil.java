package org.jboss.errai.ioc.rebind.ioc.injector;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Qualifier;

import org.jboss.errai.codegen.Statement;
import org.jboss.errai.codegen.meta.HasAnnotations;
import org.jboss.errai.codegen.meta.MetaField;
import org.jboss.errai.codegen.meta.MetaMethod;
import org.jboss.errai.codegen.util.PrivateAccessType;
import org.jboss.errai.codegen.util.Stmt;
import org.jboss.errai.ioc.rebind.ioc.injector.api.FactoryController;

public class InjectUtil {

  public static Statement invokePublicOrPrivateMethod(final FactoryController controller, final MetaMethod method, final Statement... params) {
    if (method.isPublic()) {
      return Stmt.loadVariable("instance").invoke(method, (Object[]) params);
    } else {
      return controller.addExpsoedMethod(method);
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

  public static Statement getPublicOrPrivateFieldValue(final FactoryController controller, final MetaField field) {
    if (field.isPublic()) {
      return Stmt.loadVariable("instance").loadField(field);
    } else {
      return controller.addExposedField(field, PrivateAccessType.Both);
    }
  }

}
