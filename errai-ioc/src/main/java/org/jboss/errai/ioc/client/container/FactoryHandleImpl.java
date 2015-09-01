package org.jboss.errai.ioc.client.container;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;

public class FactoryHandleImpl implements FactoryHandle {

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
  private final String factoryName;
  private final Class<? extends Annotation> scope;
  private final boolean eager;
  private final Class<? extends BeanActivator> activatorType;



  public FactoryHandleImpl(final Class<?> actualType, final String factoryName, final Class<? extends Annotation> scope,
          final boolean eager, final Class<? extends BeanActivator> activatorType) {
    this.actualType = actualType;
    this.factoryName = factoryName;
    this.scope = scope;
    this.eager = eager;
    this.activatorType = activatorType;
    qualifiers.add(ANY);
    qualifiers.add(DEFAULT);
  }

  public FactoryHandleImpl(final Class<?> actualType, final String factoryName, final Class<? extends Annotation> scope, final boolean eager) {
    this(actualType, factoryName, scope, eager, null);
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
  public String getFactoryName() {
    return factoryName;
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
    return "[type=" + actualType + ", name=" + factoryName + ", scope=" + scope.getSimpleName() + ", qualifiers=" + qualifiers + "]";
  }

  @Override
  public boolean isEager() {
    return eager;
  }

  @Override
  public Class<? extends BeanActivator> getBeanActivatorType() {
    return activatorType;
  }

}
