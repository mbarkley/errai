package org.jboss.errai.ui.test.element.client;

import static elemental.client.Browser.getDocument;

import org.jboss.errai.enterprise.client.cdi.AbstractErraiCDITest;
import org.jboss.errai.ioc.client.container.IOC;
import org.junit.Test;

import com.google.gwt.dom.client.Element;

import elemental.dom.Document;
import elemental.events.EventTarget;
import elemental.events.MouseEvent;

public class ElementTemplateTest extends AbstractErraiCDITest {

  @Override
  public String getModuleName() {
    return getClass().getName().replaceAll("client.*$", "Test");
  }

  @Test
  public void testUseElementDirectly() {
    ElementTemplateTestApp app = IOC.getBeanManager().lookupBean(ElementTemplateTestApp.class).getInstance();

    Element form = app.getForm().getElement();
    assertTrue(form.getInnerHTML().contains("Keep me logged in on this computer"));
    assertTrue(app.getForm().getForm().getInnerHTML().contains("Keep me logged in on this computer"));
    assertTrue(app.getForm().getCancel().getTextContent().equals("Cancel"));

    assertEquals(0, app.getForm().getNumberOfTimesPressed());
    click(app.getForm().getCancel());
    assertEquals(1, app.getForm().getNumberOfTimesPressed());
  }

  /**
   * Fires a left-click event on the given target (typically a DOM node).
   */
  public static void click(EventTarget target) {
    MouseEvent evt = (MouseEvent) getDocument().createEvent(
        Document.Events.MOUSE);
    evt.initMouseEvent("click", true, true, null, 0, 0, 0, 0, 0, false, false,
        false, false, MouseEvent.Button.PRIMARY, null);
    target.dispatchEvent(evt);
  }

}