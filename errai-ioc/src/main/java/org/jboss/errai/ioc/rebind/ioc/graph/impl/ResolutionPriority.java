package org.jboss.errai.ioc.rebind.ioc.graph.impl;

import java.util.Arrays;
import java.util.Collection;

import org.jboss.errai.ioc.rebind.ioc.graph.api.Injectable;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder.InjectableType;
import org.jboss.errai.ioc.rebind.ioc.injector.api.WiringElementType;

enum ResolutionPriority {
  /*
   * From highest to lowest priority.
   */
  EnabledAlternative {
    @Override
    public boolean matches(final Injectable injectable) {
      return injectable.getWiringElementTypes().contains(WiringElementType.AlternativeBean);
    }
  }, NormalType {
    final Collection<InjectableType> matchingTypes = Arrays.<InjectableType>asList(InjectableType.Type, InjectableType.Producer, InjectableType.JsType);
    @Override
    public boolean matches(final Injectable injectable) {
      return matchingTypes.contains(injectable.getInjectableType()) && !injectable.getWiringElementTypes().contains(WiringElementType.Simpleton);
    }
  }, Provided {
    private final Collection<InjectableType> providerTypes = Arrays.<InjectableType>asList(InjectableType.Provider, InjectableType.ContextualProvider);
    @Override
    public boolean matches(final Injectable injectable) {
      return providerTypes.contains(injectable.getInjectableType());
    }
  }, TransientOrExtension {
    private final Collection<InjectableType> extensionTypes = Arrays.asList(InjectableType.Extension, InjectableType.ExtensionProvided);
    @Override
    public boolean matches(Injectable injectable) {
      return extensionTypes.contains(injectable.getInjectableType());
    }
  }, Simpleton {
    @Override
    public boolean matches(final Injectable injectable) {
      return injectable.getWiringElementTypes().contains(WiringElementType.Simpleton);
    }
  };

  public abstract boolean matches(final Injectable injectable);

  public static ResolutionPriority getMatchingPriority(final Injectable injectable) {
    for (final ResolutionPriority priority : values()) {
      if (priority.matches(injectable)) {
        return priority;
      }
    }

    throw new RuntimeException("The injectable " + injectable + " does not match any resolution priority.");
  }
}