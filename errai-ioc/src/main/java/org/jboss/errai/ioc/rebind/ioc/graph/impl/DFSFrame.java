package org.jboss.errai.ioc.rebind.ioc.graph.impl;

class DFSFrame {
  final ConcreteInjectable concrete;
  int dependencyIndex = -1;

  DFSFrame(final ConcreteInjectable concrete) {
    this.concrete = concrete;
  }

  @Override
  public int hashCode() {
    return concrete.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof DFSFrame) {
      return concrete.equals(((DFSFrame) obj).concrete);
    } else {
      return false;
    }
  }

  @Override
  public String toString() {
    return "<concrete=" + concrete.toString() + ", index=" + dependencyIndex + ">";
  }
}