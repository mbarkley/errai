/**
 * Copyright (C) 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.errai.validation.rebind;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Singleton;
import javax.validation.ConstraintValidator;

import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaClassFactory;
import org.jboss.errai.ioc.client.api.IOCExtension;
import org.jboss.errai.ioc.rebind.ioc.bootstrapper.IOCProcessingContext;
import org.jboss.errai.ioc.rebind.ioc.extension.IOCExtensionConfigurator;
import org.jboss.errai.ioc.rebind.ioc.graph.impl.DefaultCustomFactoryInjectable;
import org.jboss.errai.ioc.rebind.ioc.graph.impl.InjectableHandle;
import org.jboss.errai.ioc.rebind.ioc.injector.api.InjectionContext;
import org.jboss.errai.ioc.rebind.ioc.injector.api.WiringElementType;
import org.jboss.errai.validation.client.dynamic.DynamicValidator;

/**
 *
 * @author Max Barkley <mbarkley@redhat.com>
 */
@IOCExtension
public class DynamicValidatorExtension implements IOCExtensionConfigurator {

  public static final String DYNAMIC_VALIDATION_ENABLED_PROP = "errai.dynamic_validation.enabled";

  public static final boolean DYNAMIC_VALIDATION_ENABLED = Boolean.getBoolean(DYNAMIC_VALIDATION_ENABLED_PROP);

  private List<MetaClass> validators = new ArrayList<>();

  @Override
  public void configure(final IOCProcessingContext context, final InjectionContext injectionContext) {
  }

  @Override
  public void afterInitialization(final IOCProcessingContext context, final InjectionContext injectionContext) {
    if (DYNAMIC_VALIDATION_ENABLED) {
      injectionContext.registerExtensionTypeCallback(type -> {
        if (type.isConcrete() && type.isAssignableTo(ConstraintValidator.class)) {
          validators.add(type);
        }
      });

      final InjectableHandle handle = new InjectableHandle(MetaClassFactory.get(DynamicValidator.class),
              injectionContext.getQualifierFactory().forDefault());
      injectionContext.registerInjectableProvider(handle,
              (injectionSite, nameGenerator) -> new DefaultCustomFactoryInjectable(handle.getType(),
                      handle.getQualifier(), "DynamicValidatorFactory", Singleton.class,
                      Arrays.asList(WiringElementType.NormalScopedBean), new DynamicValidatorBodyGenerator(validators)));
    }
  }

}
