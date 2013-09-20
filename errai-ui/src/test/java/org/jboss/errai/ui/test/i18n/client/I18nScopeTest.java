package org.jboss.errai.ui.test.i18n.client;

import org.jboss.errai.enterprise.client.cdi.AbstractErraiCDITest;
import org.jboss.errai.ioc.client.container.IOC;
import org.jboss.errai.ui.client.local.spi.TranslationService;
import org.jboss.errai.ui.test.i18n.client.res.AppScopedWidget;
import org.jboss.errai.ui.test.i18n.client.res.DepScopedWidget;
import org.jboss.errai.ui.test.i18n.client.res.I18nAppScopeTestApp;
import org.jboss.errai.ui.test.i18n.client.res.I18nDepInDepScopeTestApp;
import org.jboss.errai.ui.test.i18n.client.res.I18nDepScopeTestApp;
import org.junit.Test;

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

  @Test
  public void testDepScopeBeanNotInDom() throws Exception {
    assertEquals("en_us", TranslationService.currentLocale());

    DepScopedWidget depWidget = IOC.getBeanManager().lookupBean(DepScopedWidget.class).getInstance();
    
    assertTrue("This widget should not be attached to the DOM!", !depWidget.isAttached());
    
    TranslationService.setCurrentLocale("fr_fr");
    
    assertEquals("Failed to translate dependent unattached widget", "bonjour", depWidget.getInlineLabelText());
  }
  
  @Test
  public void testAppScopeBeanNotInDom() throws Exception {
    assertEquals("en_us", TranslationService.currentLocale());

    AppScopedWidget appWidget = IOC.getBeanManager().lookupBean(AppScopedWidget.class).getInstance();
    
    assertTrue("This widget should not be attached to the DOM!", !appWidget.isAttached());
    
    TranslationService.setCurrentLocale("fr_fr");
    
    assertEquals("Failed to translate dependent unattached widget", "bonjour", appWidget.getInlineLabelText());
  }

}
