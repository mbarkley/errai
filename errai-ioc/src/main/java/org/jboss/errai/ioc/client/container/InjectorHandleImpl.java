package org.jboss.errai.ioc.client.container;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;

public class InjectorHandleImpl implements InjectorHandle {

  private static final Annotation DEFAULT = new Default() {
    @Override
    public Class<? extends Annotation> annotationType() {
      return Default.class;
    }
  };

  private static final Annotation ANY = new Any() {
    @Override
    public Class<? extends Annotation> annotationType() {
      return Any.class;
    }
  };

  private final Set<Annotation> qualifiers = new HashSet<Annotation>();
  private final Set<Class<?>> assignableTypes = new HashSet<Class<?>>();
  private final Class<?> actualType;
  private final String injectorName;
  private final Class<? extends Annotation> scope;

  public InjectorHandleImpl(final Class<?> actualType, final String injectorName, final Class<? extends Annotation> scope) {
    this.actualType = actualType;
    this.injectorName = injectorName;
    this.scope = scope;
    qualifiers.add(ANY);
    qualifiers.add(DEFAULT);
  }

  @Override
  public Set<Annotation> getQualifiers() {
    return qualifiers;
  }

  @Override
  public Set<Class<?>> getAssignableTypes() {
    return Collections.unmodifiableSet(assignableTypes);
  }

  @Override
  public Class<?> getActualType() {
    return actualType;
  }

  @Override
  public String getInjectorName() {
    return injectorName;
  }

  @Override
  public Class<? extends Annotation> getScope() {
    return scope;
  }

  public void addQualifier(final Annotation qualifier) {
    qualifiers.add(qualifier);
    // Because this uses Object.equals/hashCode, it should only remove this particular instance of @Default
    qualifiers.remove(DEFAULT);
  }

  public void addAssignableType(final Class<?> type) {
    assignableTypes.add(type);
  }

  @Override
  public String toString() {
    return "[type=" + actualType + ", name=" + injectorName + ", scope=" + scope.getSimpleName() + ", qualifiers=" + qualifiers + "]";
  }

}
