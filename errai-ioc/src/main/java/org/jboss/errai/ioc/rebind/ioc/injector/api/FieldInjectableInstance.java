package org.jboss.errai.ioc.rebind.ioc.injector.api;

import static org.jboss.errai.codegen.util.Stmt.loadClassMember;

import java.lang.annotation.Annotation;

import org.jboss.errai.codegen.Statement;
import org.jboss.errai.codegen.builder.ClassStructureBuilder;
import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaField;
import org.jboss.errai.codegen.util.PrivateAccessType;
import org.jboss.errai.codegen.util.PrivateAccessUtil;
import org.jboss.errai.ioc.rebind.ioc.injector.Injector;

public class FieldInjectableInstance<A extends Annotation> extends InjectableInstance<A> {

  private static Statement generateValueStatement(final MetaField field, final TaskType taskType, final ClassStructureBuilder<?> injectorClassBuilder) {
    switch (taskType) {
    case Field:
      return loadClassMember(field.getName());
    case PrivateField:
      PrivateAccessUtil.addPrivateAccessStubs(PrivateAccessType.Both, "jsni", injectorClassBuilder, field);
    default:
      throw new RuntimeException("This statement should only ever be for a field.");
    }
  }

  private final Statement valueStatement;

  public FieldInjectableInstance(final A annotation, final MetaField field, final Injector injector,
          final InjectionContext injectionContext, final ClassStructureBuilder<?> injectorClassBuilder) {
    super(annotation, (field.isPublic() ? TaskType.Field : TaskType.PrivateField), injector, injectionContext, injectorClassBuilder);
    valueStatement = generateValueStatement(field, taskType, injectorClassBuilder);
  }

  @Override
  public Statement getValueStatement() {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
  }

  @Override
  public Injector getTargetInjector() {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
  }

  @Override
  public Statement callOrBind(Statement... values) {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
  }

  @Override
  public MetaClass getElementTypeOrMethodReturnType() {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
  }

  @Override
  public MetaClass getElementType() {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
  }

  @Override
  public void ensureMemberExposed(PrivateAccessType accessType) {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
  }

  @Override
  public String getMemberName() {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
  }

  @Override
  public MetaClass getEnclosingType() {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
  }

  @Override
  public Annotation[] getQualifiers() {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
  }

  @Override
  public Annotation[] getAnnotations() {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
  }

}
