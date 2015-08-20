package org.jboss.errai.ioc.rebind.ioc.bootstrapper;

import static org.jboss.errai.codegen.util.PrivateAccessUtil.addPrivateAccessStubs;
import static org.jboss.errai.codegen.util.Stmt.castTo;
import static org.jboss.errai.codegen.util.Stmt.declareFinalVariable;
import static org.jboss.errai.codegen.util.Stmt.loadLiteral;
import static org.jboss.errai.codegen.util.Stmt.loadVariable;
import static org.jboss.errai.codegen.util.Stmt.newObject;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Queue;

import javax.annotation.PostConstruct;

import org.jboss.errai.codegen.Statement;
import org.jboss.errai.codegen.builder.ClassStructureBuilder;
import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaField;
import org.jboss.errai.codegen.meta.MetaMethod;
import org.jboss.errai.codegen.util.PrivateAccessType;
import org.jboss.errai.codegen.util.PrivateAccessUtil;
import org.jboss.errai.codegen.util.Stmt;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraph;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraphBuilder.Dependency;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraphBuilder.DependencyType;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraphBuilder.FieldDependency;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraphBuilder.ParamDependency;
import org.jboss.errai.ioc.rebind.ioc.graph.Injectable;

import com.google.common.collect.Multimap;

class TypeInjectorBodyGenerator extends AbstractBodyGenerator {

  @Override
  protected List<Statement> generateCreateInstanceStatements(final ClassStructureBuilder<?> bodyBlockBuilder, final Injectable injectable, final DependencyGraph graph) {
    final Multimap<DependencyType, Dependency> dependenciesByType = separateByType(injectable.getDependencies());

    final Collection<Dependency> constructorDependencies = dependenciesByType.get(DependencyType.Constructor);
    final Collection<Dependency> fieldDependencies = dependenciesByType.get(DependencyType.Field);

    final List<Statement> createInstanceStatements = new ArrayList<Statement>();

    constructInstance(injectable, constructorDependencies, createInstanceStatements);
    injectFieldDependencies(injectable, fieldDependencies, createInstanceStatements, bodyBlockBuilder);
    maybeInvokePostConstructs(injectable, createInstanceStatements, bodyBlockBuilder);
    addReturnStatement(createInstanceStatements);
    return createInstanceStatements;
  }

  private void maybeInvokePostConstructs(final Injectable injectable, final List<Statement> createInstanceStatements,
          final ClassStructureBuilder<?> bodyBlockBuilder) {
    final Queue<MetaMethod> postConstructMethods = gatherPostConstructs(injectable);
    for (final MetaMethod postConstruct : postConstructMethods) {
      String methodName = postConstruct.getName();
      if (!postConstruct.isPublic()) {
        methodName = addPrivateMethodAccessor(postConstruct, bodyBlockBuilder);
      }

      addPostConstructInvocation(methodName, createInstanceStatements);
    }
  }

  private void addPostConstructInvocation(final String methodName, final List<Statement> createInstanceStatements) {
    createInstanceStatements.add(loadVariable("this").invoke(methodName, loadVariable("instance")));
  }

  private Queue<MetaMethod> gatherPostConstructs(final Injectable injectable) {
    MetaClass type = injectable.getInjectedType();
    final Deque<MetaMethod> postConstructs = new ArrayDeque<MetaMethod>();

    do {
      final List<MetaMethod> currentPostConstructs = type.getMethodsAnnotatedWith(PostConstruct.class);
      if (currentPostConstructs.size() > 0) {
        if (currentPostConstructs.size() > 1) {
          throw new RuntimeException(type.getFullyQualifiedName() + " has two @PostConstruct methods.");
        }

        final MetaMethod postConstruct = currentPostConstructs.get(0);
        if (postConstruct.getParameters().length > 0) {
          throw new RuntimeException(type.getFullyQualifiedName() + " has a @PostConstruct method with parameters.");
        }

        postConstructs.push(postConstruct);
      }
      type = type.getSuperClass();
    } while (!type.getFullyQualifiedName().equals("java.lang.Object"));

    return postConstructs;
  }

  private void injectFieldDependencies(final Injectable injectable, final Collection<Dependency> fieldDependencies,
          final List<Statement> createInstanceStatements, final ClassStructureBuilder<?> bodyBlockBuilder) {
    for (final Dependency dep : fieldDependencies) {
      final FieldDependency fieldDep = FieldDependency.class.cast(dep);
      final MetaField field = fieldDep.getField();
      final Injectable depInjectable = fieldDep.getInjectable();
      final Object injectedValue = Stmt.castTo(depInjectable.getInjectedType(), loadVariable("contextManager")
              .invoke("getInstance", loadLiteral(depInjectable.getInjectorName())));

      if (!field.isPublic()) {
        addPrivateAccessStubs(PrivateAccessType.Write, "jsni", bodyBlockBuilder, field);
        final String privateFieldInjectorName = PrivateAccessUtil.getPrivateFieldInjectorName(field);
        createInstanceStatements.add(loadVariable("this").invoke(privateFieldInjectorName, loadVariable("instance"), injectedValue));
      } else {
        createInstanceStatements.add(loadVariable("instance").loadField(field).assignValue(injectedValue));
      }
    }
  }

  private void constructInstance(final Injectable injectable, final Collection<Dependency> constructorDependencies,
          final List<Statement> createInstanceStatements) {
    if (constructorDependencies.size() > 0) {
      final Object[] constructorParameterStatements = new Object[constructorDependencies.size()];
      for (final Dependency dep : constructorDependencies) {
        final Injectable depInjectable = dep.getInjectable();
        final ParamDependency paramDep = ParamDependency.class.cast(dep);
        final String depInjectableName = depInjectable.getInjectorName();

        constructorParameterStatements[paramDep.getParamIndex()] = castTo(depInjectable.getInjectedType(),
                loadVariable("contextManager").invoke("getInstance", loadLiteral(depInjectableName)));
      }

      createInstanceStatements.add(declareFinalVariable("instance", injectable.getInjectedType(),
              newObject(injectable.getInjectedType(), constructorParameterStatements)));
    } else {
      createInstanceStatements.add(declareFinalVariable("instance", injectable.getInjectedType(),
              newObject(injectable.getInjectedType())));
    }
  }

}