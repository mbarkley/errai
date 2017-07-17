/*
 * Copyright (C) 2017 Red Hat, Inc. and/or its affiliates.
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

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;

import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder.Resolution;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder.ResolutionCardinality;
import org.jboss.errai.ioc.rebind.ioc.graph.api.Injectable;

/**
 *
 * @author Max Barkley <mbarkley@redhat.com>
 */
public class AnyResolution implements Resolution {

  private final Collection<Injectable> injectables;

  public AnyResolution(final Collection<Injectable> injectables) {
    this.injectables = injectables;
  }

  @Override
  public ResolutionCardinality getCardinality() {
    return ResolutionCardinality.ANY;
  }

  @Override
  public Optional<Injectable> asSingle() {
    return Optional.empty();
  }

  @Override
  public Optional<Collection<Injectable>> asAny() {
    return Optional.of(Collections.unmodifiableCollection(injectables));
  }

  @Override
  public Stream<Injectable> stream() {
    return injectables.stream();
  }

  @Override
  public String toString() {
    return "AnyResolution [" + injectables.stream().map(o -> o.toString()).reduce((a, b) -> a + ", " + b).orElse("") + "]";
  }

}
