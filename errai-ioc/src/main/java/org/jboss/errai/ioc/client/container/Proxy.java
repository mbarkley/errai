package org.jboss.errai.ioc.client.container;

/**
 * Normal scoped beans or dependent scoped beans decorated with AOP features
 * will be wrapped in proxies. All proxies produced by a {@link Factory} must
 * implement this interface.
 *
 * @param <T> The type of the instance being proxied.
 *
 * @author Max Barkley <mbarkley@redhat.com>
 */
public interface Proxy<T> {

  /**
   * @return Returns this proxy as the type of the instance it is proxying.
   */
  T asBeanType();

  /**
   * Set the instance that this proxy dispatches to (i.e. set the proxied instance).
   *
   * @param instance An instance created by the same factory as this proxy.
   */
  void setInstance(T instance);

  /**
   * Removes the reference to any instance that was previously proxied (via {@link #setInstance(Object)}).
   */
  void clearInstance();

  /**
   * This is called after a proxy is created so that the proxy can request a proxied instance on demand.
   *
   * @param context The context associated with the {@link Factory} that created this proxy.
   */
  void setContext(Context context);

  /**
   * If no proxied instance has yet been set, this method will request and instance from the {@link Context} and {@link #setInstance(Object) set} it.
   *
   * @return The instance wrapped by this {@link Proxy}.
   */
  T unwrappedInstance();

  /**
   * Called once after {@link #setInstance(Object)} is called.
   *
   * @param instance The instance wrapped by this proxy.
   */
  void initProxyProperties(final T instance);

}
