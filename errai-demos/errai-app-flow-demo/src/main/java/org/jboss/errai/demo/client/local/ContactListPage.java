/**
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

import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.jboss.errai.common.client.dom.Anchor;
import org.jboss.errai.common.client.dom.DOMUtil;
import org.jboss.errai.databinding.client.api.DataBinder;
import org.jboss.errai.databinding.client.api.StateSync;
import org.jboss.errai.databinding.client.components.ListComponent;
import org.jboss.errai.demo.client.shared.Contact;
import org.jboss.errai.flow.api.Command;
import org.jboss.errai.flow.api.CrudOperation;
import org.jboss.errai.flow.cdi.api.FlowInput;
import org.jboss.errai.flow.cdi.api.FlowOutput;
import org.jboss.errai.ui.nav.client.local.NavigationPanel;
import org.jboss.errai.ui.nav.client.local.Page;
import org.jboss.errai.ui.nav.client.local.PageHiding;
import org.jboss.errai.ui.nav.client.local.PageShowing;
import org.jboss.errai.ui.nav.client.local.PageShown;
import org.jboss.errai.ui.shared.api.annotations.AutoBound;
import org.jboss.errai.ui.shared.api.annotations.Bound;
import org.jboss.errai.ui.shared.api.annotations.DataField;
import org.jboss.errai.ui.shared.api.annotations.EventHandler;
import org.jboss.errai.ui.shared.api.annotations.SinkNative;
import org.jboss.errai.ui.shared.api.annotations.Templated;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.Event;

/**
 * <p>
 * An Errai UI component for creating, displaying, updating, and deleting {@link Contact Contacts}. This component is
 * also an Errai Navigation {@link Page}; it will be displayed on the GWT host page whenever the navigation URL fragment
 * is {@code #/contacts}.
 *
 * <p>
 * The HTML markup for this {@link Templated} component is the HTML element with the CSS class {@code contact-list} in
 * the file {@code contact-page.html} in this package. This component uses CSS from the file {@code contact-page.css} in
 * this package.
 *
 * <p>
 * The {@link DataField} annotation marks fields that replace HTML elements from the template file. As an example, the
 * field {@link ContactDisplay#editor} replaces the {@code <div>} element in the template with the CSS class
 * {@code modal-fields}. Because {@link ContactEditor} is an Errai UI component, the markup for {@link ContactEditor}
 * will replace the contents of the {@code modal-fields} div in this component.
 *
 * <p>
 * This component uses a {@link ListComponent} to display a list of {@link Contact Contacts}. The {@code List<Contact>}
 * returned by calling {@link DataBinder#getModel()} on {@link #binder} is a model bound to a table of
 * {@link ContactDisplay ContactDisplays} in an HTML table. Any changes to the model list (such as adding or removing
 * items) will be automatically reflected in the displayed table.
 *
 * <p>
 * Instances of this type should be obtained via Errai IoC, either by using {@link Inject} in another container managed
 * bean, or by programmatic lookup through the bean manager.
 */
@Page(path = "/contacts")
@Templated(value = "contact-page.html#contact-list", stylesheet = "contact-page.css")
public class ContactListPage {

  @Inject
  @AutoBound
  private DataBinder<List<Contact>> binder;

  @Inject
  @Bound
  @DataField
  private ListComponent<Contact, ContactDisplay> list;

  @Inject
  @DataField
  private ContactModal modal;

  @Inject
  private NavBar navbar;

  @Inject
  private Anchor newContactAnchor;

  @Inject
  private Anchor sortContactsAnchor;

  @Inject
  private FlowInput<List<Contact>> input;

  @Inject
  private FlowOutput<Command<CrudOperation, Contact>> output;

  /**
   * Register handlers and populate the list of {@link Contact Contacts}.
   */
  @PostConstruct
  private void setup() {
    // Remove placeholder table row from template.
    DOMUtil.removeAllElementChildren(list.getElement());

    /*
     * Configure actions for when a ContactDisplay in the list is selected or deselected.
     */
    list.setSelector(display -> display.setSelected(true));
    list.setDeselector(display -> display.setSelected(false));

    /*
     * Setup anchors that are added to the nav bar when the page is shown.
     */
    newContactAnchor.setHref("javascript:void(0);");
    newContactAnchor.setTextContent("Create Contact");
    newContactAnchor.setOnclick(e -> onNewContactClick(null));

    sortContactsAnchor.setHref("javascript:");
    sortContactsAnchor.setTextContent("Sort By Nickname");
    sortContactsAnchor.setOnclick(e -> sortContactsByName());
  }

  @PageShowing
  private void updateContactList() {
    binder.getModel().addAll(input.get());
  }

  /**
   * This method is invoked when this {@link Page} is attached to the {@link NavigationPanel}.
   */
  @PageShown
  public void addNavBarButtons() {
    navbar.add(newContactAnchor);
    navbar.add(sortContactsAnchor);
  }

  /**
   * This method is invoked when this {@link Page} is being removed from the {@link NavigationPanel}.
   */
  @PageHiding
  public void removeNavBarButtons() {
    navbar.remove(newContactAnchor);
    navbar.remove(sortContactsAnchor);
  }

  /**
   * This is an Errai UI native event handler. The element for which this handler is regsitered is in this class's HTML
   * template file and has the id {@code new-content}.
   * <p>
   * Because there is no {@code new-content} {@link DataField} in this class, this method's parameter is a non-specific
   * {@link Event} (rather than a more specific {@link ClickEvent}). For the same reason, the {@link SinkNative}
   * annotation is required to specify which kinds of DOM events this method should handle.
   * <p>
   * This method displays the hidden modal form so that a user can create a new {@link Contact}.
   */
  @SinkNative(Event.ONCLICK)
  @EventHandler("new-contact")
  public void onNewContactClick(final Event event) {
    output.submit(new Command<>(CrudOperation.CREATE, new Contact()));
  }

  /**
   * Observes local CDI events fired from
   * {@link ContactDisplay#onDoubleClick(com.google.gwt.event.dom.client.DoubleClickEvent)}, in order to display the
   * modal form for editting a contact.
   */
  public void editComponent(final @Observes @DoubleClick ContactDisplay component) {
    selectComponent(component);
    output.submit(new Command<>(CrudOperation.UPDATE, component.getValue()));
  }

  /**
   * This method observes CDI events fired locally by {@link ContactDisplay#onClick(ClickEvent)} in order to highlight a
   * {@link ContactDisplay} when it is clicked.
   */
  public void selectComponent(final @Observes @Click ContactDisplay component) {
    if (list.getSelectedComponents().contains(component)) {
      list.deselectAll();
    }
    else {
      list.deselectAll();
      list.selectComponent(component);
    }
  }

  private void sortContactsByName() {
    binder.pause();
    Collections.sort(binder.getModel(), (a,b) -> a.getNickname().compareTo(b.getNickname()));
    binder.resume(StateSync.FROM_MODEL);
  }
}
