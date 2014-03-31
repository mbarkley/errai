package org.jboss.errai.security.shared.api.identity;

import static java.util.Collections.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.errai.common.client.api.Assert;
import org.jboss.errai.common.client.api.annotations.MapsTo;
import org.jboss.errai.common.client.api.annotations.Portable;
import org.jboss.errai.security.shared.api.Role;

@Portable
public class UserImpl implements User, Serializable {

  private static final long serialVersionUID = 3172905561115755369L;

  private final List<Role> roles = new ArrayList<Role>();
  private final String name;
  private final Map<String, String> properties = new HashMap<String, String>();

  public UserImpl(final String name) {
    this(name, Collections.<Role> emptyList());
  }

  public UserImpl(final String name, final Collection<? extends Role> roles) {
    this.name = name;
    this.roles.addAll(roles);
  }

  public UserImpl(
          @MapsTo("name") final String name,
          @MapsTo("roles") final Collection<? extends Role> roles,
          @MapsTo("properties") final Map<String, String> properties) {
    this.name = name;
    this.roles.addAll(roles);
    this.properties.putAll(properties);
  }

  @Override
  public List<Role> getRoles() {
    return roles;
  }

  @Override
  public boolean hasRole(final Role role) {
    Assert.notNull(role);
    for (final Role activeRole : roles) {
      if (activeRole.getName().equals(role.getName())) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Map<String, String> getProperties() {
    return unmodifiableMap(properties);
  }

  @Override
  public void setProperty(final String name, final String value) {
    properties.put(name, value);
  }

  @Override
  public void removeProperty(final String name) {
    properties.remove(name);
  }

  @Override
  public String getProperty(final String name) {
    return properties.get(name);
  }

  @Override
  public String getIdentifier() {
    return name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof User)) {
      return false;
    }

    User user = (User) o;

    return name.equals(user.getIdentifier());
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public String toString() {
    return "SubjectImpl [roles=" + roles + ", name=" + name + ", properties="
            + properties + "]";
  }

}
