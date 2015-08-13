package org.jboss.errai.ioc.rebind.ioc.bootstrapper;

import static org.jboss.errai.codegen.Parameter.finalOf;
import static org.jboss.errai.codegen.builder.impl.ClassBuilder.define;
import static org.jboss.errai.codegen.meta.MetaClassFactory.parameterizedAs;
import static org.jboss.errai.codegen.meta.MetaClassFactory.typeParametersOf;
import static org.jboss.errai.codegen.util.PrivateAccessUtil.addPrivateAccessStubs;
import static org.jboss.errai.codegen.util.PrivateAccessUtil.getPrivateMethodName;
import static org.jboss.errai.codegen.util.Stmt.declareFinalVariable;
import static org.jboss.errai.codegen.util.Stmt.loadLiteral;
import static org.jboss.errai.codegen.util.Stmt.loadVariable;
import static org.jboss.errai.codegen.util.Stmt.newObject;

import java.io.File;
import java.io.PrintWriter;
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
import org.jboss.errai.common.metadata.RebindUtils;
import org.jboss.errai.ioc.client.container.ContextManager;
import org.jboss.errai.ioc.client.container.Injector;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraph;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraphBuilder.Dependency;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraphBuilder.DependencyType;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraphBuilder.FieldDependency;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraphBuilder.InjectorType;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraphBuilder.ParamDependency;
import org.jboss.errai.ioc.rebind.ioc.graph.Injectable;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.IncrementalGenerator;
import com.google.gwt.core.ext.RebindMode;
import com.google.gwt.core.ext.RebindResult;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;

public class InjectorGenerator extends IncrementalGenerator {

  private static final String GENERATED_PACKAGE = "org.jboss.errai.ioc.client";
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

  private static Multimap<DependencyType, Dependency> separateByType(final Collection<Dependency> dependencies) {
    final Multimap<DependencyType, Dependency> separated = HashMultimap.create();

    for (final Dependency dep : dependencies) {
      separated.put(dep.getDependencyType(), dep);
    }

    return separated;
  }

  @Override
  public RebindResult generateIncrementally(final TreeLogger logger, final GeneratorContext context, final String typeName)
          throws UnableToCompleteException {
    final DependencyGraph graph = assertGraphSet();
    final Injectable injectable = graph.getConcreteInjectable(typeName.substring(typeName.lastIndexOf('.')+1));
    final InjectorType injectorType = injectable.getInjectorType();

    final ClassStructureBuilder<?> injectorBuilder = define(getInjectorSubTypeName(typeName)).publicScope()
            .implementsInterface(parameterizedAs(Injector.class, typeParametersOf(injectable.getInjectedType()))).body();
    final InjectorBodyGenerator generator = selectBodyGenerator(injectorType);

    generator.generate(injectorBuilder, injectable, graph, logger, context);

    final String injectorSimpleClassName = getInjectorSubTypeSimpleName(typeName);
    final PrintWriter pw = context.tryCreate(logger, GENERATED_PACKAGE, injectorSimpleClassName);
    final String injectorSource = injectorBuilder.toJavaString();
    final File tmpFile = new File(RebindUtils.getErraiCacheDir().getAbsolutePath() + "/" + injectorSimpleClassName + ".java");
    RebindUtils.writeStringToFile(tmpFile, injectorSource);
    pw.write(injectorSource);
    context.commit(logger, pw);

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

  private String getInjectorSubTypeName(final String typeName) {
    return GENERATED_PACKAGE + "." + getInjectorSubTypeSimpleName(typeName);
  }

  private String getInjectorSubTypeSimpleName(final String typeName) {
    return typeName.replace('.', '_') + "Impl";
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

      createInstance(injectable, constructorDependencies, createInstanceStatements);
      injectFieldDependencies(injectable, fieldDependencies, createInstanceStatements, bodyBlockBuilder);
      maybeInvokePostConstruct(injectable, createInstanceStatements, bodyBlockBuilder);
      addReturnStatement(createInstanceStatements);

      implementCreateInstance(bodyBlockBuilder, injectable, createInstanceStatements);
    }

    private void implementCreateInstance(final ClassStructureBuilder<?> bodyBlockBuilder, final Injectable injectable,
            final List<Statement> createInstanceStatements) {
      bodyBlockBuilder.publicMethod(injectable.getInjectedType(), "createInstance", finalOf(ContextManager.class, "contextManager"))
                      .appendAll(createInstanceStatements)
                      .finish();
    }

    private void addReturnStatement(final List<Statement> createInstanceStatements) {
      createInstanceStatements.add(loadVariable("instance").returnValue());
    }

    private void maybeInvokePostConstruct(final Injectable injectable, final List<Statement> createInstanceStatements,
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
      createInstanceStatements.add(loadVariable("instance").invoke(methodName));
    }

    private String addPrivateMethodAccessor(final MetaMethod postConstruct, final ClassStructureBuilder<?> bodyBlockBuilder) {
      addPrivateAccessStubs("jsni", bodyBlockBuilder, postConstruct);

      return getPrivateMethodName(postConstruct);
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
                .invoke("getInstance", loadLiteral(depInjectable.getInjectorClassSimpleName())));

        if (!field.isPublic()) {
          addPrivateAccessStubs(PrivateAccessType.Write, "jsni", bodyBlockBuilder, field);
          final String privateFieldInjectorName = PrivateAccessUtil.getPrivateFieldInjectorName(field);
          createInstanceStatements.add(loadVariable("instance").invoke(privateFieldInjectorName, injectedValue));
        } else {
          createInstanceStatements.add(loadVariable("instance").loadField(field).assignValue(injectedValue));
        }
      }
    }

    private void createInstance(final Injectable injectable, final Collection<Dependency> constructorDependencies,
            final List<Statement> createInstanceStatements) {
      if (constructorDependencies.size() > 0) {
        final Object[] constructorParameterStatements = new Object[constructorDependencies.size()];
        for (final Dependency dep : constructorDependencies) {
          final Injectable depInjectable = dep.getInjectable();
          final ParamDependency paramDep = ParamDependency.class.cast(dep);
          final String depInjectableName = depInjectable.getInjectorClassSimpleName();

          constructorParameterStatements[paramDep.getParamIndex()] = loadVariable("contextManager").invoke("getInstance", loadLiteral(depInjectableName));
        }

        createInstanceStatements.add(declareFinalVariable("instance", injectable.getInjectedType(),
                newObject(injectable.getInjectedType(), constructorParameterStatements)));
      } else {
        createInstanceStatements.add(declareFinalVariable("instance", injectable.getInjectedType(),
                newObject(injectable.getInjectedType())));
      }
    }
  }

}
