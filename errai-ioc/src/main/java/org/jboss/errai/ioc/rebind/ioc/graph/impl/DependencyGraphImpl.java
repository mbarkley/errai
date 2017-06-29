/**
 * Copyright (C) 2016 Red Hat, Inc. and/or its affiliates.
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

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.jboss.errai.ioc.client.api.EntryPoint;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraph;
import org.jboss.errai.ioc.rebind.ioc.graph.api.Fragment;
import org.jboss.errai.ioc.rebind.ioc.graph.api.Injectable;

import com.google.common.collect.Iterators;

/**
 * @see DependencyGraph
 * @author Max Barkley <mbarkley@redhat.com>
 */
class DependencyGraphImpl implements DependencyGraph {

  private final Map<String, Injectable> injectablesByName;

  DependencyGraphImpl(final Map<String, Injectable> injectablesByName) {
    this.injectablesByName = injectablesByName;
  }

  @Override
  public Iterator<Injectable> iterator() {
    return Iterators.unmodifiableIterator(injectablesByName.values().iterator());
  }

  @Override
  public Injectable getConcreteInjectable(final String injectableName) {
    return injectablesByName.get(injectableName);
  }

  @Override
  public int getNumberOfInjectables() {
    return injectablesByName.size();
  }

  @Override
  public List<Fragment> getFragments() {
    class Rep {
      Injectable rep;
      Rep(final Injectable rep) {
        this.rep = rep;
      }
      @Override
      public boolean equals(final Object obj) {
        return obj instanceof Rep && ((Rep) obj).rep.equals(rep);
      }
      @Override
      public int hashCode() {
        return rep.hashCode();
      }
    }

    final Map<Injectable, Rep> injToFragmentRep = new HashMap<>();
    injectablesByName
      .values()
      .stream()
      .filter(inj -> EntryPoint.class.equals(inj.getScope()))
      .forEach(inj -> {
        final Queue<Injectable> queue = new LinkedList<>();
        final Rep rep = new Rep(inj);
        queue.add(inj);
        do {
          final Injectable cur = queue.poll();
          final Rep prevRep = injToFragmentRep.get(cur);
          if (prevRep == null) {
            injToFragmentRep.put(cur, rep);
            cur
              .getDependencies()
              .stream()
              .map(dep -> GraphUtil.getResolvedDependency(dep, cur))
              .forEach(queue::add);
          } else if (!prevRep.equals(rep)) {
            prevRep.rep = rep.rep;
          }
        } while (!queue.isEmpty());
      });

    final Map<Injectable, List<Injectable>> injectablesByRep = injToFragmentRep
      .entrySet()
      .stream()
      .collect(groupingBy(e -> e.getValue().rep,
               mapping(e -> e.getKey(),
               toList())));

    return injectablesByRep
      .entrySet()
      .stream()
      .map(e -> new Fragment(prettyPrint(e.getKey().getHandle()), e.getValue()))
      .collect(toList());
  }

  private String prettyPrint(final InjectableHandle handle) {
    return handle.getQualifier().toString() + " " + handle.getType();
  }

}