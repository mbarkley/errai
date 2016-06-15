/*
 * Copyright (C) 2016 Red Hat, Inc. and/or its affiliates.
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


package org.jboss.errai.demo.client.local;

import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.errai.common.client.dom.Button;
import org.jboss.errai.common.client.dom.DOMUtil;
import org.jboss.errai.common.client.dom.Form;
import org.jboss.errai.demo.client.shared.Contact;
import org.jboss.errai.flow.cdi.api.FlowInput;
import org.jboss.errai.flow.cdi.api.FlowOutput;
import org.jboss.errai.ui.shared.api.annotations.DataField;
import org.jboss.errai.ui.shared.api.annotations.EventHandler;
import org.jboss.errai.ui.shared.api.annotations.SinkNative;
import org.jboss.errai.ui.shared.api.annotations.Templated;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.TakesValue;


@Templated("contact-page.html#modal")
public class ContactModal implements TakesValue<Contact> {

  @Inject
  @DataField
  private Form modal;

  @Inject
  @DataField("modal-fields")
  private ContactEditor editor;

  @Inject
  @DataField("modal-delete")
  private Button delete;

  @Inject
  private FlowInput<Contact> input;

  @Inject
  private FlowOutput<Optional<Contact>> output;

  @PostConstruct
  private void init() {
    delete.getStyle().setProperty("display", "none");
  }

  public void handleDisplay(@Observes @Named("ContactModal") final Boolean display) {
    if (display) {
      editor.setValuePaused(input.get());
      DOMUtil.addCSSClass(modal, "displayed");
    }
    else {
      DOMUtil.removeCSSClass(modal, "displayed");
    }
  }

  /**
   * This is an Errai UI native event handler. The element for which this handler is regsitered is in this class's HTML
   * template file and has the {@code modal-submit} CSS class.
   * <p>
   * Because there is no {@code modal-submit} {@link DataField} in this class, this method's parameter is a non-specific
   * {@link Event} (rather than a more specific {@link ClickEvent}). For the same reason, the {@link SinkNative}
   * annotation is required to specify which kinds of DOM events this method should handle.
   * <p>
   * This method displays and persists changes made to a {@link Contact} in the {@link ContactEditor}, whether it is a
   * newly created or an previously existing {@link Contact}.
   */
  @SinkNative(Event.ONCLICK)
  @EventHandler("modal-submit")
  public void onModalSubmitClick(final Event event) {
    if (modal.checkValidity()) {
      editor.syncStateFromUI();
      output.submit(Optional.of(editor.getValue()));
    }
  }

  /**
   * This is an Errai UI native event handler. The element for which this handler is regsitered is in this class's HTML
   * template file and has the {@code modal-cancel} CSS class.
   * <p>
   * Because there is no {@code modal-cancel} {@link DataField} in this class, this method's parameter is a non-specific
   * {@link Event} (rather than a more specific {@link ClickEvent}). For the same reason, the {@link SinkNative}
   * annotation is required to specify which kinds of DOM events this method should handle.
   * <p>
   * This method hides the ContactEditor modal form and resets the bound model.
   */
  @SinkNative(Event.ONCLICK)
  @EventHandler("modal-cancel")
  public void onModalCancelClick(final Event event) {
    output.submit(Optional.empty());
  }

  @Override
  public void setValue(final Contact value) {
    editor.setValue(value);
  }

  @Override
  public Contact getValue() {
    return editor.getValue();
  }

  public void syncStateFromUI() {
    editor.syncStateFromUI();
  }

  public void display() {
    DOMUtil.addCSSClass(modal, "displayed");
  }

}
