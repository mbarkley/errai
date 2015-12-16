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

package org.jboss.errai.ui.test.standalone.client.res;

import org.jboss.errai.ui.client.local.api.TemplateProcessor;
import org.jboss.errai.ui.shared.api.annotations.DataField;
import org.jboss.errai.ui.shared.api.annotations.Templated;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.DOM;

/**
 *
 * @author Max Barkley <mbarkley@redhat.com>
 */
@Templated
public class StandaloneTemplated {

  public static abstract class StandaloneTemplatedProcessor implements TemplateProcessor<StandaloneTemplated> {
  }

  public static StandaloneTemplated create() {
    final StandaloneTemplatedProcessor processor = GWT.create(StandaloneTemplatedProcessor.class);
    final StandaloneTemplated templated = new StandaloneTemplated();

    return processor.process(templated);
  }

  @DataField
  private final Element root = DOM.createDiv();

  @DataField
  private final Element link = DOM.createAnchor();

  public Element getRoot() {
    return root;
  }

  public Element getLink() {
    return link;
  }

}
