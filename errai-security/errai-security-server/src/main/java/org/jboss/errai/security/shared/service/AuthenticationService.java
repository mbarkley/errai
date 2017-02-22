/*
 * Copyright (C) 2014 Red Hat, Inc. and/or its affiliates.
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

package org.jboss.errai.security.shared.service;

import org.jboss.errai.bus.server.annotations.Remote;
import org.jboss.errai.codegen.util.Implementations;
import org.jboss.errai.security.shared.api.identity.Credential;
import org.jboss.errai.security.shared.api.identity.Identifier;
import org.jboss.errai.security.shared.api.identity.User;
import org.jboss.errai.security.shared.exception.AuthenticationException;

/**
 * AuthenticationService service for authenticating users and getting their roles.
 *
 * @author edewit@redhat.com
 */
@Remote
public interface AuthenticationService {

  /**
   * Login with the given username and password, throwing an exception if the login fails.
   *
   * @param username The username to log in with.
   * @param password The password to authenticate with.
   * @return The logged in {@link User}.
   * @throws Implementations should throw an {@link AuthenticationException} if authentication fails.
   */
  public User login(String username, String password);

  /**
   * Login with the given identifier and credential, throwing an exception if the login fails.
   *
   * @param id An identifier for a user, such as a username or email address.
   * @param credential A credential for proving a user's identity, such as a password or a public key.
   * @return The logged in {@link User}.
   * @throws Implementations should throw an {@link AuthenticationException} if authentication fails.
   */
  public User login(Identifier id, Credential credential);

  /**
   * Authenticate a user with the given identifier and credential, throwing an exception if the authentication fails.
   * Unlike {@link #login(Identifier, Credential)}, this does not change the state of the currently logged in user.
   * This method can be called to test the authenticity of an {@link Identifier}-{@link Credential} pair without attempting a login.
   *
   *
   * @param id An identifier for a user, such as a username or email address.
   * @param credential A credential for proving a user's identity, such as a password or a public key.
   * @return A user associated with the given identifier and credentials {@link User}.
   * @throws Implementations should throw an {@link AuthenticationException} if authentication fails.
   */
  public User authenticate(Identifier id, Credential credential);

  /**
   * @return True iff a user is currently logged in.
   */
  public boolean isLoggedIn();

  /**
   * Log out the currently authenticated user. Has no effect if there is no current user.
   */
  public void logout();

  /**
   * Get the currently authenticated user.
   *
   * @return The currently authenticated user. Never returns {@code null}. If no
   *         user is logged in, returns {@link User#ANONYMOUS}.
   */
  public User getUser();
}
