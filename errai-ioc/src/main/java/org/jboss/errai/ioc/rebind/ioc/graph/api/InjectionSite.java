/**
 * Copyright (C) 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.errai.ioc.rebind.ioc.graph.api;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;

import org.jboss.errai.codegen.meta.HasAnnotations;
import org.jboss.errai.codegen.meta.MetaClass;

/**
 * Contains metadata for a single injection point.
 *
 * @author Max Barkley <mbarkley@redhat.com>
 */
public class InjectionSite implements HasAnnotations {

  private final MetaClass enclosingType;
  private final HasAnnotations annotated;
  private final Collection<Injectable> otherResolvedInjectables;
  private final MetaClass siteType;

  public InjectionSite(final MetaClass enclosingType, final HasAnnotations annotated, final MetaClass siteType, final Collection<Injectable> otherResolvedInjectables) {
    this.enclosingType = enclosingType;
    this.annotated = annotated;
    this.siteType = siteType;
    this.otherResolvedInjectables = Collections.unmodifiableCollection(otherResolvedInjectables);
  }

  /**
   * @return The enclosing type for this injection site.
   */
  public MetaClass getEnclosingType() {
    return enclosingType;
  }

  @Override
  public Annotation[] getAnnotations() {
    return annotated.getAnnotations();
  }

  @Override
  public boolean isAnnotationPresent(final Class<? extends Annotation> annotation) {
    return annotated.isAnnotationPresent(annotation);
  }

  @Override
  public <A extends Annotation> A getAnnotation(final Class<A> annotation) {
    return annotated.getAnnotation(annotation);
  }

  /**
   * @return The exact type of this injection site.
   */
  public MetaClass getExactType() {
    return siteType;
  }

  /**
   * This method exists primarily to allow extensions to give more useful error messages.
   *
   * @return A collection of other {@link Injectable injectables} that were
   *         resolved for this site but were of lower priority.
   */
  public Collection<Injectable> getOtherResolvedInjectables() {
    return otherResolvedInjectables;
  }

  @Override
  public String toString() {
    return "InjectionSite [enclosingType=" + enclosingType + ", annotated=" + annotated + ", otherResolvedInjectables="
            + otherResolvedInjectables + ", siteType=" + siteType + "]";
  }

}