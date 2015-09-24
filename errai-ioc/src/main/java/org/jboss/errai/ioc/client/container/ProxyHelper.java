package org.jboss.errai.ioc.client.container;

/**
 * For any {@link Proxy}, all proxied methods should dispatch to the instance
 * returned by {@link #getInstance(Proxy)}. It is the helpers job to load
 * an instance for the proxy on demand.
 *
 * @param <T>
 *          The type wrapped by the proxy containing this helper.
 *
 * @author Max Barkley <mbarkley@redhat.com>
 */
public interface ProxyHelper<T> {

  /**
   * @param instance The instance to be returened by future calls to {@link #getInstance(Proxy)}.
   */
  void setInstance(T instance);

  /**
   * @param proxy The proxy containing this helper.
   * @return The instance that the containing proxy should dispatch to.
   */
  T getInstance(Proxy<T> proxy);

  /**
   * Removes the stored proxied reference so that future calls to
   * {@link #getInstance(Proxy)} will need to load a new instance.
   */
  void clearInstance();

  /**
   * Required for loading instances on demand.
   *
   * @param context The context associated with the containing {@link Proxy}.
   */
  void setContext(Context context);

  /**
   * @return The {@link Context} previously set by {@link #setContext(Context)}.
   */
  Context getContext();
}
