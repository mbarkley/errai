package org.jboss.errai.ioc.client.container;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @see FactoryHandle
 * @author Max Barkley <mbarkley@redhat.com>
 */
public class FactoryHandleImpl implements FactoryHandle {

  // TODO intern qualifiers for all FactoryHandle instances
  private final Set<Annotation> qualifiers = new HashSet<Annotation>();
  private final Set<Class<?>> assignableTypes = new HashSet<Class<?>>();
  private final Class<?> actualType;
  private final String factoryName;
  private final Class<? extends Annotation> scope;
  private final boolean eager;
  private final String beanName;
  private final Class<? extends BeanActivator> activatorType;




  public FactoryHandleImpl(final Class<?> actualType, final String factoryName, final Class<? extends Annotation> scope,
          final boolean eager, final String beanName, final Class<? extends BeanActivator> activatorType) {
    this.actualType = actualType;
    this.factoryName = factoryName;
    this.scope = scope;
    this.eager = eager;
    this.beanName = beanName;
    this.activatorType = activatorType;
  }

  public FactoryHandleImpl(final Class<?> actualType, final String factoryName, final Class<? extends Annotation> scope, final boolean eager, final String beanName) {
    this(actualType, factoryName, scope, eager, beanName, null);
  }

  @Override
  public String getBeanName() {
    return beanName;
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
