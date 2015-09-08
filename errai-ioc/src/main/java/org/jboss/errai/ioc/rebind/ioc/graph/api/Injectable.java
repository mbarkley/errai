package org.jboss.errai.ioc.rebind.ioc.graph.api;

import java.lang.annotation.Annotation;
import java.util.Collection;

import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder.Dependency;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder.InjectableType;
import org.jboss.errai.ioc.rebind.ioc.graph.impl.InjectableHandle;
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
