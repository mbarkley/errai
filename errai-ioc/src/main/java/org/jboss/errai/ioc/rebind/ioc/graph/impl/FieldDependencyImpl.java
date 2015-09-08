package org.jboss.errai.ioc.rebind.ioc.graph.impl;

import org.jboss.errai.codegen.meta.MetaField;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder.DependencyType;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder.FieldDependency;

class FieldDependencyImpl extends BaseDependency implements FieldDependency {

  final MetaField field;

  FieldDependencyImpl(final AbstractInjectable abstractInjectable, final MetaField field) {
    super(abstractInjectable, DependencyType.Field);
    this.field = field;
  }

  @Override
  public MetaField getField() {
    return field;
  }

}