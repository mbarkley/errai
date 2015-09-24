package org.jboss.errai.ioc.rebind.ioc.bootstrapper;

import org.jboss.errai.codegen.builder.ClassStructureBuilder;
import org.jboss.errai.ioc.client.container.Factory;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraph;
import org.jboss.errai.ioc.rebind.ioc.graph.api.Injectable;
import org.jboss.errai.ioc.rebind.ioc.injector.api.InjectionContext;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;

/**
 * Used by the {@link FactoryGenerator} for generating the body of
 * {@link Factory} subclasses.
 *
 * @see AbstractBodyGenerator
 * @see TypeFactoryBodyGenerator
 * @author Max Barkley <mbarkley@redhat.com>
 */
public interface FactoryBodyGenerator {

  /**
   * Generates a {@link Factory} subclasses body into the given
   * {@link ClassStructureBuilder}. Must implement all the abstract methods of
   * {@link Factory}.
   *
   * @param bodyBlockBuilder
   *          The {@link ClassStructureBuilder} for the {@link Factory} being
   *          generated.
   * @param injectable
   *          The {@link Injectable} for the bean of the {@link Factory} being
   *          generated.
   * @param graph
   *          The {@link DependencyGraph} that the {@link Injectable} parameter
   *          is from.
   * @param injectionContext
   *          The single {@link InjectionContext} shared by all
   *          {@link FactoryBodyGenerator FactoryBodyGenerators}.
   * @param logger
   *          For logging errors to GWT.
   * @param context
   *          The generation context for this rebind.
   */
  void generate(ClassStructureBuilder<?> bodyBlockBuilder, Injectable injectable, DependencyGraph graph,
          InjectionContext injectionContext, TreeLogger logger, GeneratorContext context);
}