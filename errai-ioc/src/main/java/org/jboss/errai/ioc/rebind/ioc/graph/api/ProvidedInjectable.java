package org.jboss.errai.ioc.rebind.ioc.graph.api;

import java.lang.annotation.Annotation;

import org.jboss.errai.codegen.meta.HasAnnotations;
import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaField;
import org.jboss.errai.codegen.meta.MetaParameter;

public interface ProvidedInjectable extends Injectable {

  public InjectionSite getInjectionSite();

  public static class InjectionSite implements HasAnnotations {

    private final MetaClass enclosingType;
    private final HasAnnotations annotated;

    public InjectionSite(final MetaClass enclosingType, final HasAnnotations annotated) {
      this.enclosingType = enclosingType;
      this.annotated = annotated;
    }

    public MetaClass getEnclosingType() {
      return enclosingType;
    }

    @Override
    public Annotation[] getAnnotations() {
      return annotated.getAnnotations();
    }

    @Override
    public boolean isAnnotationPresent(final Class<? extends Annotation> annotation) {
      return annotated.isAnnotationPresent(annotation);
    }

    @Override
    public <A extends Annotation> A getAnnotation(final Class<A> annotation) {
      return annotated.getAnnotation(annotation);
    }

    public MetaClass getExactType() {
      if (annotated instanceof MetaField) {
        return ((MetaField) annotated).getType();
      } else if (annotated instanceof MetaParameter) {
        return ((MetaParameter) annotated).getType();
      } else {
        throw new RuntimeException("Not yet implemented for annotated of type " + annotated.getClass().getName());
      }
    }

  }

}
