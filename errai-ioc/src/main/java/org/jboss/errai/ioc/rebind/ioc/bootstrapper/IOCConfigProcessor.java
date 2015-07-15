package org.jboss.errai.ioc.rebind.ioc.bootstrapper;

import java.lang.annotation.Annotation;
import java.util.List;

import org.jboss.errai.ioc.rebind.ioc.extension.AnnotationHandler;
import org.jboss.errai.ioc.rebind.ioc.extension.RuleDef;

public interface IOCConfigProcessor {

  void registerHandler(Class<? extends Annotation> annotation, AnnotationHandler handler);

  void registerHandler(Class<? extends Annotation> annotation, AnnotationHandler handler, List<RuleDef> rules);

}