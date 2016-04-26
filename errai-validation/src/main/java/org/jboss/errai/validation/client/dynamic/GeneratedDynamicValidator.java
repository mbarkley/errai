package org.jboss.errai.validation.client.dynamic;

import java.util.Map;
import java.util.Set;

import javax.validation.ConstraintViolation;

/**
 * 
 * @author Christian Sadilek <csadilek@redhat.com>
 */
public interface GeneratedDynamicValidator<T> {

  Set<ConstraintViolation<T>> validate(Map<String, String> parameters, T value);
  
  Set<ConstraintViolation<T>> validate(Map<String, String> parameters, T value, String messageTemplate);
}
