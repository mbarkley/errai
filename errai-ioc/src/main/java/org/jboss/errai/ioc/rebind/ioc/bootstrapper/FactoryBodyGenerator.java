package org.jboss.errai.ioc.rebind.ioc.bootstrapper;

import org.jboss.errai.codegen.builder.ClassStructureBuilder;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraph;
import org.jboss.errai.ioc.rebind.ioc.graph.api.Injectable;
import org.jboss.errai.ioc.rebind.ioc.injector.api.InjectionContext;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;

public interface FactoryBodyGenerator {
  void generate(ClassStructureBuilder<?> bodyBlockBuilder, Injectable injectable, DependencyGraph graph, InjectionContext injectionContext, TreeLogger logger, GeneratorContext context);
}