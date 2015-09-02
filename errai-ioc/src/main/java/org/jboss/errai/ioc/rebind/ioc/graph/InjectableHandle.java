package org.jboss.errai.ioc.rebind.ioc.graph;

import org.jboss.errai.codegen.meta.MetaClass;

public class InjectableHandle {
  final MetaClass type;

  final Qualifier qualifier;

  public InjectableHandle(final MetaClass type, final Qualifier qualifier) {
    this.type = type;
    this.qualifier = qualifier;
  }

  public MetaClass getType() {
    return type;
  }

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