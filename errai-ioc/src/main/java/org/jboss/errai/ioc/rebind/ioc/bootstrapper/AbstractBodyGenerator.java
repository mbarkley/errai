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
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Queue;

import javax.inject.Qualifier;

import org.jboss.errai.codegen.InnerClass;
import org.jboss.errai.codegen.Modifier;
import org.jboss.errai.codegen.Parameter;
import org.jboss.errai.codegen.Statement;
import org.jboss.errai.codegen.builder.BlockBuilder;
import org.jboss.errai.codegen.builder.ClassStructureBuilder;
import org.jboss.errai.codegen.builder.ConstructorBlockBuilder;
import org.jboss.errai.codegen.builder.ContextualStatementBuilder;
import org.jboss.errai.codegen.builder.impl.ClassBuilder;
import org.jboss.errai.codegen.literal.LiteralFactory;
import org.jboss.errai.codegen.meta.HasAnnotations;
import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaClassFactory;
import org.jboss.errai.codegen.meta.MetaMethod;
import org.jboss.errai.codegen.meta.MetaParameter;
import org.jboss.errai.codegen.util.Stmt;
import org.jboss.errai.ioc.client.api.EntryPoint;
import org.jboss.errai.ioc.client.container.Context;
import org.jboss.errai.ioc.client.container.ContextManager;
import org.jboss.errai.ioc.client.container.FactoryHandle;
import org.jboss.errai.ioc.client.container.FactoryHandleImpl;
import org.jboss.errai.ioc.client.container.NonProxiableWrapper;
import org.jboss.errai.ioc.client.container.Proxy;
import org.jboss.errai.ioc.client.container.ProxyHelper;
import org.jboss.errai.ioc.client.container.ProxyHelperImpl;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraph;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraphBuilder.Dependency;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraphBuilder.DependencyType;
import org.jboss.errai.ioc.rebind.ioc.graph.Injectable;
import org.jboss.errai.ioc.rebind.ioc.injector.api.FactoryController;
import org.jboss.errai.ioc.rebind.ioc.injector.api.InjectionContext;
import org.jboss.errai.ioc.rebind.ioc.injector.api.WiringElementType;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;

public abstract class AbstractBodyGenerator implements FactoryBodyGenerator {

  protected static Multimap<DependencyType, Dependency> separateByType(final Collection<Dependency> dependencies) {
    final Multimap<DependencyType, Dependency> separated = HashMultimap.create();

    for (final Dependency dep : dependencies) {
      separated.put(dep.getDependencyType(), dep);
    }

    return separated;
  }

  protected final FactoryController controller = new FactoryController();

  protected void implementCreateProxy(final ClassStructureBuilder<?> bodyBlockBuilder, final Injectable injectable) {
    final MetaClass proxyImpl = maybeCreateProxyImplementation(injectable, bodyBlockBuilder);
    final Object newProxyImplStmt;

    final BlockBuilder<?> createProxyBody = bodyBlockBuilder
            .publicMethod(parameterizedAs(Proxy.class, typeParametersOf(injectable.getInjectedType())), "createProxy",
                    finalOf(Context.class, "context"))
            .body();
    // TODO clean this up
    if (proxyImpl.getFullyQualifiedName().equals(NonProxiableWrapper.class.getName())) {
      newProxyImplStmt = newObject(
              parameterizedAs(NonProxiableWrapper.class, typeParametersOf(injectable.getInjectedType())),
              loadVariable("this").invoke("createInstance", loadVariable("context").invoke("getContextManager")));
    } else {
      newProxyImplStmt = newObject(proxyImpl);
    }

    createProxyBody
            ._(declareFinalVariable("proxyImpl",
                    parameterizedAs(Proxy.class, typeParametersOf(injectable.getInjectedType())), newProxyImplStmt))
            ._(loadVariable("proxyImpl").invoke("setContext", loadVariable("context")))
            ._(loadVariable("proxyImpl").returnValue()).finish();
  }

  private MetaClass maybeCreateProxyImplementation(final Injectable injectable, final ClassStructureBuilder<?> bodyBlockBuilder) {

    final ClassStructureBuilder<?> proxyImpl;
    final MetaClass injectedType = injectable.getInjectedType();
    if (injectedType.isInterface()) {
      proxyImpl = ClassBuilder
              .define(injectable.getFactoryName() + "ProxyImpl")
              .privateScope()
              .implementsInterface(parameterizedAs(Proxy.class, typeParametersOf(injectedType)))
              .implementsInterface(injectedType).body();
      declareAndInitializeProxyHelper(injectable, proxyImpl);
    } else if (isProxiable(injectable)) {
      proxyImpl = ClassBuilder
              .define(injectable.getFactoryName() + "ProxyImpl", injectedType)
              .privateScope()
              .implementsInterface(parameterizedAs(Proxy.class, typeParametersOf(injectedType))).body();
      declareAndInitializeProxyHelper(injectable, proxyImpl);
    } else {
      if (!injectable.requiresProxy()) {
        return parameterizedAs(NonProxiableWrapper.class, typeParametersOf(injectedType));
      } else {
        throw new RuntimeException(injectedType + " must be proxiable but is not default instatiable.");
      }
    }

    for (final Entry<String, Statement> prop : controller.getProxyProperties()) {
      proxyImpl.privateField(prop.getKey(), prop.getValue().getType()).initializesWith(prop.getValue()).finish();
    }

    implementProxyMethods(proxyImpl, injectable);
    implementAccessibleMethods(proxyImpl, injectable);

    bodyBlockBuilder.declaresInnerClass(new InnerClass(proxyImpl.getClassDefinition()));

    return proxyImpl.getClassDefinition();
  }

  private boolean isProxiable(final Injectable injectable) {
    final MetaClass type = injectable.getInjectedType();

    return !injectable.getWiringElementTypes().contains(WiringElementType.Simpleton) && type.isDefaultInstantiable() && !type.isFinal();
  }

  private void declareAndInitializeProxyHelper(final Injectable injectable, final ClassStructureBuilder<?> bodyBlockBuilder) {
    bodyBlockBuilder
            .privateField("proxyHelper",
                    parameterizedAs(ProxyHelper.class, typeParametersOf(injectable.getInjectedType())))
            .modifiers(Modifier.Final)
            .initializesWith(initializeProxyHelper(injectable))
            .finish();
  }

  private Statement initializeProxyHelper(final Injectable injectable) {
    return newObject(
            parameterizedAs(ProxyHelperImpl.class, typeParametersOf(injectable.getInjectedType())),
            injectable.getFactoryName());
  }

  private void implementAccessibleMethods(final ClassStructureBuilder<?> proxyImpl, final Injectable injectable) {
    final MetaClass injectableType = injectable.getInjectedType();
    for (final MetaMethod method : injectableType.getMethods()) {
      // TODO clean this up and maybe proxy package private and proetected methods?
      if (!method.isStatic() && method.isPublic() && !method.isFinal() && (method.asMethod() == null || method.asMethod().getDeclaringClass() == null
              || !method.asMethod().getDeclaringClass().equals(Object.class))) {
        final BlockBuilder<?> body = proxyImpl
                .publicMethod(method.getReturnType().getErased(), method.getName(), getParametersForDeclaration(method))
                .annotatedWith(new Override() {
                  @Override
                  public Class<? extends Annotation> annotationType() {
                    return Override.class;
                  }
                })
                .throws_(method.getCheckedExceptions()).body();
        body.appendAll(controller.getInvokeBeforeStatements(method));
        final ContextualStatementBuilder invocation = loadVariable("proxyHelper").invoke("getInstance").invoke(method.getName(), getParametersForInvocation(method));
        if (method.getReturnType().isVoid()) {
          body._(invocation);
          body.appendAll(controller.getInvokeAfterStatements(method));
        } else {
          body._(declareFinalVariable("retVal", method.getReturnType(), invocation));
          body.appendAll(controller.getInvokeAfterStatements(method));
          body._(loadVariable("retVal").returnValue());
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
      params[i] = finalOf(metaParams[i].getType().getErased(), metaParams[i].getName());
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

  protected abstract List<Statement> generateCreateInstanceStatements(ClassStructureBuilder<?> bodyBlockBuilder, Injectable injectable, DependencyGraph graph, InjectionContext injectionContext);

  @Override
  public void generate(final ClassStructureBuilder<?> bodyBlockBuilder, final Injectable injectable, final DependencyGraph graph, InjectionContext injectionContext, final TreeLogger logger, final GeneratorContext context) {
    final List<Statement> createInstanceStatements = generateCreateInstanceStatements(bodyBlockBuilder, injectable, graph, injectionContext);
    final List<Statement> destroyInstanceStatements = generateDestroyInstanceStatements(bodyBlockBuilder, injectable, graph, injectionContext);

    implementCreateInstance(bodyBlockBuilder, injectable, createInstanceStatements);
    implementDestroyInstance(bodyBlockBuilder, injectable, destroyInstanceStatements);
    implementCreateProxy(bodyBlockBuilder, injectable);
    implementGetHandle(bodyBlockBuilder, injectable);
  }

  private void implementDestroyInstance(final ClassStructureBuilder<?> bodyBlockBuilder, final Injectable injectable,
          final List<Statement> destroyInstanceStatements) {
    bodyBlockBuilder
            .publicMethod(void.class, "generatedDestroyInstance", finalOf(Object.class, "instance"),
                    finalOf(ContextManager.class, "contextManager"))
            .append(loadVariable("this").invoke("destroyInstanceHelper",
                    Stmt.castTo(injectable.getInjectedType(), loadVariable("instance")),
                    loadVariable("contextManager")))
            .finish();
    bodyBlockBuilder.publicMethod(void.class, "destroyInstanceHelper",
            finalOf(injectable.getInjectedType(), "instance"), finalOf(ContextManager.class, "contextManager"))
            .appendAll(destroyInstanceStatements).finish();
  }

  protected List<Statement> generateDestroyInstanceStatements(final ClassStructureBuilder<?> bodyBlockBuilder,
          final Injectable injectable, final DependencyGraph graph, final InjectionContext injectionContext) {
    return controller.getDestructionStatements();
  }

  private void implementGetHandle(final ClassStructureBuilder<?> bodyBlockBuilder, final Injectable injectable) {
    bodyBlockBuilder.privateField("handle", FactoryHandleImpl.class)
            .initializesWith(newObject(FactoryHandleImpl.class, injectable.getInjectedType().asClass(),
                    injectable.getFactoryName(), injectable.getScope(), isEager(injectable.getInjectedType())))
            .finish();
    final ConstructorBlockBuilder<?> con = bodyBlockBuilder.publicConstructor();
    for (final MetaClass assignableType : getAllAssignableTypes(injectable.getInjectedType())) {
      con._(loadVariable("handle").invoke("addAssignableType", assignableType.asClass()));
    }
    for (final Annotation qual : getQualifiers(injectable.getInjectedType())) {
      con._(loadVariable("handle").invoke("addQualifier", annotationLiteral(qual)));
    }
    con.finish();
    bodyBlockBuilder.publicMethod(FactoryHandle.class, "getHandle").body()._(loadVariable("handle").returnValue())
            .finish();
  }

  private Object isEager(final MetaClass injectedType) {
    // TODO Support other annotations like @Startup
    return injectedType.isAnnotationPresent(EntryPoint.class);
  }

  private Object annotationLiteral(final Annotation qual) {
    return LiteralFactory.getLiteral(qual);
  }

  protected Collection<Annotation> getQualifiers(final HasAnnotations injectedType) {
    final Collection<Annotation> annos = new ArrayList<Annotation>();
    for (final Annotation anno : injectedType.getAnnotations()) {
      if (anno.annotationType().isAnnotationPresent(Qualifier.class)) {
        annos.add(anno);
      }
    }

    return annos;
  }

  private Collection<MetaClass> getAllAssignableTypes(final MetaClass injectedType) {
    final Collection<MetaClass> assignableTypes = new ArrayList<MetaClass>();

    final Queue<MetaClass> ifaces = new LinkedList<MetaClass>();
    if (injectedType.isInterface()) {
      ifaces.add(injectedType);
    } else {
      ifaces.addAll(getPublicInterfaces(injectedType));
    }
    while (ifaces.size() > 0) {
      final MetaClass iface = ifaces.poll().getErased();
      assignableTypes.add(iface);
      ifaces.addAll(getPublicInterfaces(iface));
    }

    if (!injectedType.isInterface()) {
      MetaClass type = injectedType.getErased();
      do {
        if (type.isPublic()) {
          assignableTypes.add(type);
        }
        type = type.getSuperClass().getErased();
      } while (!type.getFullyQualifiedName().equals("java.lang.Object"));
      assignableTypes.add(MetaClassFactory.get(Object.class));
    }

    return assignableTypes;
  }

  private Collection<MetaClass> getPublicInterfaces(final MetaClass injectedType) {
    final Collection<MetaClass> ifaces = new ArrayList<MetaClass>();
    for (final MetaClass iface : injectedType.getInterfaces()) {
      if (iface.isPublic()) {
        ifaces.add(iface);
      }
    }

    return ifaces;
  }

}
