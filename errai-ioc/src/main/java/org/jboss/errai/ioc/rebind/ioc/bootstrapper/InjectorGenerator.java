package org.jboss.errai.ioc.rebind.ioc.bootstrapper;

import static org.jboss.errai.codegen.builder.impl.ClassBuilder.define;
import static org.jboss.errai.codegen.meta.MetaClassFactory.parameterizedAs;
import static org.jboss.errai.codegen.meta.MetaClassFactory.typeParametersOf;

import java.io.File;
import java.io.PrintWriter;

import org.jboss.errai.codegen.builder.ClassStructureBuilder;
import org.jboss.errai.common.metadata.RebindUtils;
import org.jboss.errai.ioc.client.container.Injector;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraph;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraphBuilder.InjectorType;
import org.jboss.errai.ioc.rebind.ioc.graph.Injectable;

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
    case Provider:
      generator = new ProviderBodyGenerator();
    case ContextualProvider:
    case Producer:
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

}
