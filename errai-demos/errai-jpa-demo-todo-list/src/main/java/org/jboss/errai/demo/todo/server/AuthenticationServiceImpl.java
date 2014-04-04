package org.jboss.errai.demo.todo.server;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.jboss.errai.bus.server.annotations.Service;
import org.jboss.errai.demo.todo.shared.TodoListUser;
import org.jboss.errai.security.shared.exception.AuthenticationException;
import org.jboss.errai.security.shared.service.AuthenticationService;
import org.picketlink.Identity;
import org.picketlink.credential.DefaultLoginCredentials;
import org.picketlink.idm.model.basic.User;

@Dependent @Stateless @Service
public class AuthenticationServiceImpl implements AuthenticationService {

  @Inject private DefaultLoginCredentials credentials;
  @Inject private Identity identity;
  @Inject private EntityManager entityManager;

  @TransactionAttribute(TransactionAttributeType.REQUIRED)
  @Override
  public org.jboss.errai.security.shared.api.identity.User login(String username, String password) {
    credentials.setUserId(username);
    credentials.setPassword(password);
    
    if (identity.login() == Identity.AuthenticationResult.SUCCESS) {
      final User picketLinkUser = (User) identity.getAccount();
      final TodoListUser todoListUser = lookupTodoListUser(picketLinkUser.getEmail());

      return todoListUser;
    }
    else {
      throw new AuthenticationException();
    }
  }

  @Override
  public boolean isLoggedIn() {
    return identity.isLoggedIn();
  }

  @Override
  public void logout() {
    identity.logout();
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRED)
  @Override
  public org.jboss.errai.security.shared.api.identity.User getUser() {
    if (identity.isLoggedIn()) {
      final User picketLinkUser = (User)identity.getAccount();
      return lookupTodoListUser(picketLinkUser.getEmail());
    }
    else {
      return org.jboss.errai.security.shared.api.identity.User.ANONYMOUS;
    }
  }
  
  private TodoListUser lookupTodoListUser(String email) {
      final TodoListUser todoListUser = entityManager
              .createNamedQuery("userByEmail", TodoListUser.class)
              .setParameter("email", email)
              .getSingleResult();

      return todoListUser;
  }
}
