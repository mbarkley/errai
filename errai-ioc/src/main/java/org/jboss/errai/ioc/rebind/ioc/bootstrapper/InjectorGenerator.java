package org.jboss.errai.ioc.rebind.ioc.bootstrapper;

import static org.jboss.errai.codegen.builder.impl.ClassBuilder.define;
import static org.jboss.errai.codegen.meta.MetaClassFactory.parameterizedAs;
import static org.jboss.errai.codegen.meta.MetaClassFactory.typeParametersOf;
import static org.jboss.errai.codegen.util.Stmt.declareFinalVariable;
import static org.jboss.errai.codegen.util.Stmt.newObject;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jboss.errai.codegen.Statement;
import org.jboss.errai.codegen.builder.BlockBuilder;
import org.jboss.errai.codegen.builder.ClassDefinitionStaticOption;
import org.jboss.errai.codegen.builder.ClassStructureBuilder;
import org.jboss.errai.codegen.builder.impl.ClassBuilder;
import org.jboss.errai.codegen.builder.impl.ObjectBuilder;
import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaClassFactory;
import org.jboss.errai.codegen.util.Stmt;
import org.jboss.errai.ioc.client.container.Injector;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraph;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraphBuilder.Dependency;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraphBuilder.DependencyType;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraphBuilder.InjectorType;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraphBuilder.ParamDependency;
import org.jboss.errai.ioc.rebind.ioc.graph.Injectable;

import com.google.common.collect.Multimap;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.IncrementalGenerator;
import com.google.gwt.core.ext.RebindMode;
import com.google.gwt.core.ext.RebindResult;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;

import javassist.compiler.ast.CondExpr;

public class InjectorGenerator extends IncrementalGenerator {

  private static DependencyGraph graph;

  public static void setDependencyGraph(final DependencyGraph graph) {
    InjectorGenerator.graph = graph;
  }

  private static DependencyGraph assertGraphSet() {
    if (graph == null) {
      throw new RuntimeException("Dependency graph must be generated and set before " + InjectorGenerator.class.getSimpleName() + " runs.");
    }

    return graph;
  }

  private static Multimap<DependencyType, Dependency> separateByType(Collection<Dependency> dependencies) {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
  }

  @Override
  public RebindResult generateIncrementally(final TreeLogger logger, final GeneratorContext context, final String typeName)
          throws UnableToCompleteException {
    final DependencyGraph graph = assertGraphSet();
    final Injectable injectable = graph.getConcreteInjectable(typeName);
    final InjectorType injectorType = injectable.getInjectorType();

    final ClassStructureBuilder<?> injectorBuilder = define(getInjectorSubTypeName(typeName)).publicScope()
            .implementsInterface(parameterizedAs(Injector.class, typeParametersOf(injectable.getInjectedType()))).body();
    final InjectorBodyGenerator generator = selectBodyGenerator(injectorType);

    generator.generate(injectorBuilder, injectable, graph, logger, context);

    try {
      context.commit(logger, new PrintWriter(injectorBuilder.toJavaString()));
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }

    return new RebindResult(RebindMode.USE_ALL_NEW_WITH_NO_CACHING, injectorBuilder.getClassDefinition().getFullyQualifiedName());
  }

  private InjectorBodyGenerator selectBodyGenerator(final InjectorType injectorType) {
    final InjectorBodyGenerator generator;
    switch (injectorType) {
    case Type:
      generator = new TypeInjectorBodyGenerator();
      break;
    case ContextualProvider:
    case Producer:
    case Provider:
    default:
      throw new RuntimeException("Not yet implemented!");
    }

    return generator;
  }

  private String getInjectorSubTypeName(String typeName) {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
  }

  @Override
  public long getVersionId() {
    return 1;
  }

  private static interface InjectorBodyGenerator {
    void generate(ClassStructureBuilder<?> bodyBlockBuilder, Injectable injectable, DependencyGraph graph, TreeLogger logger, GeneratorContext context);
  }

  private static class TypeInjectorBodyGenerator implements InjectorBodyGenerator {
    @Override
    public void generate(final ClassStructureBuilder<?> bodyBlockBuilder, final Injectable injectable,
            final DependencyGraph graph, final TreeLogger logger, final GeneratorContext context) {
      final Collection<Dependency> dependencies = injectable.getDependencies();
      final Multimap<DependencyType, Dependency> dependenciesByType = separateByType(dependencies);

      final Collection<Dependency> constructorDependencies = dependenciesByType.get(DependencyType.Constructor);
      final Collection<Dependency> fieldDependencies = dependenciesByType.get(DependencyType.Field);

      final List<Statement> createInstanceStatements = new ArrayList<Statement>();

      if (constructorDependencies.size() > 0) {
        final Object[] constructorParameterStatements = new Object[constructorDependencies.size()];
        for (final Dependency dep : constructorDependencies) {
          final Injectable depInjectable = dep.getInjectable();
          final ParamDependency paramDep = ParamDependency.class.cast(dep);
        }

        createInstanceStatements.add(declareFinalVariable("instance", injectable.getInjectedType(),
                newObject(injectable.getInjectedType(), constructorParameterStatements)));
      }
    }
  }

}
