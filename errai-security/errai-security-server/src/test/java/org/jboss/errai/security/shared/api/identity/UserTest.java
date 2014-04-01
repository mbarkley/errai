package org.jboss.errai.security.shared.api.identity;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.jboss.errai.security.shared.api.RoleImpl;
import org.junit.Test;

public class UserTest {

  @Test
  public void userImplShouldCaptureRolesFromConstructor() {
    User user = new UserImpl("test", Arrays.asList(new RoleImpl("a"), new RoleImpl("b"), new RoleImpl("c")));
    assertTrue(user.getRoles().contains(new RoleImpl("a")));
  }

  @Test
  public void testHasAnyRoles() {
    User user = new UserImpl("test", Arrays.asList(new RoleImpl("a"), new RoleImpl("b"), new RoleImpl("c")));
    assertTrue(user.hasAnyRoles("a"));
    assertTrue(user.hasAnyRoles("b", "c"));
    assertTrue(user.hasAnyRoles("a", "f"));

    assertFalse(user.hasAnyRoles("f"));
    assertFalse(user.hasAnyRoles("f", "d"));
    assertFalse(user.hasAnyRoles());
  }

  @Test
  public void testHasAllRoles() throws Exception {
    User user = new UserImpl("test", Arrays.asList(new RoleImpl("a"), new RoleImpl("b"), new RoleImpl("c")));
    assertTrue(user.hasAllRoles("a"));
    assertTrue(user.hasAllRoles("a", "b"));
    assertTrue(user.hasAllRoles("c", "a", "b"));

    assertFalse(user.hasAllRoles("f"));
    assertFalse(user.hasAllRoles("a", "f"));
    assertFalse(user.hasAllRoles("a", "b", "f", "c"));
    assertTrue(user.hasAllRoles());
  }
}
