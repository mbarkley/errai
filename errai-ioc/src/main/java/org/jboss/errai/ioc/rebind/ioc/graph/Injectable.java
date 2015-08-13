package org.jboss.errai.ioc.rebind.ioc.graph;

import java.lang.annotation.Annotation;

import org.jboss.errai.codegen.meta.MetaClass;

public interface Injectable {

  MetaClass getInjectedType();

  Class<? extends Annotation> getScope();

  Qualifier getQualifier();

  String getInjectorClassSimpleName();

}
