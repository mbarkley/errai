package org.jboss.errai.ioc.rebind.ioc.bootstrapper;

import static org.jboss.errai.codegen.util.Stmt.invokeStatic;

import java.util.Collections;
import java.util.List;

import org.jboss.errai.codegen.Statement;
import org.jboss.errai.codegen.builder.ClassStructureBuilder;
import org.jboss.errai.codegen.util.Stmt;
import org.jboss.errai.ioc.client.WindowInjectionContext;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraph;
import org.jboss.errai.ioc.rebind.ioc.graph.api.Injectable;
import org.jboss.errai.ioc.rebind.ioc.injector.api.InjectionContext;

public class JsTypeFactoryBodyGenerator extends AbstractBodyGenerator {

  @Override
  protected List<Statement> generateCreateInstanceStatements(ClassStructureBuilder<?> bodyBlockBuilder,
          Injectable injectable, DependencyGraph graph, InjectionContext injectionContext) {
    return Collections.<Statement> singletonList(
            Stmt.castTo(injectable.getInjectedType(), invokeStatic(WindowInjectionContext.class, "createOrGet")
                    .invoke("getBean", injectable.getInjectedType().getFullyQualifiedName())).returnValue());
  }

}
