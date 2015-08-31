package org.jboss.errai.ioc.rebind.ioc.bootstrapper;

import static org.jboss.errai.codegen.meta.MetaClassFactory.parameterizedAs;
import static org.jboss.errai.codegen.meta.MetaClassFactory.typeParametersOf;
import static org.jboss.errai.codegen.util.Bool.instanceOf;
import static org.jboss.errai.codegen.util.PrivateAccessUtil.addPrivateAccessStubs;
import static org.jboss.errai.codegen.util.PrivateAccessUtil.getPrivateFieldAccessorName;
import static org.jboss.errai.codegen.util.PrivateAccessUtil.getPrivateMethodName;
import static org.jboss.errai.codegen.util.Stmt.castTo;
import static org.jboss.errai.codegen.util.Stmt.declareVariable;
import static org.jboss.errai.codegen.util.Stmt.if_;
import static org.jboss.errai.codegen.util.Stmt.loadVariable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jboss.errai.codegen.Statement;
import org.jboss.errai.codegen.builder.ClassStructureBuilder;
import org.jboss.errai.codegen.meta.MetaClassMember;
import org.jboss.errai.codegen.meta.MetaField;
import org.jboss.errai.codegen.meta.MetaMethod;
import org.jboss.errai.codegen.util.PrivateAccessType;
import org.jboss.errai.ioc.client.container.Proxy;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraph;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraphBuilder.Dependency;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraphBuilder.DependencyType;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraphBuilder.ParamDependency;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraphBuilder.ProducerInstanceDependency;
import org.jboss.errai.ioc.rebind.ioc.graph.Injectable;
import org.jboss.errai.ioc.rebind.ioc.injector.api.InjectionContext;

import com.google.common.collect.Multimap;

public class ProducerFactoryBodyGenerator extends AbstractBodyGenerator {

  @Override
  protected List<Statement> generateCreateInstanceStatements(final ClassStructureBuilder<?> bodyBlockBuilder,
          final Injectable injectable, final DependencyGraph graph, final InjectionContext injectionContext) {
    final Multimap<DependencyType, Dependency> depsByType = separateByType(injectable.getDependencies());
    if (depsByType.get(DependencyType.ProducerMember).size() != 1) {
      throw new RuntimeException("A produced type must have exactly 1 producing instance but "
              + depsByType.get(DependencyType.ProducerMember).size() + " were found.");
    }
    final ProducerInstanceDependency producerInstanceDep = (ProducerInstanceDependency) depsByType.get(DependencyType.ProducerMember).iterator().next();
    final Injectable producerInjectable = producerInstanceDep.getInjectable();
    final MetaClassMember producingMember = producerInstanceDep.getProducingMember();

    if (producingMember instanceof MetaField) {
      return fieldCreateInstanceStatements((MetaField) producingMember, producerInjectable, bodyBlockBuilder);
    } else if (producingMember instanceof MetaMethod) {
      return methodCreateInstanceStatements((MetaMethod) producingMember, producerInjectable, depsByType.get(DependencyType.ProducerParameter), bodyBlockBuilder);
    } else {
      throw new RuntimeException("Unrecognized producing member: " + producingMember);
    }
  }

  private List<Statement> methodCreateInstanceStatements(final MetaMethod producingMember, final Injectable producerInjectable,
          final Collection<Dependency> paramDeps, final ClassStructureBuilder<?> bodyBlockBuilder) {
    final List<Statement> stmts = new ArrayList<Statement>();
    addPrivateAccessStubs("jsni", bodyBlockBuilder, producingMember);

    final Statement producerInstanceValue = loadVariable("contextManager").invoke("getInstance", producerInjectable.getFactoryName());
    stmts.add(declareVariable("producerInstance", producerInjectable.getInjectedType(), producerInstanceValue));
    // TODO figure out if proxied at compile time to simplify this code
    stmts.add(
            if_(instanceOf(
                    loadVariable(
                            "producerInstance"),
                    Proxy.class))._(loadVariable("producerInstance").assignValue(
                            castTo(parameterizedAs(Proxy.class, typeParametersOf(producerInjectable.getInjectedType())),
                                    loadVariable("producerInstance")).invoke("unwrappedInstance")))
                            .finish());
    stmts.add(loadVariable("this").invoke(getPrivateMethodName(producingMember), getProducerInvocationParams(producingMember, paramDeps)).returnValue());

    return stmts;
  }

  private Object[] getProducerInvocationParams(final MetaMethod producingMember, final Collection<Dependency> paramDeps) {
    // TODO validate params
    final int offset;
    final Object[] params;
    if (producingMember.isStatic()) {
      offset = 0;
      params = new Object[producingMember.getParameters().length];
    } else {
      offset = 1;
      params = new Object[producingMember.getParameters().length+1];
      params[0] = loadVariable("producerInstance");
    }

    for (final Dependency dep : paramDeps) {
      final ParamDependency paramDep = (ParamDependency) dep;
      params[paramDep.getParamIndex()+offset] = loadVariable("contextManager").invoke("getInstance", paramDep.getInjectable().getFactoryName());
    }

    return params;
  }

  private List<Statement> fieldCreateInstanceStatements(final MetaField producingMember, final Injectable producerInjectable,
          final ClassStructureBuilder<?> bodyBlockBuilder) {
    final List<Statement> stmts = new ArrayList<Statement>();
    addPrivateAccessStubs(PrivateAccessType.Read, "jsni", bodyBlockBuilder, producingMember);

    final Statement producerInstanceValue = loadVariable("contextManager").invoke("getInstance", producerInjectable.getFactoryName());
    stmts.add(declareVariable("producerInstance", producerInjectable.getInjectedType(), producerInstanceValue));
    // TODO figure out if proxied at compile time to simplify this code
    stmts.add(
            if_(instanceOf(
                    loadVariable(
                            "producerInstance"),
                    Proxy.class))._(loadVariable("producerInstance").assignValue(
                            castTo(parameterizedAs(Proxy.class, typeParametersOf(producerInjectable.getInjectedType())),
                                    loadVariable("producerInstance")).invoke("unwrappedInstance")))
                            .finish());
    final Object[] params = (producingMember.isStatic()) ? new Object[0] : new Object[] { loadVariable("producerInstance") };
    stmts.add(loadVariable("this").invoke(getPrivateFieldAccessorName(producingMember), params).returnValue());

    return stmts;
  }

  @Override
  protected List<Statement> generateDestroyInstanceStatements(final ClassStructureBuilder<?> bodyBlockBuilder,
          final Injectable injectable, final DependencyGraph graph, final InjectionContext injectionContext) {
    final List<Statement> destroyInstanceStmts = new ArrayList<Statement>();

    return destroyInstanceStmts;
  }

}
