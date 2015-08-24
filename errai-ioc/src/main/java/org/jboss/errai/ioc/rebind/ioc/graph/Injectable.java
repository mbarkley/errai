package org.jboss.errai.ioc.rebind.ioc.graph;

import java.lang.annotation.Annotation;
import java.util.Collection;

import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraphBuilder.Dependency;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraphBuilder.FactoryType;
import org.jboss.errai.ioc.rebind.ioc.injector.api.WiringElementType;

public interface Injectable {

  MetaClass getInjectedType();

  Class<? extends Annotation> getScope();

  Qualifier getQualifier();

  String getFactoryName();

  Collection<Dependency> getDependencies();

  FactoryType getFactoryType();

  Collection<WiringElementType> getWiringElementTypes();

  boolean requiresProxy();

  boolean isContextual();

}
