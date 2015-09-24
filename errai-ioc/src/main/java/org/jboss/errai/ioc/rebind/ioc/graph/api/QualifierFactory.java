package org.jboss.errai.ioc.rebind.ioc.graph.api;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.inject.Named;

import org.jboss.errai.codegen.meta.HasAnnotations;

/**
 * A factory for creating {@link Qualifier} instances for {@link Injectable
 * injectables} and injection points.
 *
 * Implementations should handle the special case rules for {@link Any},
 * {@link Default}, and {@link Named}.
 *
 * {@link Qualifier} implementations will only every be compared against other
 * implementations from the same factory.
 *
 * @author Max Barkley <mbarkley@redhat.com>
 */
public interface QualifierFactory {

  /**
   * Creates qualifier for an injectable like a concrete type or a producer
   * member.
   *
   * @param annotated
   *          An annotated object. Usually a concrete type or a producer member.
   * @return A qualifier for an injectable.
   */
  Qualifier forSource(HasAnnotations annotated);

  /**
   * Creates qualifier for an injection point, like a field or constructor
   * dependency.
   *
   * @param annotated
   *          An annotated object. Usually a field or parameter that is an
   *          injection point.
   * @return A qualifier for a dependency.
   */
  Qualifier forSink(HasAnnotations annotated);

  /**
   * @return The empty qualifier. This qualifier only satisfies itself, and is
   *         satisfied by all other qualifiers.
   */
  Qualifier forUnqualified();

  /**
   * @return The universal qualifier. This qualifier satisfies all other
   *         qualifiers, and is only satisfied by itself.
   */
  Qualifier forUniversallyQualified();

  /**
   * @param qualifier
   *          Must not be null.
   * @param qualifier2
   *          Must not be null.
   * @return A single qualifier containing all the annotations of both. This
   *         will satisfy both parameters but should not be satisfied by eithe
   *         parameter unless one parameter was the empty qualifier.
   */
  Qualifier combine(Qualifier qualifier, Qualifier qualifier2);

  /**
   * @return A convenience method for getting a qualifier for an object that has
   *         no explicit annotations. Because of the special case qualifiers,
   *         {@link Any} and {@link Default}, this is not equivalent to the
   *         return value of {@link #forUnqualified()}.
   */
  Qualifier forDefault();

}
