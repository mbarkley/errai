/*
 * Copyright (C) 2011 Red Hat, Inc. and/or its affiliates.
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

package org.jboss.errai.ui.test.standalone.client;

import org.jboss.errai.ui.test.standalone.client.res.StandaloneTemplated;

import com.google.gwt.dom.client.Element;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.DOM;

/**
 *
 * @author Max Barkley <mbarkley@redhat.com>
 */
public class StandaloneTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return getClass().getName().replaceAll("\\.client\\..*", ".Test");
  }

  public void testProcessStandaloneTemplate() throws Exception {
    final StandaloneTemplated templated = StandaloneTemplated.create();

    final Element renderer = DOM.createDiv();
    renderer.appendChild(templated.getRoot());
    final String renderedHtml = renderer.getInnerHTML();
    final String expectedHtml = "<div data-field=\"root\">\n"
                              + "  <h1>Title should be displayed</h1>\n"
                              + "  <a data-field=\"link\">Link text should be displayed</a>\n"
                              + "</div>";

    assertEquals(expectedHtml, renderedHtml.trim());
    assertTrue(templated.getLink().hasParentElement());
    assertEquals(templated.getRoot(), templated.getLink().getParentElement());
  }

}
