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
package org.jboss.errai.security.client.local.context.impl;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.errai.security.client.local.context.ActiveUserCache;
import org.jboss.errai.security.client.local.context.Simple;
import org.jboss.errai.security.client.local.identity.UserStorageHandler;
import org.jboss.errai.security.shared.api.identity.User;
import org.slf4j.Logger;

/**
 * A {@link Simple} implementation for {@link ActiveUserCache}, storing a
 * {@link User} in memory and in browser local storage (if configured).
 * 
 * @author Max Barkley <mbarkley@redhat.com>
 */
@Simple
@ApplicationScoped
public class BasicUserCacheImpl implements ActiveUserCache {

  @Inject
  private Logger logger;

  @Inject
  private UserStorageHandler userStorageHandler;

  private boolean valid = false;

  private User activeUser;

  @Override
  public User getUser() {
    return activeUser;
  }

  @Override
  public void setUser(User user) {
    setActiveUser(user, true);
  }

  @PostConstruct
  private void maybeLoadStoredCache() {
    logger.debug("PostConstruct invoked.");
    if (!isValid()) {
      logger.debug("Checking for user in local storage.");
      final User storedUser = userStorageHandler.getUser();

      if (storedUser != null) {
        setActiveUser(storedUser, false);
      }
    }
  }

  private void setActiveUser(User user, boolean localStorage) {
    logger.debug("Setting active user: " + String.valueOf(user));
    valid = true;
    activeUser = user;
    if (localStorage) {
      userStorageHandler.setUser(user);
    }
  }

  @Override
  public boolean hasUser() {
    return getUser() != null;
  }

  @Override
  public boolean isValid() {
    return valid;
  }

  @Override
  public void invalidateCache() {
    logger.debug("Invalidating cache.");
    valid = false;
    activeUser = null;
    userStorageHandler.setUser(null);
  }

}
