package org.jboss.errai.ioc.rebind.ioc.graph.api;

import java.lang.annotation.Annotation;

import javax.inject.Named;

/**
 * A single object for holding all the qualifier annotations of an injectable or
 * an injection point.
 *
 * @see QualifierFactory
 * @author Max Barkley <mbarkley@redhat.com>
 */
public interface Qualifier extends Iterable<Annotation> {

  /**
   * @param other Another qualifier to compare this to.
   * @return True if this qualifier is satsified by {@code other}.
   */
  boolean isSatisfiedBy(Qualifier other);

  /**
   * @return A unique string that can be used as part of a Java identifier.
   */
  String getIdentifierSafeString();

  /**
   * @return The value of {@link Named} if it is present.
   */
  String getName();

}
