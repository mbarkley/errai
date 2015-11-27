package org.jboss.errai.ui.test.stylebinding.client.res;

import javax.inject.Inject;

import org.jboss.errai.databinding.client.api.DataBinder;
import org.jboss.errai.ui.shared.api.annotations.AutoBound;
import org.jboss.errai.ui.shared.api.annotations.Bound;
import org.jboss.errai.ui.shared.api.annotations.DataField;
import org.jboss.errai.ui.shared.api.annotations.Templated;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;

import elemental.client.Browser;
import elemental.html.SpanElement;

/**
 * @author Mike Brock
 */
@Templated
public class StyleBoundTemplate extends Composite {

  @Inject @Bound @AdminBinding @DataField private Label testA;
  @Inject @Bound @TestBinding @DataField private Label testB;
  @Inject @Bound @ComponentBinding @DataField private CustomComponent testC;
  @AdminBinding @DataField private SpanElement elemental = Browser.getDocument().createSpanElement();
  @AdminBinding @DataField("user-element") private com.google.gwt.dom.client.SpanElement userElement = Document.get().createSpanElement();

  public Label getTestA() {
    return testA;
  }

  public Label getTestB() {
    return testB;
  }

  public CustomComponent getTestC() {
    return testC;
  }

  public SpanElement getElementalElement() {
    return elemental;
  }

  public com.google.gwt.dom.client.SpanElement getUserSpanElement() {
    return userElement;
  }

  public TestModel getTestModel() {
    return dataBinder.getModel();
  }

  @Inject @AutoBound DataBinder<TestModel> dataBinder;

  @TestBinding
  private void testBindingStyleUpdate(Style style) {
     if ("0".equals(getTestModel().getTestB())) {
       style.setVisibility(Style.Visibility.HIDDEN);
     }
     else {
       style.clearVisibility();
     }
  }

  @ComponentBinding
  private void testCustomComponentBindingStyleUpdate(Style style) {
    if ("0".equals(getTestModel().getTestC())) {
      style.setVisibility(Style.Visibility.HIDDEN);
    }
    else {
      style.clearVisibility();
    }
  }
}
