/*
 * Copyright 2013 JBoss, by Red Hat, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.errai.ioc.rebind.ioc.injector;

import java.util.HashMap;
import java.util.Map;

import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaClassMember;
import org.jboss.errai.codegen.meta.MetaParameterizedType;
import org.jboss.errai.ioc.rebind.ioc.injector.api.ContextualProviderInjectorProducer;
import org.jboss.errai.ioc.rebind.ioc.injector.api.InjectableInstance;
import org.jboss.errai.ioc.rebind.ioc.injector.api.InjectionContext;
import org.jboss.errai.ioc.rebind.ioc.injector.api.InjectorProducer;
import org.jboss.errai.ioc.rebind.ioc.injector.api.ProducerInjectorProducer;
import org.jboss.errai.ioc.rebind.ioc.injector.api.ProviderInjectorProducer;
import org.jboss.errai.ioc.rebind.ioc.injector.api.QualifiedTypeInjectorProducer;
import org.jboss.errai.ioc.rebind.ioc.injector.api.TypeInjectorProducer;
import org.jboss.errai.ioc.rebind.ioc.injector.api.WiringElementType;
import org.jboss.errai.ioc.rebind.ioc.injector.async.AsyncContextualProviderInjectorProducer;
import org.jboss.errai.ioc.rebind.ioc.injector.async.AsyncProducerInjectorProducer;
import org.jboss.errai.ioc.rebind.ioc.injector.async.AsyncProviderInjectorProducer;
import org.jboss.errai.ioc.rebind.ioc.injector.async.AsyncQualifiedTypeInjectorProducer;
import org.jboss.errai.ioc.rebind.ioc.injector.async.AsyncTypeInjectorProducer;
import org.jboss.errai.ioc.rebind.ioc.injector.basic.SyncContextualProviderInjectorProducer;
import org.jboss.errai.ioc.rebind.ioc.injector.basic.SyncProducerInjectorProducer;
import org.jboss.errai.ioc.rebind.ioc.injector.basic.SyncProviderInjectorProducer;
import org.jboss.errai.ioc.rebind.ioc.injector.basic.SyncQualifiedTypeInjectorProducer;
import org.jboss.errai.ioc.rebind.ioc.injector.basic.SyncTypeInjectorProducer;

/**
 * @author Mike Brock
 */
public class InjectorFactory {
  private final Map<BootstrapType, Map<WiringElementType, InjectorProducer>> injectors
      = new HashMap<BootstrapType, Map<WiringElementType, InjectorProducer>>();

  private final boolean async;

  public InjectorFactory(final boolean async) {
    this.async = async;

    addInjector(BootstrapType.Synchronous, WiringElementType.Type, new SyncTypeInjectorProducer());
    addInjector(BootstrapType.Synchronous, WiringElementType.ProducerElement, new SyncProducerInjectorProducer());
    addInjector(BootstrapType.Synchronous, WiringElementType.TopLevelProvider, new SyncProviderInjectorProducer());
    addInjector(BootstrapType.Synchronous, WiringElementType.ContextualTopLevelProvider, new SyncContextualProviderInjectorProducer());
    addInjector(BootstrapType.Synchronous, WiringElementType.QualifiyingType, new SyncQualifiedTypeInjectorProducer());

    addInjector(BootstrapType.Asynchronous, WiringElementType.Type, new AsyncTypeInjectorProducer());
    addInjector(BootstrapType.Asynchronous, WiringElementType.ProducerElement, new AsyncProducerInjectorProducer());
    addInjector(BootstrapType.Asynchronous, WiringElementType.TopLevelProvider, new AsyncProviderInjectorProducer());
    addInjector(BootstrapType.Asynchronous, WiringElementType.ContextualTopLevelProvider, new AsyncContextualProviderInjectorProducer());
    addInjector(BootstrapType.Asynchronous, WiringElementType.QualifiyingType, new AsyncQualifiedTypeInjectorProducer());
  }

  private BootstrapType getDefaultBootstrapType() {
    return async ? BootstrapType.Asynchronous : BootstrapType.Synchronous;
  }

  public Injector getTypeInjector(final MetaClass type,
                                  final InjectionContext context) {
    return getTypeInjector(getDefaultBootstrapType(), type, context);
  }

  private Injector getTypeInjector(final BootstrapType bootstrapType,
                                   final MetaClass type,
                                   final InjectionContext context) {
    final InjectorProducer injectorProducer = injectors.get(bootstrapType).get(WiringElementType.Type);

    return TypeInjectorProducer.class.cast(injectorProducer).create(type, context);
  }

  public Injector getProviderInjector(final MetaClass type,
                                      final MetaClass providerType,
                                      final InjectionContext context) {
    return getProviderInjector(getDefaultBootstrapType(), type, providerType, context);
  }

  public Injector getProviderInjector(final BootstrapType bootstrapType,
                                      final MetaClass type,
                                      final MetaClass providerType,
                                      final InjectionContext context) {
    final InjectorProducer injectorProducer = injectors.get(bootstrapType).get(WiringElementType.TopLevelProvider);

    return ProviderInjectorProducer.class.cast(injectorProducer).create(type, providerType, context);
  }

  public Injector getContextualProviderInjector(final MetaClass type,
                                                final MetaClass providerType,
                                                final InjectionContext context) {
    return getContextualProviderInjector(getDefaultBootstrapType(), type, providerType, context);
  }

  public Injector getContextualProviderInjector(final BootstrapType bootstrapType,
                                                final MetaClass type,
                                                final MetaClass providerType,
                                                final InjectionContext context) {
    final InjectorProducer injectorProducer = injectors.get(bootstrapType).get(WiringElementType.ContextualTopLevelProvider);

    return ContextualProviderInjectorProducer.class.cast(injectorProducer).create(type, providerType, context);
  }

  public Injector getProducerInjector(final MetaClass type,
                                      final MetaClassMember providerType,
                                      final InjectableInstance injectableInstance) {
    return getProducerInjector(getDefaultBootstrapType(),
        type, providerType, injectableInstance);
  }


  public Injector getProducerInjector(final BootstrapType bootstrapType,
                                      final MetaClass type,
                                      final MetaClassMember providerType,
                                      final InjectableInstance injectableInstance) {
    final InjectorProducer injectorProducer = injectors.get(bootstrapType).get(WiringElementType.ProducerElement);

    return ProducerInjectorProducer.class.cast(injectorProducer).create(type, providerType, injectableInstance);
  }

  public Injector getQualifyingTypeInjector(final MetaClass type,
                                            final Injector delegate,
                                            final MetaParameterizedType metaParameterizedType) {

    return getQualifyingTypeInjector(getDefaultBootstrapType(), type, delegate, metaParameterizedType);
  }

  public Injector getQualifyingTypeInjector(final BootstrapType bootstrapType,
                                            final MetaClass type,
                                            final Injector delegate,
                                            final MetaParameterizedType metaParameterizedType) {

    final InjectorProducer injectorProducer = injectors.get(bootstrapType).get(WiringElementType.QualifiyingType);

    return QualifiedTypeInjectorProducer.class.cast(injectorProducer).create(type, delegate, metaParameterizedType);
  }


  private <PRODUCER extends InjectorProducer> void addInjector(final BootstrapType type,
                           final WiringElementType elementType,
                           final PRODUCER injectorProducer) {

    Map<WiringElementType, InjectorProducer> wiringElementTypeClassMap = injectors.get(type);
    if (wiringElementTypeClassMap == null) {
      wiringElementTypeClassMap = new HashMap<WiringElementType, InjectorProducer>();
      injectors.put(type, wiringElementTypeClassMap);
    }

    wiringElementTypeClassMap.put(elementType, injectorProducer);
  }
}
