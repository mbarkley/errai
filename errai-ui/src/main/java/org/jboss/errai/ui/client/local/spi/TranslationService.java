/*
 * Copyright 2012 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.errai.ui.client.local.spi;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Logger;

import org.jboss.errai.ui.shared.DomRevisitor;
import org.jboss.errai.ui.shared.DomVisit;
import org.jboss.errai.ui.shared.JSONMap;
import org.jboss.errai.ui.shared.TemplateTranslationVisitor;
import org.jboss.errai.ui.shared.api.annotations.Templated;
import org.jboss.errai.ui.shared.wrapper.ElementWrapper;
import org.w3c.dom.Element;

import com.google.gwt.dom.client.Document;

/**
 * A base class for a generated translation service that includes all
 * of the translation visible at compile time.
 *
 * @author eric.wittmann@redhat.com
 */
public abstract class TranslationService {

  private static final Logger logger = Logger.getLogger(TranslationService.class.getName());
  private static String currentLocale = null;

  private Dictionary dictionary = new Dictionary();
  /**
   * @return true if the translation service is enabled/should be used
   */
  public boolean isEnabled() {
    return !dictionary.getSupportedLocals().isEmpty();
  }

  public Collection<String> getSupportedLocales() {
    return dictionary.getSupportedLocals();
  }
  
  /**
   * Registers the bundle with the translation service.
   * @param jsonData
   */
  protected void registerBundle(String jsonData, String locale) {
    JSONMap data = JSONMap.create(jsonData);
    register(data, locale);
  }

  /**
   * Registers some i18n data with the translation service.  This is called
   * for each discovered bundle file.
   * @param data
   * @param locale
   */
  protected void register(JSONMap data, String locale) {
    if (locale != null) {
      locale = locale.toLowerCase();
    }
    logger.fine("Registering translation data for locale: " + locale);
    Set<String> keys = data.keys();
    for (String key : keys) {
      String value = data.get(key);
      dictionary.put(locale, key, value);
    }
    logger.fine("Registered " + keys.size() + " translation keys.");
  }

  /**
   * Gets the translation for the given i18n translation key.
   * @param translationKey
   */
  public String getTranslation(String translationKey) {
    String localeName = getActiveLocale();
    logger.fine("Translating key: " + translationKey + "  into locale: " + localeName);
    Map<String, String> translationData = dictionary.get(localeName);
    if (translationData.containsKey(translationKey)) {
      logger.fine("Translation found in locale map: " + localeName);
      return translationData.get(translationKey);
    }
    // Nothing?  Then return null.
    logger.fine("Translation not found in any locale map, leaving unchanged.");
    return null;
  }

  public String getActiveLocale() {
    String localeName = currentLocale();
    if (!dictionary.get(localeName).isEmpty()) {
      return localeName;
    }
    if (localeName != null && localeName.contains("_")
            && !dictionary.get(localeName.substring(0, localeName.indexOf('_'))).isEmpty()) {
      return localeName.substring(0, localeName.indexOf('_'));
    }
    return null;
  }

  /**
   * @return the currently configured locale
   */
  public static String currentLocale() {
    if (currentLocale == null) {
      String locale = com.google.gwt.user.client.Window.Location.getParameter("locale");
      if (locale == null || locale.trim().length() == 0) {
        locale = getBrowserLocale();
        if (locale != null) {
          if (locale.indexOf('-') != -1) {
            locale = locale.replace('-', '_');
          }
        }
      }
      if (locale == null) {
        locale = "default";
      }
      currentLocale = locale.toLowerCase();
      logger.fine("Discovered the current locale (either via query string or navigator) of: " + currentLocale);
    }
    return currentLocale;
  }

  /**
   * Gets the browser's configured locale.
   */
  public final static native String getBrowserLocale() /*-{
    if ($wnd.navigator.language) {
      return $wnd.navigator.language;
    }
    if ($wnd.navigator.userLanguage) {
      return $wnd.navigator.userLanguage;
    }
    if ($wnd.navigator.browserLanguage) {
      return $wnd.navigator.browserLanguage;
    }
    if ($wnd.navigator.systemLanguage) {
      return $wnd.navigator.systemLanguage;
    }
    return null;
  }-*/;

  /**
   * Forcibly set the current locale and re-translate all instantiated {@link Templated} beans.
   * 
   * @param locale
   */
  public final static void setCurrentLocale(String locale) {
    setCurrentLocaleWithoutUpdate(locale);
    retranslateTemplatedBeans();
  }
  
  /**
   * Forcibly set the current locale but do not re-translate existing templated instances. Mostly
   * useful for testing.
   * 
   * @param locale
   */
  public final static void setCurrentLocaleWithoutUpdate(String locale) {
    currentLocale = locale;
  }

  /**
   * Re-translate displayed {@link Templated} beans to the current locale.
   */
  public static void retranslateTemplatedBeans() {
    DomVisit.revisit(new ElementWrapper(Document.get().getBody()), new DomRevisitor() {
      /*
       * Outline:
       * 
       * Root nodes of templates are marked with 'data-i18n-prefix' attribute, allowing lookup. Use
       * a stack to keep track of which template we are in. After visiting, if the top of the stack
       * matches an element prefix attribute, we are leaving that template.
       */

      private TemplateTranslationVisitor visitor = new TemplateTranslationVisitor("");
      private final Stack<String> prefixes = new Stack<String>();
      private static final String PREFIX = "data-i18n-prefix";

      @Override
      public boolean visit(Element element) {
        if (visitor.hasAttribute(element, PREFIX))
          prefixes.push(element.getAttribute(PREFIX));

        if (prefixes.empty())
          return !visitor.isTextOnly(element);

        visitor.setI18nPrefix(prefixes.peek());
        return visitor.visit(element);
      }

      @Override
      public void afterVisit(Element element) {
        if (visitor.hasAttribute(element, PREFIX) && element.getAttribute(PREFIX).equals(prefixes.peek()))
          prefixes.pop();
      }
    });
  }

}
