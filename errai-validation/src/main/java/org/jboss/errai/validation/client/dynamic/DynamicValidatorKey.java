package org.jboss.errai.validation.client.dynamic;

public class DynamicValidatorKey {

  private String constraint;
  private String valueType;

  public DynamicValidatorKey(String constraint, String valueType) {
    this.constraint = constraint;
    this.valueType = valueType;
  }

  public String getConstraint() {
    return constraint;
  }

  public void setConstraint(String constraint) {
    this.constraint = constraint;
  }

  public String getValueType() {
    return valueType;
  }

  public void setValueType(String valueType) {
    this.valueType = valueType;
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

}
