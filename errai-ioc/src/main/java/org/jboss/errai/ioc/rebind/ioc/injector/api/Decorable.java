package org.jboss.errai.ioc.rebind.ioc.injector.api;

import static org.jboss.errai.codegen.util.PrivateAccessUtil.getPrivateFieldAccessorName;
import static org.jboss.errai.codegen.util.PrivateAccessUtil.getPrivateMethodName;
import static org.jboss.errai.codegen.util.Stmt.loadClassMember;
import static org.jboss.errai.codegen.util.Stmt.loadVariable;
import static org.jboss.errai.ioc.rebind.ioc.bootstrapper.FactoryGenerator.getLocalVariableName;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;

import org.jboss.errai.codegen.Statement;
import org.jboss.errai.codegen.meta.HasAnnotations;
import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaField;
import org.jboss.errai.codegen.meta.MetaMethod;
import org.jboss.errai.codegen.meta.MetaParameter;

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
      Statement getAccessStatement(final HasAnnotations annotated) {
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
      Statement getAccessStatement(final HasAnnotations annotated, final Statement[] statement) {
        final MetaMethod method = (MetaMethod) annotated;
        if (method.isPublic()) {
          return loadVariable("this").invoke(method, (Object[]) statement);
        } else {
          final Object[] params = new Object[statement.length+1];
          for (int i = 0; i < statement.length; i++) {
            params[i+1] = statement[i];
          }
          params[0] = loadVariable("instance");
          return loadVariable("this").invoke(getPrivateMethodName(method), params);
        }
      }

      @Override
      Statement getAccessStatement(final HasAnnotations annotated) {
        return getAccessStatement(annotated, new Statement[0]);
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
      Statement getAccessStatement(final HasAnnotations annotated) {
        final MetaParameter param = (MetaParameter) annotated;
        return loadVariable(getLocalVariableName(param));
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
      Statement getAccessStatement(HasAnnotations annotated) {
        return loadVariable("instance");
      }
    };

    abstract MetaClass getType(HasAnnotations annotated);
    abstract MetaClass getEnclosingType(HasAnnotations annotated);
    Statement getAccessStatement(HasAnnotations annotated, Statement[] params) {
      return getAccessStatement(annotated);
    }
    abstract Statement getAccessStatement(HasAnnotations annotated);

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

  public Decorable(final HasAnnotations annotated, final Annotation annotation, final DecorableType decorableType) {
    this.annotated = annotated;
    this.annotation = annotation;
    this.decorableType = decorableType;
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
    return decorableType().getAccessStatement(annotated, params);
  }

  public HasAnnotations get() {
    return annotated;
  }

  public MetaMethod getAsMethod() {
    return (MetaMethod) annotated;
  }

  DecorableType decorableType() {
    return decorableType;
  }

}
