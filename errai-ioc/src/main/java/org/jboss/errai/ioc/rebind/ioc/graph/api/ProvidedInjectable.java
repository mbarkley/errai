package org.jboss.errai.ioc.rebind.ioc.graph.api;

import java.lang.annotation.Annotation;

import org.jboss.errai.codegen.meta.HasAnnotations;
import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaClassMember;
import org.jboss.errai.codegen.meta.MetaField;
import org.jboss.errai.codegen.meta.MetaParameter;

/**
 * When an injectable is added using
 * {@link DependencyGraphBuilder#addExtensionInjectable(MetaClass, Qualifier, Class, org.jboss.errai.ioc.rebind.ioc.injector.api.WiringElementType...)}
 * each injection point it satisfies will have its own
 * {@link ProvidedInjectable}.
 *
 * @see Injectable
 * @author Max Barkley <mbarkley@redhat.com>
 */
public interface ProvidedInjectable extends Injectable {

  /**
   * @return Get the single injection site for this {@link ProvidedInjectable}.
   */
  public InjectionSite getInjectionSite();

  /**
   * Contains metadata for a single injection point.
   *
   * @author Max Barkley <mbarkley@redhat.com>
   */
  public static class InjectionSite implements HasAnnotations {

    private final MetaClass enclosingType;
    private final HasAnnotations annotated;

    public InjectionSite(final MetaClass enclosingType, final HasAnnotations annotated) {
      this.enclosingType = enclosingType;
      this.annotated = annotated;
    }

    private String annotatedName() {
      if (annotated instanceof MetaClassMember) {
        return ((MetaClassMember) annotated).getDeclaringClassName() + "_" + ((MetaClassMember) annotated).getName();
      } else if (annotated instanceof MetaParameter) {
        final MetaClassMember declaringMember = ((MetaParameter) annotated).getDeclaringMember();
        return declaringMember.getDeclaringClassName() + "_" + declaringMember.getName() + "_" + ((MetaParameter) annotated).getName();
      } else {
        throw new RuntimeException("Not yet implemented!");
      }
    }

    /**
     * @return A unique name for this injection site.
     */
    public String getUniqueName() {
      return enclosingType.getName() + "_" + annotatedName();
    }

    /**
     * @return The enclosing type for this injection site.
     */
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

    /**
     * @return The exact type of this injection site.
     */
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
