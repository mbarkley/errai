/**
 * JBoss, Home of Professional Open Source
 * Copyright 2015, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.errai.ui.client.element;

import org.jboss.errai.ui.shared.TemplateWidget;

import com.google.gwt.dom.client.Element;

/**
 * @author Max Barkley <mbarkley@redhat.com>
 */
public abstract class AbstractTemplated {

  private Element element;
  private TemplateWidget widget;

  public Element getElement() {
    return element;
  }

  public void setElement(Element element) {
    this.element = element;
  }

  public void setWidget(TemplateWidget widget) {
    this.widget = widget;
  }

  public TemplateWidget getWidget() {
    return widget;
  }
}
