package org.jboss.errai.ui.test.i18n.client.res;

import org.jboss.errai.ui.shared.api.annotations.Bundle;
import org.jboss.errai.ui.shared.api.annotations.DataField;
import org.jboss.errai.ui.shared.api.annotations.Templated;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Composite;

@Templated("BasicTemplate.html#greeting")
@Bundle("BasicTemplate.json")
public class TemplatedChild extends Composite {
  
  @DataField
  public Element greeting = DOM.createDiv();

}
