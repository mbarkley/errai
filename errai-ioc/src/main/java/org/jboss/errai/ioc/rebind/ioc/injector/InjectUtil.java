package org.jboss.errai.ioc.rebind.ioc.injector;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;

import org.jboss.errai.codegen.Statement;
import org.jboss.errai.codegen.VariableReference;
import org.jboss.errai.codegen.builder.VariableReferenceContextualStatementBuilder;
import org.jboss.errai.codegen.meta.HasAnnotations;
import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaField;
import org.jboss.errai.codegen.meta.MetaMethod;
import org.jboss.errai.codegen.meta.MetaParameter;
import org.jboss.errai.ioc.rebind.ioc.injector.api.InjectionContext;

public class InjectUtil {

  public class BeanMetric {

    public Collection<Object> getAllInjectors() {
      // TODO Auto-generated method stub
      throw new RuntimeException("Not yet implemented.");
    }

    public Collection<MetaParameter> getConsolidatedMetaParameters() {
      // TODO Auto-generated method stub
      throw new RuntimeException("Not yet implemented.");
    }

  }

  public static Statement createDestructionCallback(MetaClass type, String initVar,
          List<Statement> destructionStatements) {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
  }

  public static BeanMetric getFilteredBeanMetric(InjectionContext injectionContext, MetaClass injectedType,
          Class<? extends Annotation> class1) {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
  }

  public static Statement getPublicOrPrivateFieldValue(InjectionContext injectionContext,
          VariableReferenceContextualStatementBuilder loadVariable, MetaField field) {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
  }

  public static Statement invokePublicOrPrivateMethod(InjectionContext injectionContext,
          VariableReference variableReference, MetaMethod method) {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
  }

  public static Statement getPublicOrPrivateFieldValue(InjectionContext injectionContext,
          VariableReference variableReference, MetaField field) {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
  }

  public static Statement createInitializationCallback(MetaClass injectedType, String string,
          List<Statement> initStmts) {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
  }

  public static Statement invokePublicOrPrivateMethod(InjectionContext injectionContext,
          VariableReference variableReference, MetaMethod method, Statement elementAccessor) {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
  }

  public static Statement invokePublicOrPrivateMethod(InjectionContext injectionContext, Statement component,
          MetaMethod method, VariableReferenceContextualStatementBuilder loadVariable) {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
  }

  public static List<Annotation> extractQualifiers(HasAnnotations annotated) {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
  }

}
