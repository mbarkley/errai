package org.jboss.errai.validation.client.dynamic;

import java.util.Map;
import java.util.Set;

import javax.validation.ConstraintViolation;

/**
 * 
 * @author Christian Sadilek <csadilek@redhat.com>
 */
@SuppressWarnings("unchecked")
public class DynamicValidator {
  
  // populated at bootstrap time by generated code...
  Map<DynamicValidatorKey, GeneratedDynamicValidator<?>> validators; 
  
  private static DynamicValidator instance  = new DynamicValidator();
  
  private DynamicValidator() {};
  
  public static DynamicValidator getInstance() {
    return instance;
  }

  // not throw NPE, but a reasonable exception if no validator is found for this constraint
  public <T> Set<ConstraintViolation<T>> validate(String constraint, Map<String, String> parameters, T value) {
    final GeneratedDynamicValidator<T> dynamicValidator = getValidator(constraint, value);
    return dynamicValidator.validate(parameters, value);
  }

  public <T> Set<ConstraintViolation<T>> validate(String constraint, Map<String, String> parameters, T value, String message) {
    final GeneratedDynamicValidator<T> dynamicValidator = getValidator(constraint, value);
    return dynamicValidator.validate(parameters, value, message);
  }

  private <T> GeneratedDynamicValidator<T> getValidator(String constraint, T value) {
    final DynamicValidatorKey key = new DynamicValidatorKey(constraint, value.getClass().getName());
    final GeneratedDynamicValidator<T> dynamicValidator = (GeneratedDynamicValidator<T>) validators.get(key);
    return dynamicValidator;
  }
}
