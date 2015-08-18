package org.jboss.errai.ioc.rebind.ioc.graph;

import org.jboss.errai.codegen.meta.HasAnnotations;

/**
 * @author Max Barkley <mbarkley@redhat.com>
 */
public interface QualifierFactory {

  Qualifier forConcreteInjectable(HasAnnotations annotated);

  Qualifier forAbstractInjectable(HasAnnotations annotated);

  Qualifier forUnqualified();

  Qualifier forUniversallyQualified();

}
