package org.jboss.errai.ioc.rebind.ioc.graph.api;

import java.lang.annotation.Annotation;

/**
 * @author Max Barkley <mbarkley@redhat.com>
 */
public interface Qualifier extends Iterable<Annotation> {

  boolean isSatisfiedBy(Qualifier other);

  String getIdentifierSafeString();

  String getName();

}
