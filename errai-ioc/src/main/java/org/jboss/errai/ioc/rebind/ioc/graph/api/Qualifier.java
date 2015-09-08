package org.jboss.errai.ioc.rebind.ioc.graph.api;

/**
 * @author Max Barkley <mbarkley@redhat.com>
 */
public interface Qualifier {

  boolean isSatisfiedBy(Qualifier other);

  String getIdentifierSafeString();

  String getName();

}
