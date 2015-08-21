package org.jboss.errai.ioc.client.container;

import com.google.gwt.core.client.js.JsType;

@JsType
public class JsTypeProvider<T> {

  // TODO JsInterop doesn't work when making these methods abstract
  public T getInstance() {
    return null;
  }
}
