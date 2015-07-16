package org.jboss.errai.ioc.rebind.ioc.bootstrapper.spi;

import java.lang.annotation.Annotation;
import java.util.List;

import org.jboss.errai.ioc.rebind.ioc.extension.AnnotationHandler;
import org.jboss.errai.ioc.rebind.ioc.extension.RuleDef;

public interface IOCConfigProcessor {

  <A extends Annotation> void registerHandler(Class<A> annotation, AnnotationHandler<A> handler);

  <A extends Annotation> void registerHandler(Class<A> annotation, AnnotationHandler<A> handler, List<RuleDef> rules);

}