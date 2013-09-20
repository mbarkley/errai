package org.jboss.errai.ui.test.i18n.client;

import org.jboss.errai.enterprise.client.cdi.AbstractErraiCDITest;
import org.jboss.errai.ioc.client.container.IOC;
import org.jboss.errai.ui.client.local.spi.TranslationService;
import org.jboss.errai.ui.test.i18n.client.res.AppScopedWidget;
import org.jboss.errai.ui.test.i18n.client.res.I18nAppScopeTestApp;
import org.jboss.errai.ui.test.i18n.client.res.I18nDepInDepScopeTestApp;
import org.jboss.errai.ui.test.i18n.client.res.I18nDepScopeTestApp;
import org.jboss.errai.ui.test.i18n.client.res.TemplatedParent;
import org.junit.Test;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * Test that templated beans of different scopes are re-translated when the locale is manually
 * changed.
 * 
 * @author Max Barkley <mbarkley@redhat.com>
 */
public class I18nScopeTest extends AbstractErraiCDITest {

  @Override
  public String getModuleName() {
    return "org.jboss.errai.ui.test.i18n.Test";
  }

  @Override
  protected void gwtSetUp() throws Exception {
    super.gwtSetUp();
    TranslationService.setCurrentLocaleWithoutUpdate("en_us");
  }

  @Override
  protected void gwtTearDown() throws Exception {
    super.gwtTearDown();
    TranslationService.setCurrentLocaleWithoutUpdate("en_us");
  }

  /**
   * Test locale translation with a dependent scoped UI element in an AppScoped container.
   */
  @Test
  public void testDepScopeInAppScope() throws Exception {
    assertEquals("en_us", TranslationService.currentLocale());

    I18nAppScopeTestApp app1 = IOC.getBeanManager().lookupBean(I18nAppScopeTestApp.class).getInstance();

    assertEquals("Failed to load default text", "hello", app1.getWidget().getInlineLabelText());
    assertTrue("Widget must be attached to DOM", app1.getWidget().isAttached());

    TranslationService.setCurrentLocale("fr_fr");

    assertEquals("Failed to translate application scoped widget", "bonjour", app1.getWidget().getInlineLabelText());
  }

  /**
   * Test locale translation with application scoped UI element within Dependent container.
   */
  @Test
  public void testAppScopeInDepScope() throws Exception {
    assertEquals("en_us", TranslationService.currentLocale());

    I18nDepScopeTestApp app1 = IOC.getBeanManager().lookupBean(I18nDepScopeTestApp.class).getInstance();

    assertEquals("Failed to load default text", "hello", app1.getWidget().getInlineLabelText());
    assertTrue("Widget must be attached to DOM", app1.getWidget().isAttached());

    TranslationService.setCurrentLocale("fr_fr");

    assertEquals("Failed to translate application scoped widget", "bonjour", app1.getWidget().getInlineLabelText());
  }

  /**
   * Test that dependent scoped beans will be translated after manual locale change.
   */
  @Test
  public void testDepScopeTest() throws Exception {
    assertEquals("en_us", TranslationService.currentLocale());

    I18nDepInDepScopeTestApp app1 = IOC.getBeanManager().lookupBean(I18nDepInDepScopeTestApp.class).getInstance();

    assertEquals("Failed to load default text", "hello", app1.getWidget().getInlineLabelText());
    assertTrue("Widget must be attached to DOM", app1.getWidget().isAttached());

    TranslationService.setCurrentLocale("fr_fr");

    assertEquals("Failed to translate depdendent scoped widget", "bonjour", app1.getWidget().getInlineLabelText());
  }

  /**
   * Test that newly created Dependent scoped beans will be translated after manual locale change.
   */
  @Test
  public void testDepScopeTestReplacement() throws Exception {
    assertEquals("en_us", TranslationService.currentLocale());

    I18nDepInDepScopeTestApp app1 = IOC.getBeanManager().lookupBean(I18nDepInDepScopeTestApp.class).getInstance();

    assertEquals("Failed to load default text", "hello", app1.getWidget().getInlineLabelText());

    TranslationService.setCurrentLocale("fr_fr");

    I18nDepInDepScopeTestApp app2 = IOC.getBeanManager().lookupBean(I18nDepInDepScopeTestApp.class).getInstance();

    assertEquals("Failed to translate depdendent scoped widget", "bonjour", app2.getWidget().getInlineLabelText());
  }

  // @Test
  // public void testDepScopeBeanNotInDom() throws Exception {
  // assertEquals("en_us", TranslationService.currentLocale());
  //
  // DepScopedWidget depWidget =
  // IOC.getBeanManager().lookupBean(DepScopedWidget.class).getInstance();
  //
  // assertTrue("This widget should not be attached to the DOM!", !depWidget.isAttached());
  //
  // TranslationService.setCurrentLocale("fr_fr");
  //
  // RootPanel.get().add(depWidget);
  //
  // assertEquals("Failed to translate dependent unattached widget", "bonjour",
  // depWidget.getInlineLabelText());
  // }

  @Test
  public void testAppScopeBeanNotInDom() throws Exception {
    assertEquals("en_us", TranslationService.currentLocale());

    AppScopedWidget appWidget = IOC.getBeanManager().lookupBean(AppScopedWidget.class).getInstance();

    assertTrue("This widget should not be attached to the DOM!", !appWidget.isAttached());

    TranslationService.setCurrentLocale("fr_fr");

    RootPanel.get().add(appWidget);

    assertEquals("Failed to translate dependent unattached widget", "bonjour", appWidget.getInlineLabelText());
  }

  /**
   * Make sure that re-translation does not clobber overridden parts of template.
   */
  @Test
  public void testTemplatedInTemplated() throws Exception {
    assertEquals("en_us", TranslationService.currentLocale());

    TemplatedParent parent = IOC.getBeanManager().lookupBean(TemplatedParent.class).getInstance();

    RootPanel.get().add(parent);
    assertTrue("This widget should be attached to the DOM", parent.isAttached());

    TranslationService.setCurrentLocale("fr_fr");

    // Check values through DOM
    Element element = parent.getElement();
    assertEquals("Parent template leaf element was not properly translated", "bonjour", element.getFirstChildElement()
            .getInnerText());
    assertEquals("Non-keyed child template was not translated", "bonjour", element.getFirstChildElement()
            .getNextSiblingElement().getInnerText());
    assertEquals("Keyed child template was not translated", "bonjour",
            element.getFirstChildElement().getNextSiblingElement().getNextSiblingElement().getInnerText());

    // Check values through widgets
    assertEquals("Parent template leaf element was not properly translated", "bonjour", parent.greeting.getInnerText());
    assertEquals("Non-keyed child template was not translated", "bonjour",
            parent.templatedChildNoI18nKey.greeting.getInnerText());
    assertEquals("Keyed child template was not translated", "bonjour",
            parent.templatedChildWithI18nKey.greeting.getInnerText());
  }

}
