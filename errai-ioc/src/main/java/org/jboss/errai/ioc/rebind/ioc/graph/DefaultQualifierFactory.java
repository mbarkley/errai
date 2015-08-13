package org.jboss.errai.ioc.rebind.ioc.graph;

import org.jboss.errai.codegen.meta.HasAnnotations;

public class DefaultQualifierFactory implements QualifierFactory {

  private static final Qualifier universal = new Qualifier() {

    @Override
    public boolean isSatisfiedBy(Qualifier other) {
      return true;
    }

    @Override
    public String toString() {
      return "UNIVERSAL";
    }

    @Override
    public String getIdentifierSafeString() {
      return "Universal";
    }
  };

  @Override
  public Qualifier create(HasAnnotations annotated) {
    return universal;
  }

  @Override
  public Qualifier unqualified() {
    return universal;
  }

  @Override
  public Qualifier universalQualifier() {
    return universal;
  }

}
