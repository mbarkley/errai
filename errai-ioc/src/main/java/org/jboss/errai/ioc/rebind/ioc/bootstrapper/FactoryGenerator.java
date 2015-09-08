package org.jboss.errai.ioc.rebind.ioc.bootstrapper;

import static org.jboss.errai.codegen.builder.impl.ClassBuilder.define;
import static org.jboss.errai.codegen.meta.MetaClassFactory.parameterizedAs;
import static org.jboss.errai.codegen.meta.MetaClassFactory.typeParametersOf;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.jboss.errai.codegen.builder.ClassStructureBuilder;
import org.jboss.errai.codegen.meta.MetaClassMember;
import org.jboss.errai.codegen.meta.MetaParameter;
import org.jboss.errai.common.metadata.RebindUtils;
import org.jboss.errai.ioc.client.api.ContextualTypeProvider;
import org.jboss.errai.ioc.client.container.Factory;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraph;
import org.jboss.errai.ioc.rebind.ioc.graph.api.Injectable;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder.InjectableType;
import org.jboss.errai.ioc.rebind.ioc.injector.api.InjectionContext;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.IncrementalGenerator;
import com.google.gwt.core.ext.RebindMode;
import com.google.gwt.core.ext.RebindResult;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;

public class FactoryGenerator extends IncrementalGenerator {

  private static final String GENERATED_PACKAGE = "org.jboss.errai.ioc.client";
  private static final Map<String, FactoryBodyGenerator> customBodyGenerators = new HashMap<String, FactoryBodyGenerator>();
  private static DependencyGraph graph;
  private static InjectionContext injectionContext;

  public static void setDependencyGraph(final DependencyGraph graph) {
    FactoryGenerator.graph = graph;
  }

  public static void setInjectionContext(final InjectionContext injectionContext) {
    FactoryGenerator.injectionContext = injectionContext;
  }

  public static void registerCustomBodyGenerator(final String typeName, final FactoryBodyGenerator generator) {
    customBodyGenerators.put(typeName, generator);
  }

  public static String getLocalVariableName(final MetaParameter param) {
    final MetaClassMember member = param.getDeclaringMember();

    return member.getName() + "_" + param.getName() + "_" + param.getIndex();
  }

  private static DependencyGraph assertGraphSet() {
    if (graph == null) {
      throw new RuntimeException("Dependency graph must be generated and set before " + FactoryGenerator.class.getSimpleName() + " runs.");
    }

    return graph;
  }
  private static InjectionContext assertInjectionContextSet() {
    if (injectionContext == null) {
      throw new RuntimeException("Injection context must be set before " + FactoryGenerator.class.getSimpleName() + " runs.");
    }

    return injectionContext;
  }

  @Override
  public RebindResult generateIncrementally(final TreeLogger logger, final GeneratorContext generatorContext, final String typeName)
          throws UnableToCompleteException {
    final DependencyGraph graph = assertGraphSet();
    final InjectionContext injectionContext = assertInjectionContextSet();
    final Injectable injectable = graph.getConcreteInjectable(typeName.substring(typeName.lastIndexOf('.')+1));
    final InjectableType factoryType = injectable.getInjectableType();

    final ClassStructureBuilder<?> factoryBuilder = define(getFactorySubTypeName(typeName),
            parameterizedAs(Factory.class, typeParametersOf(injectable.getInjectedType()))).publicScope().body();
    final FactoryBodyGenerator generator = selectBodyGenerator(factoryType, typeName);

    final String factorySimpleClassName = getFactorySubTypeSimpleName(typeName);
    final PrintWriter pw = generatorContext.tryCreate(logger, GENERATED_PACKAGE, factorySimpleClassName);
    if (pw != null) {
      generator.generate(factoryBuilder, injectable, graph, injectionContext, logger, generatorContext);

      final String factorySource = factoryBuilder.toJavaString();
      final File tmpFile = new File(RebindUtils.getErraiCacheDir().getAbsolutePath() + "/" + factorySimpleClassName + ".java");
      RebindUtils.writeStringToFile(tmpFile, factorySource);
      pw.write(factorySource);
      generatorContext.commit(logger, pw);

      return new RebindResult(RebindMode.USE_ALL_NEW, factoryBuilder.getClassDefinition().getFullyQualifiedName());
    } else {
      return new RebindResult(RebindMode.USE_EXISTING, getFactorySubTypeName(typeName));
    }
  }

  private FactoryBodyGenerator selectBodyGenerator(final InjectableType factoryType, final String typeName) {
    final FactoryBodyGenerator generator;
    switch (factoryType) {
    case Type:
      generator = new TypeFactoryBodyGenerator();
      break;
    case Provider:
      generator = new ProviderFactoryBodyGenerator();
      break;
    case JsType:
      generator = new JsTypeFactoryBodyGenerator();
      break;
    case Producer:
      generator = new ProducerFactoryBodyGenerator();
      break;
    case Extension:
      final String simpleName = getSimpleName(typeName);
      if (!customBodyGenerators.containsKey(simpleName)) {
        throw new RuntimeException(simpleName + " has " + InjectableType.class.getSimpleName() + " " + InjectableType.Extension
                + " but no custom " + FactoryBodyGenerator.class.getSimpleName() + " has been registered.");
      }
      generator = customBodyGenerators.get(simpleName);
      break;
    case ContextualProvider:
      throw new RuntimeException("Types provided by a " + ContextualTypeProvider.class.getSimpleName() + " should not have factories generated.");
    default:
      throw new RuntimeException(factoryType + " not yet implemented!");
    }

    return generator;
  }

  private static String getSimpleName(final String typeName) {
    return typeName.substring(typeName.lastIndexOf('.')+1);
  }

  public static String getFactorySubTypeName(final String typeName) {
    return GENERATED_PACKAGE + "." + getFactorySubTypeSimpleName(typeName);
  }

  public static String getFactorySubTypeSimpleName(final String typeName) {
    final int simpleNameStart = Math.max(typeName.lastIndexOf('.'), typeName.lastIndexOf('$')) + 1;
    return typeName.substring(simpleNameStart);
  }

  @Override
  public long getVersionId() {
    return 1;
  }

}
