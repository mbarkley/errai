package org.jboss.errai.ioc.client.api;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jboss.errai.ioc.client.container.Context;

/**
 * This annotation is used to declare that an implementation of {@link Context}
 * should be used to handle some given set of scopes.
 *
 * At runtime, a bean will be assigned to the scope of the {@link Context}
 * implementation that contained the bean's scope annotation in the context's
 * {@link ScopeContext#value()}.
 *
 *@see Context
 * @author Max Barkley <mbarkley@redhat.com>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ScopeContext {

  /**
   * @return An array of scope annotations for which the annotated
   *         {@link Context} implementation is responsible.
   */
  Class<? extends Annotation>[] value();

}
