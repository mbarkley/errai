package org.jboss.errai.marshalling.tests.res;

import org.jboss.errai.common.client.api.annotations.Portable;

@Portable
public class Child {
  private Parent parent;
  public void setParent(Parent parent) {
    this.parent = parent;
  }
  public Parent getParent() {
    return parent;
  }
}