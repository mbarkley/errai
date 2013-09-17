package org.jboss.errai.ui.test.i18n.client;

import org.jboss.errai.enterprise.client.cdi.AbstractErraiCDITest;
import org.jboss.errai.ioc.client.container.IOC;
import org.jboss.errai.ui.client.local.spi.TranslationService;
import org.jboss.errai.ui.test.i18n.client.res.I18nAppScopeTestApp;
import org.jboss.errai.ui.test.i18n.client.res.I18nDepScopeTestApp;
import org.junit.Test;

/**
 * @author Max Barkley <mbarkley@redhat.com>
 */
public class I18nApplicationScopedTest extends AbstractErraiCDITest {

  @Override
  public String getModuleName() {
    return "org.jboss.errai.ui.test.i18n.Test";
  }
  
  @Override
  protected void gwtSetUp() throws Exception {
    super.gwtSetUp();
    TranslationService.setCurrentLocale("en_US");
  }

  /**
   * Test locale translation with a dependent scoped UI element in an AppScoped container.
   */
  @Test
  public void testDepScopeInAppScope() throws Exception {
    assertEquals("en_US", TranslationService.currentLocale());
    
    I18nAppScopeTestApp app1 = IOC.getBeanManager().lookupBean(I18nAppScopeTestApp.class).getInstance();
    
    assertEquals("Failed to load default text", "hello", app1.getWidget().getInlineLabelText());
    
    TranslationService.setCurrentLocale("fr_FR");
    
    assertEquals("Failed to translate application scoped widget", "bonjour", app1.getWidget().getInlineLabelText());
  }
  
  /**
   * Test locale translation with application scoped UI element within Dependent container.
   */
  @Test
  public void testAppScopeInDepScope() throws Exception {
    assertEquals("en_US", TranslationService.currentLocale());

    I18nDepScopeTestApp app1 = IOC.getBeanManager().lookupBean(I18nDepScopeTestApp.class).getInstance();

    assertEquals("Failed to load default text", "hello", app1.getWidget().getInlineLabelText());
    
    TranslationService.setCurrentLocale("fr_FR");
    
    assertEquals("Failed to translate application scoped widget", "bonjour", app1.getWidget().getInlineLabelText());
  }

}
