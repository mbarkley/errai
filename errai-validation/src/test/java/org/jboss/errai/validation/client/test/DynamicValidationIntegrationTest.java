/*
 * Copyright (C) 2011 Red Hat, Inc. and/or its affiliates.
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

package org.jboss.errai.validation.client.test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Singleton;
import javax.validation.ConstraintViolation;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.Size;

import org.jboss.errai.ioc.client.container.IOC;
import org.jboss.errai.ioc.client.container.SyncBeanDef;
import org.jboss.errai.ioc.client.test.AbstractErraiIOCTest;
import org.jboss.errai.validation.client.dynamic.DynamicValidator;

/**
 * @author Max Barkley <mbarkley@redhat.com>
 */
public class DynamicValidationIntegrationTest extends AbstractErraiIOCTest {

  @Override
  public String getModuleName() {
    return "org.jboss.errai.validation.ValidationTestModule";
  }

  @Override
  protected void gwtSetUp() throws Exception {
    super.gwtSetUp();
  }

  public void testLookupDynamicValidatorWithIoc() throws Exception {
    final SyncBeanDef<DynamicValidator> beanDef = IOC.getBeanManager().lookupBean(DynamicValidator.class);
    assertEquals(Singleton.class, beanDef.getScope());
    final DynamicValidator validator = beanDef.getInstance();
    assertNotNull(validator);
    assertSame(validator, beanDef.getInstance());
  }

  public void testValidateMaxStringValidator() throws Exception {
    final DynamicValidator validator = IOC.getBeanManager().lookupBean(DynamicValidator.class).getInstance();
    final Set<ConstraintViolation<String>> validResult = validator.validate(Max.class,
            Collections.singletonMap("value", 100L), "99");
    assertTrue(validResult.toString(), validResult.isEmpty());
    final Set<ConstraintViolation<String>> invalidResult = validator.validate(Max.class,
            Collections.singletonMap("value", 100L), "101");
    assertEquals(1, invalidResult.size());
  }

  public void testValidationWithMultipleParams() throws Exception {
    final Map<String, Object> params = new HashMap<>();
    params.put("integer", 3);
    params.put("fraction", 3);
    final DynamicValidator validator = IOC.getBeanManager().lookupBean(DynamicValidator.class).getInstance();
    final Set<ConstraintViolation<String>> validResult = validator.validate(Digits.class, params, "123.123");
    assertTrue(validResult.isEmpty());
    final Set<ConstraintViolation<String>> invalidResult = validator.validate(Digits.class, params, "1234.123");
    assertEquals(1, invalidResult.size());
  }

  public void testValidateSeveralTypesForSameConstraintAndParams() throws Exception {
    final DynamicValidator validator = IOC.getBeanManager().lookupBean(DynamicValidator.class).getInstance();
    final Map<String, Object> params = new HashMap<>();
    params.put("min", 1);
    params.put("max", 1);

    assertFalse(validator.validate(Size.class, params, Collections.emptyList()).isEmpty());
    assertTrue(validator.validate(Size.class, params, Collections.singleton(new Object())).isEmpty());

    assertFalse(validator.validate(Size.class, params, "").isEmpty());
    assertTrue(validator.validate(Size.class, params, "a").isEmpty());

    assertFalse(validator.validate(Size.class, params, new int[0]).isEmpty());
    assertTrue(validator.validate(Size.class, params, new int[] { 1 }).isEmpty());

    assertFalse(validator.validate(Size.class, params, new Integer[0]).isEmpty());
    assertTrue(validator.validate(Size.class, params, new Integer[] { 1 }).isEmpty());
  }
}
