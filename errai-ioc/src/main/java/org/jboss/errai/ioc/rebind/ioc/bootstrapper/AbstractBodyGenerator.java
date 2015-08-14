package org.jboss.errai.ioc.rebind.ioc.bootstrapper;

import static org.jboss.errai.codegen.Parameter.finalOf;
import static org.jboss.errai.codegen.meta.MetaClassFactory.parameterizedAs;
import static org.jboss.errai.codegen.meta.MetaClassFactory.typeParametersOf;
import static org.jboss.errai.codegen.util.PrivateAccessUtil.addPrivateAccessStubs;
import static org.jboss.errai.codegen.util.PrivateAccessUtil.getPrivateMethodName;
import static org.jboss.errai.codegen.util.Stmt.declareFinalVariable;
import static org.jboss.errai.codegen.util.Stmt.loadVariable;
import static org.jboss.errai.codegen.util.Stmt.newObject;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;

import org.jboss.errai.codegen.InnerClass;
import org.jboss.errai.codegen.Modifier;
import org.jboss.errai.codegen.Parameter;
import org.jboss.errai.codegen.Statement;
import org.jboss.errai.codegen.builder.BlockBuilder;
import org.jboss.errai.codegen.builder.ClassStructureBuilder;
import org.jboss.errai.codegen.builder.ContextualStatementBuilder;
import org.jboss.errai.codegen.builder.impl.ClassBuilder;
import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaMethod;
import org.jboss.errai.codegen.meta.MetaParameter;
import org.jboss.errai.codegen.util.Stmt;
import org.jboss.errai.ioc.client.container.Context;
import org.jboss.errai.ioc.client.container.ContextManager;
import org.jboss.errai.ioc.client.container.Proxy;
import org.jboss.errai.ioc.client.container.ProxyHelper;
import org.jboss.errai.ioc.client.container.ProxyHelperImpl;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraph;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraphBuilder.Dependency;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraphBuilder.DependencyType;
import org.jboss.errai.ioc.rebind.ioc.graph.Injectable;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;

public abstract class AbstractBodyGenerator implements InjectorBodyGenerator {

  protected static Multimap<DependencyType, Dependency> separateByType(final Collection<Dependency> dependencies) {
    final Multimap<DependencyType, Dependency> separated = HashMultimap.create();

    for (final Dependency dep : dependencies) {
      separated.put(dep.getDependencyType(), dep);
    }

    return separated;
  }

  protected void implementCreateProxy(final ClassStructureBuilder<?> bodyBlockBuilder, final Injectable injectable) {
    final ClassStructureBuilder<?> proxyImpl = createProxyImplementation(injectable);
    bodyBlockBuilder.declaresInnerClass(new InnerClass(proxyImpl.getClassDefinition()));
    bodyBlockBuilder
            .publicMethod(parameterizedAs(Proxy.class, typeParametersOf(injectable.getInjectedType())), "createProxy",
                    finalOf(Context.class, "context"))
            .body()
            ._(declareFinalVariable("proxyImpl",
                    parameterizedAs(Proxy.class, typeParametersOf(injectable.getInjectedType())),
                    newObject(proxyImpl.getClassDefinition())))
            ._(loadVariable("proxyImpl").invoke("setContext", loadVariable("context")))
            ._(loadVariable("proxyImpl").returnValue()).finish();
  }

  private ClassStructureBuilder<?> createProxyImplementation(final Injectable injectable) {
    final ClassStructureBuilder<?> proxyImpl;
    if (injectable.getInjectedType().isInterface()) {
      proxyImpl = ClassBuilder
              .define(injectable.getInjectorClassSimpleName() + "ProxyImpl")
              .privateScope()
              .implementsInterface(parameterizedAs(Proxy.class, typeParametersOf(injectable.getInjectedType())))
              .implementsInterface(injectable.getInjectedType()).body();
    } else {
      proxyImpl = ClassBuilder
              .define(injectable.getInjectorClassSimpleName() + "ProxyImpl", injectable.getInjectedType())
              .privateScope()
              .implementsInterface(parameterizedAs(Proxy.class, typeParametersOf(injectable.getInjectedType()))).body();
    }

    proxyImpl
            .privateField("proxyHelper",
                    parameterizedAs(ProxyHelper.class, typeParametersOf(injectable.getInjectedType())))
            .modifiers(Modifier.Final)
            .initializesWith(Stmt.newObject(
                    parameterizedAs(ProxyHelperImpl.class, typeParametersOf(injectable.getInjectedType())),
                    injectable.getInjectorClassSimpleName()))
            .finish();

    implementProxyMethods(proxyImpl, injectable);
    implementAccessibleMethods(proxyImpl, injectable);

    return proxyImpl;
  }

  private void implementAccessibleMethods(final ClassStructureBuilder<?> proxyImpl, final Injectable injectable) {
    final MetaClass injectableType = injectable.getInjectedType();
    for (final MetaMethod method : injectableType.getMethods()) {
      // TODO clean this up and maybe proxy package private and proetected methods?
      if (method.isPublic() && (method.asMethod() == null || method.asMethod().getDeclaringClass() == null
              || !method.asMethod().getDeclaringClass().equals(Object.class))) {
        final BlockBuilder<?> body = proxyImpl
                .publicMethod(method.getReturnType(), method.getName(), getParametersForDeclaration(method))
                .annotatedWith(new Override() {
                  @Override
                  public Class<? extends Annotation> annotationType() {
                    return Override.class;
                  }
                }).body();
        final ContextualStatementBuilder invocation = loadVariable("proxyHelper").invoke("getInstance").invoke(method.getName(), getParametersForInvocation(method));
        if (method.getReturnType().isVoid()) {
          body._(invocation);
        } else {
          body._(invocation.returnValue());
        }

        body.finish();
      }
    }
  }

  private Object[] getParametersForInvocation(final MetaMethod method) {
    final Object[] params = new Object[method.getParameters().length];
    final MetaParameter[] declaredParams = method.getParameters();
    for (int i = 0; i < declaredParams.length; i++) {
      params[i] = loadVariable(declaredParams[i].getName());
    }

    return params;
  }

  private Parameter[] getParametersForDeclaration(final MetaMethod method) {
    final MetaParameter[] metaParams = method.getParameters();
    final Parameter[] params = new Parameter[metaParams.length];

    for (int i = 0; i < params.length; i++) {
      params[i] = finalOf(metaParams[i].getType(), metaParams[i].getName());
    }

    return params;
  }

  private void implementProxyMethods(final ClassStructureBuilder<?> proxyImpl, final Injectable injectable) {
    implementAsBeanType(proxyImpl, injectable);
    implementSetInstance(proxyImpl, injectable);
    implementClearInstance(proxyImpl, injectable);
    implementSetContext(proxyImpl, injectable);
  }

  private void implementSetContext(final ClassStructureBuilder<?> proxyImpl, final Injectable injectable) {
    proxyImpl.publicMethod(void.class, "setContext", finalOf(Context.class, "context"))
             .body()
             ._(loadVariable("proxyHelper").invoke("setContext", loadVariable("context")))
             .finish();
  }

  private void implementClearInstance(final ClassStructureBuilder<?> proxyImpl, final Injectable injectable) {
    proxyImpl.publicMethod(void.class, "clearInstance")
             .body()
             ._(loadVariable("proxyHelper").invoke("clearInstance"))
             .finish();
  }

  private void implementSetInstance(final ClassStructureBuilder<?> proxyImpl, final Injectable injectable) {
    proxyImpl.publicMethod(void.class, "setInstance", finalOf(injectable.getInjectedType(), "instance"))
             .body()
             ._(loadVariable("proxyHelper").invoke("setInstance", loadVariable("instance")))
             .finish();
  }

  private void implementAsBeanType(final ClassStructureBuilder<?> proxyImpl, final Injectable injectable) {
    proxyImpl.publicMethod(injectable.getInjectedType(), "asBeanType")
             .body()
             ._(loadVariable("this").returnValue())
             .finish();
  }

  protected void implementCreateInstance(final ClassStructureBuilder<?> bodyBlockBuilder, final Injectable injectable, final List<Statement> createInstanceStatements) {
    bodyBlockBuilder.publicMethod(injectable.getInjectedType(), "createInstance", finalOf(ContextManager.class, "contextManager"))
                    .appendAll(createInstanceStatements)
                    .finish();
  }

  protected void addReturnStatement(final List<Statement> createInstanceStatements) {
    createInstanceStatements.add(loadVariable("instance").returnValue());
  }

  protected String addPrivateMethodAccessor(final MetaMethod postConstruct, final ClassStructureBuilder<?> bodyBlockBuilder) {
    addPrivateAccessStubs("jsni", bodyBlockBuilder, postConstruct);

    return getPrivateMethodName(postConstruct);
  }

  protected abstract List<Statement> generateCreateInstanceStatements(ClassStructureBuilder<?> bodyBlockBuilder, Injectable injectable, DependencyGraph graph);

  @Override
  public void generate(final ClassStructureBuilder<?> bodyBlockBuilder, final Injectable injectable, final DependencyGraph graph, final TreeLogger logger, final GeneratorContext context) {
    final List<Statement> createInstanceStatements = generateCreateInstanceStatements(bodyBlockBuilder, injectable, graph);

    implementCreateInstance(bodyBlockBuilder, injectable, createInstanceStatements);

    implementCreateProxy(bodyBlockBuilder, injectable);
  }

}
