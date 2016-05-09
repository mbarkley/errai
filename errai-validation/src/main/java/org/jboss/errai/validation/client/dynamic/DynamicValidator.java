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

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.enterprise.inject.Alternative;
import javax.validation.ConstraintViolation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Christian Sadilek <csadilek@redhat.com>
 * @author Max Barkley <mbarkley@redhat.com>
 */
@SuppressWarnings("unchecked")
@Alternative
public class DynamicValidator {

  private static final Logger logger = LoggerFactory.getLogger(DynamicValidator.class);

  private final Map<DynamicValidatorKey, GeneratedDynamicValidator<?>> validators = new HashMap<>();

  public void addValidator(final String constraint, final String valueType, final GeneratedDynamicValidator<?> validator) {
    final DynamicValidatorKey key = new DynamicValidatorKey(constraint, valueType);
    if (validators.containsKey(key)) {
      logger.warn("Validator for " + key + " is being overridden.\n\tPrevious: "
              + validators.get(key).getClass().getSimpleName() + "\n\tNew: " + validator.getClass().getSimpleName());
    }
    validators.put(key, validator);
  }

  public <T> Set<ConstraintViolation<T>> validate(final Class<? extends Annotation> constraint,
          final Map<String, Object> parameters, final T value) {
    return validate(constraint.getName(), parameters, value);
  }

  public <T> Set<ConstraintViolation<T>> validate(final String constraint, final Map<String, Object> parameters, final T value) {
    final GeneratedDynamicValidator<T> dynamicValidator = getValidatorOrThrow(constraint, value);
    return dynamicValidator.validate(parameters, value);
  }

  private <T> GeneratedDynamicValidator<T> getValidatorOrThrow(final String constraint, final T value) {
    final DynamicValidatorKey originalKey = new DynamicValidatorKey(constraint, value.getClass().getName());
    DynamicValidatorKey key = originalKey;
    GeneratedDynamicValidator<T> dynamicValidator = (GeneratedDynamicValidator<T>) validators.get(key);

    while (dynamicValidator == null && key.hasAlias()) {
      key = key.getAlias().get();
      dynamicValidator = (GeneratedDynamicValidator<T>) validators.get(key);
    }

    if (dynamicValidator == null) {
      throw new IllegalArgumentException("There is no validator for " + originalKey);
    }
    else {
      return dynamicValidator;
    }
  }
}
