/*
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

package org.jboss.errai.ioc.rebind.ioc.graph.impl;

import java.lang.annotation.Annotation;

import org.jboss.errai.codegen.meta.HasAnnotations;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder.Dependency;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder.DependencyType;

/**
 * Base implementation for all {@link Dependency dependencies}.
 *
 * @author Max Barkley <mbarkley@redhat.com>
 */
abstract class BaseDependency implements Dependency {
  private final InjectableHandle injectable;
  private final DependencyType dependencyType;

  BaseDependency(final InjectableHandle injectable, final DependencyType dependencyType) {
    this.injectable = injectable;
    this.dependencyType = dependencyType;
  }

  @Override
  public InjectableHandle getHandle() {
    return injectable;
  }

  @Override
  public String toString() {
    return "[dependencyType=" + dependencyType.toString() + ", reference=" + injectable.toString() + "]";
  }

  @Override
  public DependencyType getDependencyType() {
    return dependencyType;
  }

  @Override
  public Annotation[] getAnnotations() {
    return getAnnotated().getAnnotations();
  }

  @Override
  public boolean isAnnotationPresent(final Class<? extends Annotation> annotation) {
    return getAnnotated().isAnnotationPresent(annotation);
  }

  @Override
  public abstract boolean equals(Object obj);

  @Override
  public abstract int hashCode();

  protected abstract HasAnnotations getAnnotated();
}
