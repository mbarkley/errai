package org.jboss.errai.ioc.client.container;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class InjectorHandleImpl implements InjectorHandle {

  private final Set<Annotation> qualifiers = new HashSet<Annotation>();
  private final Set<Class<?>> assignableTypes = new HashSet<Class<?>>();
  private final Class<?> actualType;
  private final String injectorName;
  private final Class<? extends Annotation> scope;

  public InjectorHandleImpl(final Class<?> actualType, final String injectorName, final Class<? extends Annotation> scope) {
    this.actualType = actualType;
    this.injectorName = injectorName;
    this.scope = scope;
  }

  @Override
  public Set<Annotation> getQualifiers() {
    return Collections.unmodifiableSet(qualifiers);
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
  }

  public void addAssignableType(final Class<?> type) {
    assignableTypes.add(type);
  }

}
