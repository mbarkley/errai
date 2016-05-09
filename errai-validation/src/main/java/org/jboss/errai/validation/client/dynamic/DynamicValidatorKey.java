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

package org.jboss.errai.validation.client.dynamic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.jboss.errai.common.client.function.Optional;
import org.jboss.errai.common.client.logging.util.StringFormat;

public class DynamicValidatorKey {

  private static final Map<String, String> typeAliases = new HashMap<>();

  static {
    typeAliases.put(Collections.emptyList().getClass().getName(), List.class.getName());
    typeAliases.put(Collections.singletonList(null).getClass().getName(), List.class.getName());
    typeAliases.put(ArrayList.class.getName(), List.class.getName());
    typeAliases.put(LinkedList.class.getName(), List.class.getName());

    typeAliases.put(Collections.emptySet().getClass().getName(), Set.class.getName());
    typeAliases.put(Collections.singleton(null).getClass().getName(), Set.class.getName());
    typeAliases.put(HashSet.class.getName(), Set.class.getName());
    typeAliases.put(LinkedHashSet.class.getName(), Set.class.getName());
    typeAliases.put(TreeSet.class.getName(), Set.class.getName());

    typeAliases.put(Collections.emptyMap().getClass().getName(), Map.class.getName());
    typeAliases.put(Collections.singletonMap(null, null).getClass().getName(), Map.class.getName());
    typeAliases.put(HashMap.class.getName(), Map.class.getName());
    typeAliases.put(LinkedHashMap.class.getName(), Map.class.getName());
    typeAliases.put(TreeMap.class.getName(), Map.class.getName());

    typeAliases.put(List.class.getName(), Collection.class.getName());
    typeAliases.put(Set.class.getName(), Collection.class.getName());
  }

  private final String constraint;
  private final String valueType;

  public DynamicValidatorKey(final String constraint, final String valueType) {
    this.constraint = constraint;
    this.valueType = valueType;
  }

  public String getConstraint() {
    return constraint;
  }

  public String getValueType() {
    return valueType;
  }

  public boolean hasAlias() {
    return getAlias().isPresent();
  }

  public Optional<DynamicValidatorKey> getAlias() {
    if (isNonPrimitiveArrayType() && !Object[].class.getName().equals(valueType)) {
      return Optional.ofNullable(new DynamicValidatorKey(constraint, Object[].class.getName()));
    }
    else {
      return Optional
              .ofNullable(typeAliases.get(valueType))
              .map(alias -> new DynamicValidatorKey(constraint, alias));
    }
  }

  private boolean isNonPrimitiveArrayType() {
    return valueType.startsWith("[") && valueType.length() > 2;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((constraint == null) ? 0 : constraint.hashCode());
    result = prime * result + ((valueType == null) ? 0 : valueType.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    final DynamicValidatorKey other = (DynamicValidatorKey) obj;
    if (constraint == null) {
      if (other.constraint != null)
        return false;
    }
    else if (!constraint.equals(other.constraint))
      return false;
    if (valueType == null) {
      if (other.valueType != null)
        return false;
    }
    else if (!valueType.equals(other.valueType))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return StringFormat.format("DynamicValidatorKey(constraint=%s,valueType=%s)", constraint, valueType);
  }

}
