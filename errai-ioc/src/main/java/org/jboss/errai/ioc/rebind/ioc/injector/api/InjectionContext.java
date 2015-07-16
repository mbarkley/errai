package org.jboss.errai.ioc.rebind.ioc.injector.api;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.errai.codegen.Statement;
import org.jboss.errai.codegen.meta.HasAnnotations;
import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaField;
import org.jboss.errai.codegen.meta.MetaMethod;
import org.jboss.errai.codegen.meta.MetaParameter;
import org.jboss.errai.codegen.util.PrivateAccessType;
import org.jboss.errai.ioc.rebind.ioc.bootstrapper.spi.IOCProcessingContext;
import org.jboss.errai.ioc.rebind.ioc.extension.IOCDecoratorExtension;
import org.jboss.errai.ioc.rebind.ioc.graph.GraphBuilder;
import org.jboss.errai.ioc.rebind.ioc.injector.Injector;
import org.jboss.errai.ioc.rebind.ioc.injector.InjectorFactory;
import org.jboss.errai.ioc.rebind.ioc.metadata.QualifyingMetadata;

public interface InjectionContext {

  Injector getProxiedInjector(MetaClass type, QualifyingMetadata metadata);

  Injector getQualifiedInjector(MetaClass type, Annotation[] annotations);

  Injector getQualifiedInjector(MetaClass type, QualifyingMetadata metadata);

  boolean hasInjectorForType(MetaClass type);

  boolean isTypeInjectable(MetaClass type);

  void recordCycle(MetaClass from, MetaClass to);

  boolean cycles(MetaClass from, MetaClass to);

  void addProxiedInjector(Injector proxyInjector);

  /**
   * Marks the proxy for te specified type and qualifying metadata closed.
   *
   * @param injectorType
   * @param qualifyingMetadata
   */
  void markProxyClosedIfNeeded(MetaClass injectorType, QualifyingMetadata qualifyingMetadata);

  boolean isProxiedInjectorRegistered(MetaClass injectorType, QualifyingMetadata qualifyingMetadata);

  boolean isInjectorRegistered(MetaClass injectorType, QualifyingMetadata qualifyingMetadata);

  boolean isInjectableQualified(MetaClass injectorType, QualifyingMetadata qualifyingMetadata);

  boolean isIncluded(MetaClass type);

  boolean isWhitelisted(MetaClass type);

  boolean isBlacklisted(MetaClass type);

  List<Injector> getInjectors(MetaClass type);

  Injector getInjector(MetaClass type);

  void registerInjector(Injector injector);

  void registerDecorator(IOCDecoratorExtension<?> iocExtension);

  Set<Class<? extends Annotation>> getDecoratorAnnotations();

  IOCDecoratorExtension[] getDecorator(Class<? extends Annotation> annotation);

  Collection<Class<? extends Annotation>> getDecoratorAnnotationsBy(ElementType type);

  boolean isMetaAnnotationFor(Class<? extends Annotation> alias, Class<? extends Annotation> forAnno);

  void addExposedField(MetaField field, PrivateAccessType accessType);

  void addExposedMethod(MetaMethod method);

  void declareOverridden(MetaClass type);

  void declareOverridden(MetaMethod method);

  boolean isOverridden(MetaMethod method);

  Map<MetaField, PrivateAccessType> getPrivateFieldsToExpose();

  Collection<MetaMethod> getPrivateMethodsToExpose();

  void addType(MetaClass type);

  void addPseudoScopeForType(MetaClass type);

  IOCProcessingContext getProcessingContext();

  void mapElementType(WiringElementType type, Class<? extends Annotation> annotationType);

  Collection<Class<? extends Annotation>> getAnnotationsForElementType(WiringElementType type);

  boolean isAnyKnownElementType(HasAnnotations hasAnnotations);

  boolean isAnyOfElementTypes(HasAnnotations hasAnnotations, WiringElementType... types);

  boolean isElementType(WiringElementType type, HasAnnotations hasAnnotations);

  boolean isElementType(WiringElementType type, Class<? extends Annotation> annotation);

  /**
   * Overloaded version to check GWT's JClassType classes.
   *
   * @param type
   * @param hasAnnotations
   *
   * @return
   */
  boolean isElementType(WiringElementType type, com.google.gwt.core.ext.typeinfo.HasAnnotations hasAnnotations);

  Annotation getMatchingAnnotationForElementType(WiringElementType type, HasAnnotations hasAnnotations);

  Collection<Map.Entry<WiringElementType, Class<? extends Annotation>>> getAllElementMappings();

  Collection<MetaClass> getAllKnownInjectionTypes();

  void allowProxyCapture();

  void markOpenProxy();

  boolean isProxyOpen();

  void closeProxyIfOpen();

  void addInjectorRegistrationListener(MetaClass clazz, InjectorRegistrationListener listener);

  boolean isReachable(MetaClass clazz);

  boolean isReachable(String fqcn);

  Collection<String> getAllReachableTypes();

  void setAttribute(String name, Object value);

  Object getAttribute(String name);

  boolean hasAttribute(String name);

  void addKnownTypesWithCycles(Collection<String> types);

  boolean typeContainsGraphCycles(MetaClass type);

  void addBeanReference(MetaClass ref, Statement statement);

  Statement getBeanReference(MetaClass ref);

  void addInlineBeanReference(MetaParameter ref, Statement statement);

  Statement getInlineBeanReference(MetaParameter ref);

  void addTopLevelType(MetaClass clazz);

  void addTopLevelTypes(Collection<MetaClass> clazzes);

  boolean hasTopLevelType(MetaClass clazz);

  void addTypeToAlwaysProxy(String fqcn);

  boolean isAlwaysProxied(String fqcn);

  GraphBuilder getGraphBuilder();

  InjectorFactory getInjectorFactory();

  boolean isAsync();

}