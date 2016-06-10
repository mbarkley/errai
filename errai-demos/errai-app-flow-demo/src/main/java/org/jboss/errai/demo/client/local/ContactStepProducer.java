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

import static org.jboss.errai.demo.client.local.Constants.CONTACT_FORM;
import static org.jboss.errai.demo.client.local.Constants.CONTACT_LIST;
import static org.jboss.errai.demo.client.local.Constants.CONTACT_LOADER;
import static org.jboss.errai.demo.client.local.Constants.CONTACT_PENDING;
import static org.jboss.errai.demo.client.local.Constants.CONTACT_SAVER;
import static org.jboss.errai.demo.client.local.Constants.CONTACT_UPDATER;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import javax.enterprise.context.Dependent;
import javax.enterprise.event.Event;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.errai.bus.client.api.ClientMessageBus;
import org.jboss.errai.common.client.api.Caller;
import org.jboss.errai.demo.client.shared.Contact;
import org.jboss.errai.demo.client.shared.ContactOperation;
import org.jboss.errai.demo.client.shared.ContactStorageService;
import org.jboss.errai.enterprise.client.jaxrs.api.ResponseCallback;
import org.jboss.errai.flow.api.Command;
import org.jboss.errai.flow.api.CrudOperation;
import org.jboss.errai.flow.api.Step;
import org.jboss.errai.flow.api.Unit;
import org.jboss.errai.flow.client.local.CDIStepFactory;
import org.jboss.errai.ui.nav.client.local.TransitionTo;

@Dependent
public class ContactStepProducer {

  @Inject
  private Caller<ContactStorageService> contactService;

  @Inject
  private ClientMessageBus bus;

  @Inject
  private CDIStepFactory stepFactory;

  @Inject
  private TransitionTo<ContactListPage> listTransition;

  @Inject
  private TransitionTo<PendingPage> pendingTransition;

  @Inject
  @Named("ContactModal")
  private Event<Boolean> display;

  @Produces
  @Named(CONTACT_LOADER)
  public Step<Unit, List<Contact>> createLoader() {
    return new Step<Unit, List<Contact>>() {

      @Override
      public void execute(final Unit input, final Consumer<List<Contact>> callback) {
        contactService
          .call((final List<Contact> list) -> callback.accept(list))
          .getAllContacts();
      }

      @Override
      public String getName() {
        return CONTACT_LOADER;
      }
    };
  }

  @Produces
  @Named(CONTACT_SAVER)
  public Step<Contact, Unit> createSaver() {
    return new Step<Contact, Unit>() {

      @Override
      public void execute(final Contact input, final Consumer<Unit> callback) {
        contactService
          .call((ResponseCallback) o -> callback.accept(Unit.INSTANCE))
          .create(new ContactOperation(input, getQueueSessionId()));
      }

      @Override
      public String getName() {
        return CONTACT_SAVER;
      }
    };
  }

  @Produces
  @Named(CONTACT_UPDATER)
  public Step<Contact, Unit> createUpdater() {
    return new Step<Contact, Unit>() {

      @Override
      public void execute(final Contact input, final Consumer<Unit> callback) {
        contactService
          .call(o -> callback.accept(Unit.INSTANCE))
          .update(new ContactOperation(input, getQueueSessionId()));
      }

      @Override
      public String getName() {
        return CONTACT_UPDATER;
      }
    };
  }

  @Produces
  @Named(CONTACT_LIST)
  public Step<List<Contact>, Command<CrudOperation, Contact>> createList() {
    /*
     * For now this has no closing action. Eventually we will want the concept of an "addidtive" step that
     * prevents the previous step from closing until it is finished.
     */
    return stepFactory.createCdiStep(() -> listTransition.go(), () -> {}, CONTACT_LIST);
  }

  @Produces
  @Named(CONTACT_FORM)
  public Step<Contact, Optional<Contact>> createForm() {
    return stepFactory.createCdiStep(() -> display.fire(true), () -> display.fire(false), CONTACT_FORM);
  }

  @Produces
  @Named(CONTACT_PENDING)
  public Step<Unit,Unit> createPending() {
    return new Step<Unit, Unit>() {

      @Override
      public void execute(final Unit input, final Consumer<Unit> callback) {
        pendingTransition.go();
        callback.accept(Unit.INSTANCE);
      }

      @Override
      public String getName() {
        return CONTACT_PENDING;
      }
    };
  }

  protected String getQueueSessionId() {
    return bus.getSessionId();
  }

}
