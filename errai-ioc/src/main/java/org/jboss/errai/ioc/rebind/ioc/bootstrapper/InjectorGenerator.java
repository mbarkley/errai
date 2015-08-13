package org.jboss.errai.ioc.rebind.ioc.bootstrapper;

import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraph;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.IncrementalGenerator;
import com.google.gwt.core.ext.RebindResult;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;

public class InjectorGenerator extends IncrementalGenerator {

  private static DependencyGraph graph;

  public static void setDependencyGraph(final DependencyGraph graph) {
    InjectorGenerator.graph = graph;
  }

  @Override
  public RebindResult generateIncrementally(TreeLogger logger, GeneratorContext context, String typeName)
          throws UnableToCompleteException {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
  }

  @Override
  public long getVersionId() {
    return 1;
  }

}
