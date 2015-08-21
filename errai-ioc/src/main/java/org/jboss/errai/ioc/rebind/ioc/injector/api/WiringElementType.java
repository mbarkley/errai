package org.jboss.errai.ioc.rebind.ioc.injector.api;

/**
 * @author Mike Brock
 */
public enum WiringElementType {
  Type,
  QualifiyingType,
  SingletonBean,
  NormalScopedBean,
  JsType,
  DependentBean,
  Simpleton, // TODO review name
  Provider,
  InjectionPoint,
  ProducerElement,
  AlternativeBean,
  NotSupported
}
