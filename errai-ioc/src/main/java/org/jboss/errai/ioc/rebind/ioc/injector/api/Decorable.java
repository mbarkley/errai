package org.jboss.errai.ioc.rebind.ioc.injector.api;

import static org.apache.commons.lang3.Validate.notNull;
import static org.jboss.errai.codegen.util.PrivateAccessUtil.getPrivateFieldAccessorName;
import static org.jboss.errai.codegen.util.PrivateAccessUtil.getPrivateMethodName;
import static org.jboss.errai.codegen.util.Stmt.invokeStatic;
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
import org.jboss.errai.ioc.rebind.ioc.graph.api.Injectable;
import org.jboss.errai.ioc.util.CDIAnnotationUtils;

public class Decorable {

  public enum DecorableType {
    FIELD  {
      @Override
      public MetaClass getType(final HasAnnotations annotated) {
        return ((MetaField) annotated).getType();
      }

      @Override
      public MetaClass getEnclosingType(final HasAnnotations annotated) {
        return ((MetaField) annotated).getDeclaringClass();
      }

      @Override
      public Statement getAccessStatement(final HasAnnotations annotated, final BuildMetaClass factory) {
        final MetaField field = (MetaField) annotated;
        if (field.isPublic()) {
          return loadClassMember(field.getName());
        } else {
          return invokeStatic(notNull(factory), getPrivateFieldAccessorName(field), loadVariable("instance"));
        }
      }
    },
    METHOD  {
      @Override
      public MetaClass getType(final HasAnnotations annotated) {
        return ((MetaMethod) annotated).getReturnType();
      }

      @Override
      public MetaClass getEnclosingType(final HasAnnotations annotated) {
        return ((MetaMethod) annotated).getDeclaringClass();
      }

      @Override
      public Statement getAccessStatement(final HasAnnotations annotated, final BuildMetaClass factory, final Statement[] statement) {
        final MetaMethod method = (MetaMethod) annotated;
        if (method.isPublic()) {
          return loadVariable("instance").invoke(method, (Object[]) statement);
        } else {
          final Object[] params = new Object[statement.length+1];
          for (int i = 0; i < statement.length; i++) {
            params[i+1] = statement[i];
          }
          params[0] = loadVariable("instance");
          return invokeStatic(notNull(factory), getPrivateMethodName(method), params);
        }
      }

      @Override
      public Statement getAccessStatement(final HasAnnotations annotated, final BuildMetaClass factory) {
        return getAccessStatement(annotated, factory, new Statement[0]);
      }
    },
    PARAM {
      @Override
      public MetaClass getType(final HasAnnotations annotated) {
        return ((MetaParameter) annotated).getType();
      }

      @Override
      public MetaClass getEnclosingType(final HasAnnotations annotated) {
        return ((MetaParameter) annotated).getDeclaringMember().getDeclaringClass();
      }

      @Override
      public Statement getAccessStatement(final HasAnnotations annotated, final BuildMetaClass factory) {
        final MetaParameter param = (MetaParameter) annotated;
        return loadVariable(getLocalVariableName(param));
      }

      @Override
      public Statement call(final HasAnnotations annotated, final BuildMetaClass factory, final Statement... params) {
        return METHOD.getAccessStatement(((MetaParameter) annotated).getDeclaringMember(), factory, params);
      }

      @Override
      public String getName(final HasAnnotations annotated) {
        return ((MetaParameter) annotated).getName();
      }
    },
    TYPE {
      @Override
      public MetaClass getType(final HasAnnotations annotated) {
        return ((MetaClass) annotated);
      }

      @Override
      public MetaClass getEnclosingType(final HasAnnotations annotated) {
        return ((MetaClass) annotated);
      }

      @Override
      public Statement getAccessStatement(final HasAnnotations annotated, final BuildMetaClass factory) {
        return loadVariable("instance");
      }

      @Override
      public String getName(final HasAnnotations annotated) {
        return ((MetaClass) annotated).getName();
      }
    };

    public abstract MetaClass getType(HasAnnotations annotated);
    public abstract MetaClass getEnclosingType(HasAnnotations annotated);
    public Statement getAccessStatement(HasAnnotations annotated, final BuildMetaClass factory, final Statement[] params) {
      return getAccessStatement(annotated, factory);
    }
    public abstract Statement getAccessStatement(final HasAnnotations annotated, final BuildMetaClass factory);
    public Statement call(final HasAnnotations annotated, final BuildMetaClass factory, Statement... params) {
      return getAccessStatement(annotated, factory, params);
    }
    public String getName(HasAnnotations annotated) {
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
  private final Injectable injectable;

  public Decorable(final HasAnnotations annotated, final Annotation annotation, final DecorableType decorableType,
          final InjectionContext injectionContext, final Context context, final BuildMetaClass factory, final Injectable injectable) {
    this.annotated = annotated;
    this.annotation = annotation;
    this.decorableType = decorableType;
    this.injectionContext = injectionContext;
    this.context = context;
    this.factory = factory;
    this.injectable = injectable;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Decorable)) {
      return false;
    }
    final Decorable other = (Decorable) obj;

    return decorableType.equals(other.decorableType) && CDIAnnotationUtils.equals(annotation, other.annotation)
            && annotated.equals(other.annotated) && injectable.equals(other.injectable);
  }

  @Override
  public int hashCode() {
    return decorableType.hashCode() ^ CDIAnnotationUtils.hashCode(annotation) ^ annotated.hashCode() ^ injectable.hashCode();
  }

  public Annotation getAnnotation() {
    return annotation;
  }

  public MetaClass getDecorableDeclaringType() {
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
   * Behaves differently than getAccessStatement for parameters,
   * where the method is called.
   */
  public Statement call(final Statement... values) {
    return decorableType().call(annotated, factory, values);
  }

  public String getName() {
    return decorableType().getName(annotated);
  }

  public Injectable getEnclosingInjectable() {
    return injectable;
  }

  public BuildMetaClass getFactoryMetaClass() {
    return factory;
  }

  public boolean isEnclosingTypeDependent() {
    return injectable.getWiringElementTypes().contains(WiringElementType.DependentBean);
  }

}
