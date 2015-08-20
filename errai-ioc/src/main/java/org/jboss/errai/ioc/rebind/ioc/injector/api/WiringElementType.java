package org.jboss.errai.ioc.rebind.ioc.injector.api;

/**
 * @author Mike Brock
 */
public enum WiringElementType {
  Type,
  QualifiyingType,
  SingletonBean,
  NormalScopedBean,
  DependentBean,
  Simpleton, // TODO review name
  ContextualTopLevelProvider,
  TopLevelProvider,
  InjectionPoint,
  ProducerElement,
  AlternativeBean,
  NotSupported
}
