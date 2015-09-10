package org.jboss.errai.ioc.rebind.ioc.graph.impl;

import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder;
import org.jboss.errai.ioc.rebind.ioc.graph.api.Injectable;
import org.jboss.errai.ioc.rebind.ioc.graph.api.Qualifier;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

abstract class BaseInjectable implements Injectable {
    private static final Multiset<String> allFactoryNames = HashMultiset.create();

    final MetaClass type;
    Qualifier qualifier;
    String factoryName;

    BaseInjectable(final MetaClass type, final Qualifier qualifier) {
      this.type = type;
      this.qualifier = qualifier;
    }

    @Override
    public String getBeanName() {
      final String name = qualifier.getName();
      if (name == null) {
        return type.getName();
      } else {
        return name;
      }
    }

    @Override
    public MetaClass getInjectedType() {
      return type;
    }

    @Override
    public String toString() {
      return "class=" + type + ", injectorType=" + getInjectableType() + ", qualifier=" + qualifier.toString();
    }

    @Override
    public Qualifier getQualifier() {
      return qualifier;
    }

    @Override
    public String getFactoryName() {
      if (factoryName == null) {
        final String typeName = type.getFullyQualifiedName().replace('.', '_').replace('$', '_');
        final String qualNames = qualifier.getIdentifierSafeString();
        if (DependencyGraphBuilder.SHORT_NAMES) {
          factoryName = getInjectableType() + "_factory__" + shorten(typeName) + "__quals__" + shorten(qualNames);

        } else {
          factoryName = getInjectableType() + "_factory_for__" + typeName + "__with_qualifiers__" + qualNames;
        }
        final int collisions = allFactoryNames.count(factoryName);
        allFactoryNames.add(factoryName);
        if (collisions > 0) {
          factoryName = factoryName + "_" + String.valueOf(collisions);
        }
      }

      return factoryName;
    }

    private String shorten(final String compoundName) {
      final String[] names = compoundName.split("__");
      final StringBuilder builder = new StringBuilder();
      for (final String name : names) {
        builder.append(shortenName(name))
               .append('_');
      }
      builder.delete(builder.length()-1, builder.length());

      return builder.toString();
    }

    private String shortenName(final String name) {
      final String[] parts = name.split("_");
      final StringBuilder builder = new StringBuilder();
      boolean haveSeenUpperCase = false;
      for (final String part : parts) {
        if (haveSeenUpperCase || Character.isUpperCase(part.charAt(0))) {
          builder.append(part);
          haveSeenUpperCase = true;
        } else {
          builder.append(part.charAt(0));
        }
        builder.append('_');
      }
      builder.delete(builder.length()-1, builder.length());

      return builder.toString();
    }

    @Override
    public InjectableHandle getHandle() {
      return new InjectableHandle(type, qualifier);
    }
 }