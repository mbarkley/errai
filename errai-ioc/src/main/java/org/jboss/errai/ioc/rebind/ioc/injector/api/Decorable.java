package org.jboss.errai.ioc.rebind.ioc.injector.api;

import static org.jboss.errai.codegen.util.PrivateAccessUtil.getPrivateFieldAccessorName;
import static org.jboss.errai.codegen.util.PrivateAccessUtil.getPrivateMethodName;
import static org.jboss.errai.codegen.util.Stmt.loadClassMember;
import static org.jboss.errai.codegen.util.Stmt.loadVariable;
import static org.jboss.errai.ioc.rebind.ioc.bootstrapper.FactoryGenerator.getLocalVariableName;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;

import org.jboss.errai.codegen.Context;
import org.jboss.errai.codegen.Statement;
import org.jboss.errai.codegen.meta.HasAnnotations;
import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaClassMember;
import org.jboss.errai.codegen.meta.MetaField;
import org.jboss.errai.codegen.meta.MetaMethod;
import org.jboss.errai.codegen.meta.MetaParameter;
import org.jboss.errai.codegen.meta.impl.build.BuildMetaClass;
import org.jboss.errai.codegen.util.Stmt;

public class Decorable {

  public enum DecorableType {
    FIELD  {
      @Override
      MetaClass getType(final HasAnnotations annotated) {
        return ((MetaField) annotated).getType();
      }

      @Override
      MetaClass getEnclosingType(final HasAnnotations annotated) {
        return ((MetaField) annotated).getDeclaringClass();
      }

      @Override
      Statement getAccessStatement(final HasAnnotations annotated, BuildMetaClass factory) {
        final MetaField field = (MetaField) annotated;
        if (field.isPublic()) {
          return loadClassMember(field.getName());
        } else {
          return loadVariable("this").invoke(getPrivateFieldAccessorName(field), loadVariable("instance"));
        }
      }
    },
    METHOD  {
      @Override
      MetaClass getType(final HasAnnotations annotated) {
        return ((MetaMethod) annotated).getReturnType();
      }

      @Override
      MetaClass getEnclosingType(final HasAnnotations annotated) {
        return ((MetaMethod) annotated).getDeclaringClass();
      }

      @Override
      Statement getAccessStatement(final HasAnnotations annotated, final BuildMetaClass factory, final Statement[] statement) {
        final MetaMethod method = (MetaMethod) annotated;
        if (method.isPublic()) {
          return loadVariable("instance").invoke(method, (Object[]) statement);
        } else {
          final Object[] params = new Object[statement.length+1];
          for (int i = 0; i < statement.length; i++) {
            params[i+1] = statement[i];
          }
          params[0] = loadVariable("instance");
          return Stmt.invokeStatic(factory, getPrivateMethodName(method), params);
        }
      }

      @Override
      Statement getAccessStatement(final HasAnnotations annotated, BuildMetaClass factory) {
        return getAccessStatement(annotated, factory, new Statement[0]);
      }
    },
    PARAM {
      @Override
      MetaClass getType(final HasAnnotations annotated) {
        return ((MetaParameter) annotated).getType();
      }

      @Override
      MetaClass getEnclosingType(final HasAnnotations annotated) {
        return ((MetaParameter) annotated).getDeclaringMember().getDeclaringClass();
      }

      @Override
      Statement getAccessStatement(final HasAnnotations annotated, BuildMetaClass factory) {
        final MetaParameter param = (MetaParameter) annotated;
        return loadVariable(getLocalVariableName(param));
      }

      @Override
      Statement callOrBind(final HasAnnotations annotated, final BuildMetaClass factory, final Statement... params) {
        return METHOD.getAccessStatement(((MetaParameter) annotated).getDeclaringMember(), factory, params);
      }

      @Override
      String getName(HasAnnotations annotated) {
        return ((MetaParameter) annotated).getName();
      }
    },
    TYPE {
      @Override
      MetaClass getType(HasAnnotations annotated) {
        return ((MetaClass) annotated);
      }

      @Override
      MetaClass getEnclosingType(HasAnnotations annotated) {
        return ((MetaClass) annotated);
      }

      @Override
      Statement getAccessStatement(HasAnnotations annotated, BuildMetaClass factory) {
        return loadVariable("instance");
      }

      @Override
      String getName(HasAnnotations annotated) {
        return ((MetaClass) annotated).getName();
      }
    };

    abstract MetaClass getType(HasAnnotations annotated);
    abstract MetaClass getEnclosingType(HasAnnotations annotated);
    Statement getAccessStatement(HasAnnotations annotated, final BuildMetaClass factory, Statement[] params) {
      return getAccessStatement(annotated, factory);
    }
    abstract Statement getAccessStatement(HasAnnotations annotated, BuildMetaClass factory);
    Statement callOrBind(HasAnnotations annotated, final BuildMetaClass factory, Statement... params) {
      return getAccessStatement(annotated, factory, params);
    }
    String getName(HasAnnotations annotated) {
      return ((MetaClassMember) annotated).getName();
    }

    public static DecorableType fromElementType(ElementType elemType) {
      switch (elemType) {
      case FIELD:
        return DecorableType.FIELD;
      case METHOD:
        return DecorableType.METHOD;
      case PARAMETER:
        return DecorableType.PARAM;
      case TYPE:
        return DecorableType.TYPE;
      default:
        throw new RuntimeException("Unsupported element type " + elemType);
      }
    }
  }

  private final HasAnnotations annotated;
  private final Annotation annotation;
  private final DecorableType decorableType;
  private final InjectionContext injectionContext;
  private final Context context;
  private final BuildMetaClass factory;

  public Decorable(final HasAnnotations annotated, final Annotation annotation, final DecorableType decorableType,
          final InjectionContext injectionContext, final Context context, final BuildMetaClass factory) {
    this.annotated = annotated;
    this.annotation = annotation;
    this.decorableType = decorableType;
    this.injectionContext = injectionContext;
    this.context = context;
    this.factory = factory;
  }

  public Annotation getAnnotation() {
    return annotation;
  }

  public MetaClass getEnclosingType() {
    return decorableType().getEnclosingType(annotated);
  }

  public MetaClass getType() {
    return decorableType().getType(annotated);
  }

  public Statement getAccessStatement(Statement... params) {
    return decorableType().getAccessStatement(annotated, factory, params);
  }

  public HasAnnotations get() {
    return annotated;
  }

  public Context getCodegenContext() {
    return context;
  }

  public MetaMethod getAsMethod() {
    return (MetaMethod) annotated;
  }

  public DecorableType decorableType() {
    return decorableType;
  }

  public InjectionContext getInjectionContext() {
    return injectionContext;
  }

  public MetaParameter getAsParameter() {
    return (MetaParameter) annotated;
  }

  public MetaField getAsField() {
    return (MetaField) annotated;
  }

  /*
   * Behaves differently for than getAccessStatement for parameters,
   * where the method is called.
   */
  public Statement callOrBind(final Statement... values) {
    return decorableType().callOrBind(annotated, factory, values);
  }

  public String getName() {
    return decorableType().getName(annotated);
  }

}
