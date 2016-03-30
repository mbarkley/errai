package org.jboss.errai.marshalling.tests.res;

import org.jboss.errai.common.client.api.annotations.MapsTo;
import org.jboss.errai.common.client.api.annotations.Portable;

@Portable
public class Parent {
  private Child child;
  public Parent(@MapsTo("child") Child child) {
    this.child = child;
  }
  public Child getChild() {
    return child;
  }
}