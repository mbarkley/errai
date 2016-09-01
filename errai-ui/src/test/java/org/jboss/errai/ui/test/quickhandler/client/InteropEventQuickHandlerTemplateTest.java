/*
 * Copyright (C) 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.errai.ui.test.quickhandler.client;

import java.util.Arrays;
import java.util.Collections;

import org.jboss.errai.common.client.dom.HTMLElement;
import org.jboss.errai.common.client.ui.ElementWrapperWidget;
import org.jboss.errai.enterprise.client.cdi.AbstractErraiCDITest;
import org.jboss.errai.ioc.client.IOCUtil;
import org.jboss.errai.ui.test.quickhandler.client.res.InteropEventQuickHandlerTemplate;
import org.jboss.errai.ui.test.quickhandler.client.res.InteropEventQuickHandlerTemplate.ObservedEvent;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.user.client.ui.HasValue;

public class InteropEventQuickHandlerTemplateTest extends AbstractErraiCDITest {

  private InteropEventQuickHandlerTemplate bean;

  @Override
  public String getModuleName() {
    return getClass().getName().replaceAll("client.*$", "Test");
  }

  @Override
  protected void gwtSetUp() throws Exception {
    super.gwtSetUp();
    setupAddEventListenerInterceptor();
    bean = IOCUtil.getInstance(InteropEventQuickHandlerTemplate.class);
  }

  public void testButtonSingleClickHandler() throws Exception {
    assertTrue(bean.observed.isEmpty());
    invokeEventListeners(bean.button, "click");
    assertEquals(Arrays.asList(new ObservedEvent("button", "click")), bean.observed);
    invokeEventListeners(bean.button, "dblclick");
    assertEquals(Arrays.asList(new ObservedEvent("button", "click")), bean.observed);
  }

  public void testAnchorSingleAndDoubleClick() throws Exception {
    assertTrue(bean.observed.isEmpty());
    invokeEventListeners(bean.anchor, "click");
    assertEquals(Arrays.asList(new ObservedEvent("anchor", "click")), bean.observed);
    invokeEventListeners(bean.anchor, "dblclick");
    assertEquals(Arrays.asList(new ObservedEvent("anchor", "click"), new ObservedEvent("anchor", "dblclick")), bean.observed);
  }

  public void testInputChange() throws Exception {
    assertTrue(bean.observed.isEmpty());
    invokeEventListeners(bean.input, "change");
    assertEquals(Arrays.asList(new ObservedEvent("input", "change")), bean.observed);
    invokeEventListeners(bean.input, "click");
    assertEquals(Arrays.asList(new ObservedEvent("input", "change")), bean.observed);
  }

  public void testListenersRemovedAfterBeanDestroyed() throws Exception {
    try {
      assertTrue(bean.observed.isEmpty());
      invokeEventListeners(bean.button, "click");
      assertEquals(Arrays.asList(new ObservedEvent("button", "click")), bean.observed);
    } catch (final AssertionError ae) {
      throw new AssertionError("Precondition failed.", ae);
    }

    bean.observed.clear();
    assertTrue(bean.observed.isEmpty());
    IOCUtil.destroy(bean);
    invokeEventListeners(bean.button, "click");
    assertEquals(Collections.emptyList(), bean.observed);
  }

  /*
   * This is a really disgusting workaround for the inability to
   * dispatch native browser events in the version of HtmlUnit currently
   * bundled in gwt-dev.
   *
   * What does this do?
   * This replaces "addEventListener" and "removeEventListener"
   * in the HTMLElement prototype with functions that intercept
   * and store registered listeners.
   *
   * Why does it do it?
   * So that subsequent calls to "invokeEventListeners" can
   * manually call any functions added with "addEventListener".
   *
   * In short because we cannot dispatch browser events, to test
   * binding of native elements we must store and then manually invoke
   * all event listeners.
   */
  private static native void setupAddEventListenerInterceptor() /*-{
    console.log("Setting up event listener interceptors.");
    function ListenerMap() {
      var map = new Object();

      this.add = function(element, type, listener) {
        this.get(element, type).push(listener);
      };

      this.remove = function(element, type, listener) {
        var listeners = this.get(element, type);
        var index = listeners.indexOf(listener);
        if (index > -1) {
          listeners.splice(index, 1);
        }
      };

      this.get = function(element, type) {
        if (map[element] === undefined) {
          map[element] = {};
        }
        if (map[element][type] === undefined) {
          map[element][type] = new Array();
        }
        return map[element][type];
      };
    };
    if ($wnd.HTMLElement.prototype._addEventListener === undefined) {
      listeners = new ListenerMap();
      $wnd.HTMLElement.prototype._addEventListener = $wnd.HTMLElement.prototype.addEventListener;
      $wnd.HTMLElement.prototype._removeEventListener = $wnd.HTMLElement.prototype.removeEventListener;
      console.log("Replacing addEventListener.");
      $wnd.HTMLElement.prototype.addEventListener = function(type, listener, capture) {
        console.log("Intercepted addEventListener for " + this + " with type " + type);
        listeners.add(this, type, listener);
        this._addEventListener(type, listener, capture);
      };
      console.log("Replacing removeEventListener.");
      $wnd.HTMLElement.prototype.removeEventListener = function(type, listener, capture) {
        console.log("Intercepted removeEventListener for " + this + " with type " + type);
        listeners.remove(this, type, listener);
        this._removeEventListener(type, listener, capture);
      };
    }
  }-*/;

  @SuppressWarnings("unchecked")
  private static void invokeEventListeners(final HTMLElement element, final String eventType) {
    invokeEventListeners((Object) element, eventType);
    if ("change".equals(eventType)) {
      @SuppressWarnings("rawtypes")
      final ElementWrapperWidget elem = ElementWrapperWidget.getWidget(element);
      if (elem instanceof HasValue) {
        ValueChangeEvent.fire(((HasValue) elem), ((HasValue) elem).getValue());
      }
    }
  }

  private static void invokeEventListeners(final Object element, final String eventType) {
    final NativeEvent event = Document.get().createHtmlEvent(eventType, true, true);
    invokeEventListeners(element, eventType, event);
  }

  private static native void invokeEventListeners(Object element, String type, Object evt) /*-{
    listeners.get(element, type).forEach(function(l) { l(evt); });
  }-*/;

}
