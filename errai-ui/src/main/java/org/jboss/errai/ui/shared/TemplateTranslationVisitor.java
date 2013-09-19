package org.jboss.errai.ui.shared;

import org.jboss.errai.ui.shared.wrapper.ElementWrapper;

public class TemplateTranslationVisitor extends TemplateVisitor {

  public TemplateTranslationVisitor(String i18nPrefix) {
    super(i18nPrefix);
  }

  @Override
  protected void visitElement(String i18nKeyPrefix, org.w3c.dom.Element element) {
    String translationKey = i18nKeyPrefix + getOrGenerateTranslationKey(element);
    String translationValue = getI18nValue(translationKey);
    if (translationValue != null)
      ((ElementWrapper) element).getElement().setInnerHTML(translationValue);
  }

  @Override
  protected void visitAttribute(String i18nKeyPrefix, org.w3c.dom.Element element, String attributeName) {
    String translationKey = i18nKeyPrefix + getElementKey(element);
    translationKey += "-" + attributeName;
    String translationValue = getI18nValue(translationKey);
    if (translationValue != null)
      element.setAttribute(attributeName, translationValue);
  }

  private String getI18nValue(String translationKey) {
    return TemplateUtil.getTranslationService().getTranslation(translationKey);
  }
}
