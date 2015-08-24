package org.jboss.errai.ioc.rebind.ioc.bootstrapper;

import static org.jboss.errai.codegen.meta.MetaClassFactory.parameterizedAs;
import static org.jboss.errai.codegen.meta.MetaClassFactory.typeParametersOf;
import static org.jboss.errai.codegen.util.Stmt.castTo;
import static org.jboss.errai.codegen.util.Stmt.loadVariable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Provider;

import org.jboss.errai.codegen.Statement;
import org.jboss.errai.codegen.builder.ClassStructureBuilder;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraph;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraphBuilder.Dependency;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraphBuilder.DependencyType;
import org.jboss.errai.ioc.rebind.ioc.graph.Injectable;

import com.google.common.collect.Multimap;

public class ProviderFactoryBodyGenerator extends AbstractBodyGenerator {

  @Override
  protected List<Statement> generateCreateInstanceStatements(final ClassStructureBuilder<?> bodyBlockBuilder, final Injectable injectable, final DependencyGraph graph) {
    final Multimap<DependencyType, Dependency> dependenciesByType = separateByType(injectable.getDependencies());
    assert dependenciesByType.size() == 1 : "The factory " + injectable.getFactoryName() + " is a Provider and should have exactly one dependency";
    final Collection<Dependency> providerInstanceDeps = dependenciesByType.get(DependencyType.ProducerInstance);
    assert providerInstanceDeps.size() == 1 : "The factory " + injectable.getFactoryName()
            + " is a Provider but does not have a " + DependencyType.ProducerInstance.toString() + " depenency.";

    final Dependency providerDep = providerInstanceDeps.iterator().next();
    final List<Statement> createInstanceStatements = getAndInvokeProvider(injectable, providerDep);

    return createInstanceStatements;
  }

  private List<Statement> getAndInvokeProvider(final Injectable injectable, final Dependency providerDep) {
    final Injectable providerInjectable = providerDep.getInjectable();
    final List<Statement> statement = new ArrayList<Statement>(1);

    statement.add(castTo(parameterizedAs(Provider.class, typeParametersOf(injectable.getInjectedType())),
            loadVariable("contextManager").invoke("getInstance", providerInjectable.getFactoryName()))
                    .invoke("get").returnValue());

    return statement;
  }

}
