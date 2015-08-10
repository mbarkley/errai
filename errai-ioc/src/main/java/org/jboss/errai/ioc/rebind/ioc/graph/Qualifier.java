package org.jboss.errai.ioc.rebind.ioc.graph;

/**
 * @author Max Barkley <mbarkley@redhat.com>
 */
public interface Qualifier {

  boolean isSatisfiedBy(Qualifier other);

}
