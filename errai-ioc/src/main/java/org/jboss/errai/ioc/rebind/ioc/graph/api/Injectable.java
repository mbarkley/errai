package org.jboss.errai.ioc.rebind.ioc.graph.api;

import java.lang.annotation.Annotation;
import java.util.Collection;

import javax.enterprise.context.Dependent;
import javax.inject.Named;

import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.ioc.client.api.ContextualTypeProvider;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder.Dependency;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder.InjectableType;
import org.jboss.errai.ioc.rebind.ioc.graph.impl.InjectableHandle;
import org.jboss.errai.ioc.rebind.ioc.injector.api.WiringElementType;

/**
 * Represents a class, producer member, or provider that can supply a bean for
 * satisfying dependencies.
 *
 * @see DependencyGraphBuilder
 * @author Max Barkley <mbarkley@redhat.com>
 */
public interface Injectable {

  /**
   * @return A handle that can be used for looking up this injectable.
   */
  InjectableHandle getHandle();

  /**
   * @return The class of the injectable.
   */
  MetaClass getInjectedType();

  /**
   * @return The scope of the injectable. For pseudo-dependent injectables, this
   *         should return {@link Dependent}.
   */
  Class<? extends Annotation> getScope();

  /**
   * @return The name of this injectable if {@link Named} annotation was
   *         present.
   */
  String getBeanName();

  /**
   * @return The qualifier of this injectable.
   */
  Qualifier getQualifier();

  /**
   * @return The unique name of the factory that will produce this injectable at
   *         runtime.
   */
  String getFactoryName();

  /**
   * @return The dependencies of this injectable. These depencies will not be
   *         resolved until after
   *         {@link DependencyGraphBuilder#createGraph(boolean)} is called.
   */
  Collection<Dependency> getDependencies();

  /**
   * @return The kind of this injectable.
   */
  InjectableType getInjectableType();

  /**
   * @return The wiring element types of this injectable.
   */
  Collection<WiringElementType> getWiringElementTypes();

  /**
   * @return True if this injectable requires a proxy because of its scope or
   *         because it is injected into a constructor.
   */
  boolean requiresProxy();

  /**
   * Once invoked, all subsequent calls to {@link #requiresProxy()} will return
   * true. Usually this is called by the {@link DependencyGraphBuilder} if this
   * injectable satisfies a constructor injection point.
   */
  void setRequiresProxyTrue();

  /**
   * Convenience method that checks if this injectable has the ContextualProvider {@link WiringElementType}.
   *
   * @return True if this injectable is for a {@link ContextualTypeProvider}.
   */
  boolean isContextual();

  /**
   * @return True if this injectable was created via {@link DependencyGraphBuilder#addExtensionInjectable(MetaClass, Qualifier, Class, WiringElementType...)}.
   */
  boolean isExtension();

  /**
   * @return A hashcode based on the following:
   *    <ul>
   *      <li>The {@link MetaClass#hashContent() hashContent} of the {@link MetaClass} produced by this injectable.
   *      <il>The hashContent of the MetaClasses of all dependencies.
   *    </ul>
   */
  int hashContent();

}
