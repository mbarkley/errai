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

package org.jboss.errai.ioc.rebind.ioc.graph.api;

import static java.util.Collections.unmodifiableList;

import java.util.List;

/**
 *
 * @author Max Barkley <mbarkley@redhat.com>
 */
public class Fragment {
  private final String name;
  private final List<Injectable> injectables;
  public Fragment(final String name, final List<Injectable> injectables) {
    this.name = name;
    this.injectables = unmodifiableList(injectables);
  }
  public String getName() {
    return name;
  }
  public List<Injectable> getInjectables() {
    return injectables;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("Fragment \"")
      .append(name)
      .append("\":\n");

    injectables
      .stream()
      .forEach(inj -> sb.append('\t').append(inj).append("\n"));

    return sb.toString();
  }
}
