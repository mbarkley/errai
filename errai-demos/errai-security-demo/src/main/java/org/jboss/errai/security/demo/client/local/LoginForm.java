/**
 * JBoss, Home of Professional Open Source
 * Copyright 2014, Red Hat, Inc. and/or its affiliates, and individual
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
package org.jboss.errai.security.demo.client.local;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.jboss.errai.bus.client.api.BusErrorCallback;
import org.jboss.errai.bus.client.api.messaging.Message;
import org.jboss.errai.common.client.api.RemoteCallback;
import org.jboss.errai.security.client.local.context.SecurityContext;
import org.jboss.errai.security.client.local.identity.LoginBuilder;
import org.jboss.errai.security.shared.api.identity.User;
import org.jboss.errai.ui.nav.client.local.Page;
import org.jboss.errai.ui.nav.client.local.PageShowing;
import org.jboss.errai.ui.nav.client.local.TransitionTo;
import org.jboss.errai.ui.nav.client.local.api.LoginPage;
import org.jboss.errai.ui.shared.api.annotations.Bound;
import org.jboss.errai.ui.shared.api.annotations.DataField;
import org.jboss.errai.ui.shared.api.annotations.EventHandler;
import org.jboss.errai.ui.shared.api.annotations.Model;
import org.jboss.errai.ui.shared.api.annotations.Templated;
import org.slf4j.Logger;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;

@Page(role = LoginPage.class)
@Templated("#root")
@Dependent
public class LoginForm extends Composite {

  @Inject Logger logger;

  @Inject
  TransitionTo<WelcomePage> welcomePage;

  @Inject
  @Model
  LoginBuilder identity;

  @Inject
  private SecurityContext securityContext;

  @Inject
  @Bound
  @DataField
  private TextBox username;

  @DataField
  private final Element form = DOM.createDiv();

  @Inject
  @Bound
  @DataField
  private PasswordTextBox password;

  @Inject
  @DataField
  private Anchor login;

  @Inject
  @DataField
  private Anchor logout;

  @DataField
  Element alert = DOM.createDiv();

  @EventHandler("login")
  private void loginClicked(ClickEvent event) {
    identity.login(new RemoteCallback<User>() {

      @Override
      public void callback(final User response) {
        if (response != null) {
          securityContext.navigateBackOrHome();
        }
      }
    }, new BusErrorCallback() {
      @Override
      public boolean error(Message message, Throwable throwable) {
        logger.error("Login failure reason: ", throwable);
        alert.getStyle().setDisplay(Style.Display.BLOCK);
        return false;
      }
    });
  }

  @EventHandler("logout")
  private void logoutClicked(ClickEvent event) {
    identity.logout();
    welcomePage.go();
  }

  @PageShowing
  private void isLoggedIn() {
    identity.getUser(new RemoteCallback<User>() {

      @Override
      public void callback(final User response) {
        if (response != null) {
          form.getStyle().setDisplay(Style.Display.NONE);
          logout.getElement().getStyle().setDisplay(Style.Display.INLINE_BLOCK);
        } else {
          form.getStyle().setDisplay(Style.Display.BLOCK);
          logout.getElement().getStyle().setDisplay(Style.Display.NONE);
        }
      }
    });
  }
}
