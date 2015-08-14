package org.jboss.errai.ioc.rebind.ioc.bootstrapper;

import org.jboss.errai.codegen.builder.ClassStructureBuilder;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraph;
import org.jboss.errai.ioc.rebind.ioc.graph.Injectable;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;

interface InjectorBodyGenerator {
  void generate(ClassStructureBuilder<?> bodyBlockBuilder, Injectable injectable, DependencyGraph graph, TreeLogger logger, GeneratorContext context);
}