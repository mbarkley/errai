package org.jboss.errai.ioc.rebind.ioc.injector.async;

import org.jboss.errai.codegen.Statement;
import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaParameterizedType;
import org.jboss.errai.ioc.rebind.ioc.injector.Injector;
import org.jboss.errai.ioc.rebind.ioc.injector.api.InjectionContext;
import org.jboss.errai.ioc.rebind.ioc.injector.basic.QualifiedTypeInjectorDelegate;

/**
 * @author Mike Brock
 */
public class AsyncQualifiedTypeInjectorDelegate extends QualifiedTypeInjectorDelegate {
  public AsyncQualifiedTypeInjectorDelegate(final MetaClass type, final Injector delegate, final MetaParameterizedType parameterizedType) {
    super(type, delegate, parameterizedType);
  }

  @Override
  public void registerWithBeanManager(final InjectionContext context, final Statement valueRef) {
    throw new RuntimeException("To be removed.");
  }

  @Override
  public MetaClass getConcreteInjectedType() {
    Injector inj = delegate;
    while (inj instanceof AsyncQualifiedTypeInjectorDelegate) {
      inj = ((QualifiedTypeInjectorDelegate) inj).getDelegate();
    }
    return inj.getInjectedType();
  }

}
