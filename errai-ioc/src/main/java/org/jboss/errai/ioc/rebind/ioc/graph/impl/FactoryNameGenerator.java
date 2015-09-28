package org.jboss.errai.ioc.rebind.ioc.graph.impl;

import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder.InjectableType;
import org.jboss.errai.ioc.rebind.ioc.graph.api.Qualifier;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

public class FactoryNameGenerator {

  private final Multiset<String> allFactoryNames = HashMultiset.create();

  public String generateFor(final MetaClass type, final Qualifier qualifier, final InjectableType injectableType) {
    final String typeName = type.getFullyQualifiedName().replace('.', '_').replace('$', '_');
    final String qualNames = qualifier.getIdentifierSafeString();
    String factoryName;
    if (DependencyGraphBuilder.SHORT_NAMES) {
      factoryName = injectableType + "_factory__" + shorten(typeName) + "__quals__" + shorten(qualNames);

    } else {
      factoryName = injectableType + "_factory_for__" + typeName + "__with_qualifiers__" + qualNames;
    }
    final int collisions = allFactoryNames.count(factoryName);
    allFactoryNames.add(factoryName);
    if (collisions > 0) {
      factoryName = factoryName + "_" + String.valueOf(collisions);
    }

    return factoryName;
  }

  private String shorten(final String compoundName) {
    final String[] names = compoundName.split("__");
    final StringBuilder builder = new StringBuilder();
    for (final String name : names) {
      builder.append(shortenName(name)).append('_');
    }
    builder.delete(builder.length() - 1, builder.length());

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
      }
      else {
        builder.append(part.charAt(0));
      }
      builder.append('_');
    }
    builder.delete(builder.length() - 1, builder.length());

    return builder.toString();
  }

}
