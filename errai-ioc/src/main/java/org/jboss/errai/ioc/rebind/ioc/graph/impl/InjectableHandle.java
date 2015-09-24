package org.jboss.errai.ioc.rebind.ioc.graph.impl;

import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.ioc.rebind.ioc.graph.api.Injectable;
import org.jboss.errai.ioc.rebind.ioc.graph.api.Qualifier;

/**
 * A handle for looking up {@link Injectable injectables}.
 *
 * @see Injectable
 * @author Max Barkley <mbarkley@redhat.com>
 */
public class InjectableHandle {
  final MetaClass type;

  final Qualifier qualifier;

  public InjectableHandle(final MetaClass type, final Qualifier qualifier) {
    this.type = type;
    this.qualifier = qualifier;
  }

  /**
   * @return The class of the injectable represented by this handle.
   */
  public MetaClass getType() {
    return type;
  }

  /**
   * @return The qualifier of the injectable represented by this handle.
   */
  public Qualifier getQualifier() {
    return qualifier;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof InjectableHandle))
      return false;

    final InjectableHandle other = (InjectableHandle) obj;
    return type.equals(other.type) && qualifier.equals(other.qualifier);
  }

  @Override
  public int hashCode() {
    return type.hashCode() ^ qualifier.hashCode();
  }

  @Override
  public String toString() {
    return "[AbstractInjectableHandle:" + type.getName() + "$" + qualifier.toString() + "]";
  }
}