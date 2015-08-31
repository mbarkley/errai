package org.jboss.errai.ioc.rebind.ioc.graph;

import java.lang.annotation.Annotation;
import java.util.Collection;

import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraphBuilder.Dependency;
import org.jboss.errai.ioc.rebind.ioc.graph.DependencyGraphBuilder.InjectableType;
import org.jboss.errai.ioc.rebind.ioc.injector.api.WiringElementType;

public interface Injectable {

  InjectableHandle getHandle();

  MetaClass getInjectedType();

  Class<? extends Annotation> getScope();

  Qualifier getQualifier();

  String getFactoryName();

  Collection<Dependency> getDependencies();

  InjectableType getInjectableType();

  Collection<WiringElementType> getWiringElementTypes();

  boolean requiresProxy();

  void setRequiresProxyTrue();

  boolean isContextual();

  boolean isTransient();

}
