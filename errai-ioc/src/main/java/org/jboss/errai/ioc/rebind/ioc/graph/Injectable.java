package org.jboss.errai.ioc.rebind.ioc.graph;

import java.lang.annotation.Annotation;
import java.util.Collection;

import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraphBuilder.Dependency;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraphBuilder.InjectorType;

public interface Injectable {

  MetaClass getInjectedType();

  Class<? extends Annotation> getScope();

  Qualifier getQualifier();

  String getInjectorClassSimpleName();

  Collection<Dependency> getDependencies();

  InjectorType getInjectorType();

  boolean requiresProxy();

}
