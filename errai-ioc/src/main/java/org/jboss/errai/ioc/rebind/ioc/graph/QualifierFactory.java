package org.jboss.errai.ioc.rebind.ioc.graph;

import org.jboss.errai.codegen.meta.HasAnnotations;

public interface QualifierFactory {

  Qualifier create(HasAnnotations annotated);

  Qualifier unqualified();

}
