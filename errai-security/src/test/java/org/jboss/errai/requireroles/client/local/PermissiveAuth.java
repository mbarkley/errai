package org.jboss.errai.requireroles.client.local;

import java.util.ArrayList;
import java.util.List;

import org.jboss.errai.bus.client.framework.AbstractRpcProxy;
import org.jboss.errai.security.shared.AuthenticationService;
import org.jboss.errai.security.shared.Role;
import org.jboss.errai.security.shared.User;
import org.jboss.errai.ui.nav.client.shared.PageRequest;

public class PermissiveAuth extends AbstractRpcProxy implements AuthenticationService {
  
  private User user;
  
  @Override
  public User login(String username, String password) {
    return (user = new User(username));
  }

  @Override
  public boolean isLoggedIn() {
    return user != null;
  }

  @Override
  public void logout() {
    user = null;
  }

  @Override
  public User getUser() {
    return user;
  }

  @Override
  public List<Role> getRoles() {
    
    List<Role> roles = new ArrayList<Role>();
    
    if (user != null && user.getLoginName().equals("admin"))
      roles.add(new Role("admin"));
    else if (user != null)
      roles.add(new Role("user"));
    
    return roles;
  }

  @Override
  public boolean hasPermission(PageRequest pageRequest) {
    return true;
  }

}
