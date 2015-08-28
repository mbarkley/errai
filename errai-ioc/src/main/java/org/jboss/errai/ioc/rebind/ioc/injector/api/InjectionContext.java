/*
 * Copyright 2011 JBoss, by Red Hat, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.errai.ioc.rebind.ioc.injector.api;

import static java.util.Collections.unmodifiableCollection;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.NormalScope;
import javax.enterprise.inject.Stereotype;
import javax.inject.Qualifier;
import javax.inject.Scope;

import org.jboss.errai.codegen.Statement;
import org.jboss.errai.codegen.meta.HasAnnotations;
import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaParameter;
import org.jboss.errai.common.client.api.Assert;
import org.jboss.errai.config.util.ClassScanner;
import org.jboss.errai.ioc.rebind.ioc.bootstrapper.IOCProcessingContext;
import org.jboss.errai.ioc.rebind.ioc.extension.IOCDecoratorExtension;
import org.jboss.errai.ioc.rebind.ioc.graph.DefaultQualifierFactory;
import org.jboss.errai.ioc.rebind.ioc.graph.Injectable;
import org.jboss.errai.ioc.rebind.ioc.graph.InjectableHandle;
import org.jboss.errai.ioc.rebind.ioc.graph.QualifierFactory;
import org.jboss.errai.ioc.rebind.ioc.injector.Injector;
import org.jboss.errai.ioc.rebind.ioc.injector.InjectorImpl;
import org.jboss.errai.reflections.util.SimplePackageFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

public class InjectionContext {
  private static final Logger log = LoggerFactory.getLogger(InjectionContext.class);

  private final IOCProcessingContext processingContext;

  private final Multimap<WiringElementType, Class<? extends Annotation>> elementBindings = HashMultimap.create();
  private final Multimap<InjectableHandle, InjectableProvider> injectableProviders = HashMultimap.create();

  private final boolean async;

  private final Map<Injectable, Injector> injectors = new HashMap<Injectable, Injector>();
  private final QualifierFactory qualifierFactory;

  private final Set<String> whitelist;
  private final Set<String> blacklist;

  private static final String[] implicitWhitelist = { "org.jboss.errai.*", "com.google.gwt.*" };

  private final Multimap<Class<? extends Annotation>, IOCDecoratorExtension<? extends Annotation>> decorators = HashMultimap.create();
  private final Multimap<ElementType, Class<? extends Annotation>> decoratorsByElementType = HashMultimap.create();
  private final Multimap<Class<? extends Annotation>, Class<? extends Annotation>> metaAnnotationAliases
      = HashMultimap.create();

  private final Map<MetaParameter, Statement> inlineBeanReferenceMap = new HashMap<MetaParameter, Statement>();

  private final Map<String, Object> attributeMap = new HashMap<String, Object>();


  private InjectionContext(final Builder builder) {
    this.processingContext = Assert.notNull(builder.processingContext);
    if (builder.qualifierFactory == null) {
      this.qualifierFactory = new DefaultQualifierFactory();
    } else {
      this.qualifierFactory = builder.qualifierFactory;
    }
    this.whitelist = Assert.notNull(builder.whitelist);
    this.blacklist = Assert.notNull(builder.blacklist);
    this.async = builder.async;
  }

  public static class Builder {
    private IOCProcessingContext processingContext;
    private boolean async;
    private QualifierFactory qualifierFactory;
    private final HashSet<String> enabledAlternatives = new HashSet<String>();
    private final HashSet<String> whitelist = new HashSet<String>();
    private final HashSet<String> blacklist = new HashSet<String>();

    public static Builder create() {
      return new Builder();
    }

    public Builder qualifierFactory(final QualifierFactory qualifierFactory) {
      this.qualifierFactory = qualifierFactory;
      return this;
    }

    public Builder processingContext(final IOCProcessingContext processingContext) {
      this.processingContext = processingContext;
      return this;
    }

    public Builder enabledAlternative(final String fqcn) {
      enabledAlternatives.add(fqcn);
      return this;
    }

    public Builder addToWhitelist(final String item) {
      whitelist.add(item);
      return this;
    }

    public Builder addToBlacklist(final String item) {
      blacklist.add(item);
      return this;
    }

    public Builder asyncBootstrap(final boolean async) {
      this.async = async;
      return this;
    }

    public InjectionContext build() {
      Assert.notNull("the processingContext cannot be null", processingContext);

      return new InjectionContext(this);
    }
  }

  public void registerInjectableProvider(final InjectableHandle handle, final InjectableProvider provider) {
    injectableProviders.put(handle, provider);
  }

  public Multimap<InjectableHandle, InjectableProvider> getRegisteredInjectableProviders() {
    return Multimaps.unmodifiableMultimap(injectableProviders);
  }

  public QualifierFactory getQualifierFactory() {
    return qualifierFactory;
  }

  public boolean isIncluded(final MetaClass type) {
    return isWhitelisted(type) && !isBlacklisted(type);
  }

  public boolean isWhitelisted(final MetaClass type) {
    if (whitelist.isEmpty()) {
      return true;
    }

    final SimplePackageFilter implicitFilter = new SimplePackageFilter(Arrays.asList(implicitWhitelist));
    final SimplePackageFilter whitelistFilter = new SimplePackageFilter(whitelist);
    final String fullName = type.getFullyQualifiedName();

    return implicitFilter.apply(fullName) || whitelistFilter.apply(fullName);
  }

  public boolean isBlacklisted(final MetaClass type) {
    final SimplePackageFilter blacklistFilter = new SimplePackageFilter(blacklist);
    final String fullName = type.getFullyQualifiedName();

    return blacklistFilter.apply(fullName);
  }

  public void registerDecorator(final IOCDecoratorExtension<?> iocExtension) {
    final Class<? extends Annotation> annotation = iocExtension.decoratesWith();

    final Target target = annotation.getAnnotation(Target.class);
    if (target != null) {
      final boolean oneTarget = target.value().length == 1;

      for (final ElementType type : target.value()) {
        if (type == ElementType.ANNOTATION_TYPE) {
          // type is a meta-annotation. so we need to map all annotations with this
          // meta-annotation to the decorator extension.

          for (final MetaClass annotationClazz : ClassScanner.getTypesAnnotatedWith(annotation,
                  processingContext.getGeneratorContext())) {
            if (Annotation.class.isAssignableFrom(annotationClazz.asClass())) {
              final Class<? extends Annotation> javaAnnoCls = annotationClazz.asClass().asSubclass(Annotation.class);
              decorators.get(javaAnnoCls).add(iocExtension);

              if (oneTarget) {
                metaAnnotationAliases.put(javaAnnoCls, annotation);
              }
            }
          }
          if (oneTarget) {
            return;
          }
        }
      }
    }
    decorators.get(annotation).add(iocExtension);
  }

  public Injector getInjector(final Injectable injectable) {
    Injector injector = injectors.get(injectable);
    if (injector == null) {
      injector = new InjectorImpl(injectable);
      injectors.put(injectable, injector);
    }

    return injector;
  }

  public Set<Class<? extends Annotation>> getDecoratorAnnotations() {
    return Collections.unmodifiableSet(decorators.keySet());
  }

  public <A extends Annotation> IOCDecoratorExtension<A>[] getDecorators(final Class<A> annotation) {
    final Collection<IOCDecoratorExtension<?>> decs = decorators.get(annotation);
    @SuppressWarnings("unchecked")
    final IOCDecoratorExtension<A>[] da = new IOCDecoratorExtension[decs.size()];
    decs.toArray(da);

    return da;
  }

  public Collection<Class<? extends Annotation>> getDecoratorAnnotationsBy(final ElementType type) {
    if (decoratorsByElementType.size() == 0) {
      sortDecorators();
    }
    if (decoratorsByElementType.containsKey(type)) {
      return unmodifiableCollection(decoratorsByElementType.get(type));
    }
    else {
      return Collections.emptySet();
    }
  }

  public boolean isMetaAnnotationFor(final Class<? extends Annotation> alias, final Class<? extends Annotation> forAnno) {
    return metaAnnotationAliases.containsEntry(alias, forAnno);
  }

  private void sortDecorators() {
    for (final Class<? extends Annotation> a : getDecoratorAnnotations()) {
      if (a.isAnnotationPresent(Target.class)) {
        for (final ElementType type : a.getAnnotation(Target.class).value()) {
          decoratorsByElementType.get(type).add(a);
        }
      }
      else {
        for (final ElementType type : ElementType.values()) {
          decoratorsByElementType.get(type).add(a);
        }
      }
    }
  }

  public IOCProcessingContext getProcessingContext() {
    return processingContext;
  }

  public void mapElementType(final WiringElementType type, final Class<? extends Annotation> annotationType) {
    elementBindings.put(type, annotationType);
  }

  public Collection<Class<? extends Annotation>> getAnnotationsForElementType(final WiringElementType type) {
    return unmodifiableCollection(elementBindings.get(type));
  }

  public boolean isAnyKnownElementType(final HasAnnotations hasAnnotations) {
    return isAnyOfElementTypes(hasAnnotations, WiringElementType.values());
  }

  public boolean isAnyOfElementTypes(final HasAnnotations hasAnnotations, final WiringElementType... types) {
    for (final WiringElementType t : types) {
      if (isElementType(t, hasAnnotations))
        return true;
    }
    return false;
  }

  public boolean isElementType(final WiringElementType type, final HasAnnotations hasAnnotations) {
    final Annotation matchingAnnotation = getMatchingAnnotationForElementType(type, hasAnnotations);
    if (matchingAnnotation != null && type == WiringElementType.NotSupported) {
      log.error(hasAnnotations + " was annotated with " + matchingAnnotation.annotationType().getName()
          + " which is not supported in client-side Errai code!");
    }

    return matchingAnnotation != null;
  }

  public boolean isElementType(final WiringElementType type, final Class<? extends Annotation> annotation) {
    return getAnnotationsForElementType(type).contains(annotation);
  }

  /**
   * Overloaded version to check GWT's JClassType classes.
   *
   * @param type
   * @param hasAnnotations
   *
   * @return
   */
  public boolean isElementType(final WiringElementType type,
                               final com.google.gwt.core.ext.typeinfo.HasAnnotations hasAnnotations) {
    final Collection<Class<? extends Annotation>> annotationsForElementType = getAnnotationsForElementType(type);
    for (final Annotation a : hasAnnotations.getAnnotations()) {
      if (annotationsForElementType.contains(a.annotationType())) {
        return true;
      }
    }
    return false;
  }

  public Annotation getMatchingAnnotationForElementType(final WiringElementType type,
                                                        final HasAnnotations hasAnnotations) {

    final Collection<Class<? extends Annotation>> annotationsForElementType = getAnnotationsForElementType(type);

    for (final Annotation a : hasAnnotations.getAnnotations()) {
      if (annotationsForElementType.contains(a.annotationType())) {
        return a;
      }
    }

    final Set<Annotation> annotationSet = new HashSet<Annotation>();

    fillInStereotypes(annotationSet, hasAnnotations.getAnnotations(), false);

    for (final Annotation a : annotationSet) {
      if (annotationsForElementType.contains(a.annotationType())) {
        return a;
      }
    }
    return null;
  }

  private static void fillInStereotypes(final Set<Annotation> annotationSet,
                                        final Annotation[] from,
                                        boolean filterScopes) {

    final List<Class<? extends Annotation>> stereotypes
        = new ArrayList<Class<? extends Annotation>>();

    for (final Annotation a : from) {
      final Class<? extends Annotation> aClass = a.annotationType();
      if (aClass.isAnnotationPresent(Stereotype.class)) {
        stereotypes.add(aClass);
      }
      else if (!filterScopes &&
          aClass.isAnnotationPresent(NormalScope.class) ||
          aClass.isAnnotationPresent(Scope.class)) {
        filterScopes = true;

        annotationSet.add(a);
      }
      else if (aClass.isAnnotationPresent(Qualifier.class)) {
        annotationSet.add(a);
      }
    }

    for (final Class<? extends Annotation> stereotype : stereotypes) {
      fillInStereotypes(annotationSet, stereotype.getAnnotations(), filterScopes);
    }
  }

  public Collection<Map.Entry<WiringElementType, Class<? extends Annotation>>> getAllElementMappings() {
    return unmodifiableCollection(elementBindings.entries());
  }

  public void setAttribute(final String name, final Object value) {
    attributeMap.put(name, value);
  }

  public Object getAttribute(final String name) {
    return attributeMap.get(name);
  }

  public boolean hasAttribute(final String name) {
    return attributeMap.containsKey(name);
  }

  public void addInlineBeanReference(final MetaParameter ref, final Statement statement) {
    inlineBeanReferenceMap.put(ref, statement);
  }

  public Statement getInlineBeanReference(final MetaParameter ref) {
    return inlineBeanReferenceMap.get(ref);
  }

  public boolean isAsync() {
    return async;
  }
}
