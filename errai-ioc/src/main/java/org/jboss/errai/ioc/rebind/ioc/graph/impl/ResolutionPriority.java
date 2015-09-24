package org.jboss.errai.ioc.rebind.ioc.graph.impl;

import java.util.Arrays;
import java.util.Collection;

import javax.enterprise.inject.Alternative;

import org.jboss.errai.ioc.client.api.IOCProvider;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder.InjectableType;
import org.jboss.errai.ioc.rebind.ioc.graph.api.Injectable;
import org.jboss.errai.ioc.rebind.ioc.injector.api.WiringElementType;

/**
 * An enum with priority categories for resolved dependencies. The enum values
 * are in descending order of priority.
 *
 * When the {@link DependencyGraphBuilder} resolves dependencies, injectables
 * will be filtered by priority and only injectables in the highest non-empty
 * priority will be considered.
 *
 * @see DependencyGraphBuilder#createGraph(boolean)
 * @author Max Barkley <mbarkley@redhat.com>
 */
public enum ResolutionPriority {
  /**
   * Category for {@link Alternative} beans that have been enabled.
   */
  EnabledAlternative {
    @Override
    public boolean matches(final Injectable injectable) {
      return injectable.getWiringElementTypes().contains(WiringElementType.AlternativeBean);
    }
  },
  /**
   * Category for explicitly scoped concrete types, or producer methods.
   */
  NormalType {
    final Collection<InjectableType> matchingTypes = Arrays.<InjectableType>asList(InjectableType.Type, InjectableType.Producer, InjectableType.JsType);
    @Override
    public boolean matches(final Injectable injectable) {
      return matchingTypes.contains(injectable.getInjectableType()) && !injectable.getWiringElementTypes().contains(WiringElementType.Simpleton);
    }
  },
  /**
   * Category for types form {@link IOCProvider providers}.
   */
  Provided {
    private final Collection<InjectableType> providerTypes = Arrays.<InjectableType>asList(InjectableType.Provider, InjectableType.ContextualProvider);
    @Override
    public boolean matches(final Injectable injectable) {
      return providerTypes.contains(injectable.getInjectableType());
    }
  },
  /**
   * Category for injectables provided by
   * {@link DependencyGraphBuilder#addExtensionInjectable(org.jboss.errai.codegen.meta.MetaClass, org.jboss.errai.ioc.rebind.ioc.graph.api.Qualifier, Class, WiringElementType...)
   * extensions}.
   */
  Extension {
    private final Collection<InjectableType> extensionTypes = Arrays.asList(InjectableType.Extension, InjectableType.ExtensionProvided);
    @Override
    public boolean matches(Injectable injectable) {
      return extensionTypes.contains(injectable.getInjectableType());
    }
  },
  /**
   * Category for concrete types with no explicity scopes or injection points, and that are default constructible.
   */
  Simpleton {
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