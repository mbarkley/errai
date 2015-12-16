/*
 * Copyright (C) 2011 Red Hat, Inc. and/or its affiliates.
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

package org.jboss.errai.ui.rebind;

import static org.jboss.errai.codegen.Parameter.finalOf;
import static org.jboss.errai.codegen.util.Stmt.loadVariable;

import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.Dependent;

import org.jboss.errai.codegen.builder.BlockBuilder;
import org.jboss.errai.codegen.builder.ClassStructureBuilder;
import org.jboss.errai.codegen.builder.impl.ClassBuilder;
import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaClassFactory;
import org.jboss.errai.codegen.meta.MetaField;
import org.jboss.errai.codegen.meta.MetaMethod;
import org.jboss.errai.codegen.meta.impl.build.BuildMetaClass;
import org.jboss.errai.config.rebind.MetaClassBridgeUtil;
import org.jboss.errai.ioc.rebind.ioc.bootstrapper.AbstractBodyGenerator;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder.Dependency;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder.InjectableType;
import org.jboss.errai.ioc.rebind.ioc.graph.api.Injectable;
import org.jboss.errai.ioc.rebind.ioc.graph.api.Qualifier;
import org.jboss.errai.ioc.rebind.ioc.graph.impl.DefaultQualifierFactory;
import org.jboss.errai.ioc.rebind.ioc.graph.impl.InjectableHandle;
import org.jboss.errai.ioc.rebind.ioc.injector.api.Decorable;
import org.jboss.errai.ioc.rebind.ioc.injector.api.Decorable.DecorableType;
import org.jboss.errai.ioc.rebind.ioc.injector.api.FactoryController;
import org.jboss.errai.ioc.rebind.ioc.injector.api.WiringElementType;
import org.jboss.errai.ui.shared.api.annotations.DataField;
import org.jboss.errai.ui.shared.api.annotations.Templated;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;

/**
 *
 * @author Max Barkley <mbarkley@redhat.com>
 */
public class TemplateProcessorGenerator extends Generator {

  @Override
  public String generate(final TreeLogger logger, final GeneratorContext context, final String typeName)
          throws UnableToCompleteException {
    MetaClassBridgeUtil.populateMetaClassFactoryFromTypeOracle(context, logger);
    final MetaClass processorType = getProcessorType(typeName, context);
    final MetaClass processedType = getProcessedType(processorType, typeName);

    final ClassStructureBuilder<?> classBody = ClassBuilder
            .define("org.jboss.errai.ui.client.ProcessorFor" + typeName.substring(typeName.lastIndexOf('.') + 1),
                    processorType)
            .publicScope().body();
    final BuildMetaClass classDefinition = classBody.getClassDefinition();
    final BlockBuilder<?> methodBody = classBody.publicMethod(processedType, "process", finalOf(processedType, "instance")).body();

    final DataFieldCodeDecorator dataFieldCodeDecorator = new DataFieldCodeDecorator(DataField.class);
    final TemplatedCodeDecorator templatedCodeDecorator = new TemplatedCodeDecorator(Templated.class);

    final Injectable injectable = createInjectable(processedType, classDefinition);
    final FactoryController controller = createFactoryController(processedType, classDefinition);
    final Map<String, Object> attributes = new HashMap<String, Object>();

    final List<Decorable> dataFieldDecorables = createDataFieldDecorables(processedType, classDefinition, injectable, attributes);
    for (final Decorable decorable : dataFieldDecorables) {
      dataFieldCodeDecorator.generateDecorator(decorable, controller);
    }
    templatedCodeDecorator.generateDecorator(createTemplatedDecorable(processedType, classDefinition, injectable, attributes), controller);

    addAccessors(controller, classBody);

    methodBody.appendAll(controller.getInitializationStatements())
              .append(loadVariable("instance").returnValue()).finish();

    final String generated = classBody.toJavaString();
    final PrintWriter pw = context.tryCreate(logger, classDefinition.getPackageName(), classDefinition.getName());
    pw.append(generated);
    context.commit(logger, pw);

    return classDefinition.getFullyQualifiedName();
  }

  private MetaClass getProcessorType(String typeName, final GeneratorContext context) {
    final boolean member = context.getTypeOracle().findType(typeName).isMemberType();
    if (member) {
      final int lastDotIndex = typeName.lastIndexOf('.');
      typeName = typeName.substring(0, lastDotIndex) + "$" + typeName.substring(lastDotIndex+1);
    }

    return MetaClassFactory.get(typeName);
  }

  private List<Decorable> createDataFieldDecorables(final MetaClass processedType, final BuildMetaClass classDefinition,
          final Injectable injectable, final Map<String, Object> attributes) {
    final List<Decorable> decorables = new ArrayList<Decorable>();
    final List<MetaField> dataFields = processedType.getFieldsAnnotatedWith(DataField.class);

    for (final MetaField dataField : dataFields) {
      decorables.add(new Decorable(dataField, dataField.getAnnotation(DataField.class), DecorableType.FIELD,
              attributes, null, classDefinition, injectable));
    }

    return decorables;
  }

  private Decorable createTemplatedDecorable(final MetaClass processedType, final BuildMetaClass classDefinition,
          final Injectable injectable, final Map<String, Object> attributes) {
    return new Decorable(processedType, processedType.getAnnotation(Templated.class), DecorableType.TYPE,
            attributes, null, classDefinition, injectable);
  }

  private void addAccessors(final FactoryController controller, final ClassStructureBuilder<?> classBody) {
    AbstractBodyGenerator.addPrivateAccessors(controller, classBody);
  }

  private FactoryController createFactoryController(final MetaClass processedType, final BuildMetaClass processorClassDefinition) {
    return new FactoryController(processedType, processorClassDefinition.getFullyQualifiedName(), processorClassDefinition);
  }

  private MetaClass getProcessedType(final MetaClass processorType, final String typeName) {
    for (final MetaMethod meth : processorType.getMethods()) {
      if (meth.getName().equals("process") && (meth.asMethod() == null || !meth.asMethod().isBridge())) {
        return meth.getReturnType();
      }
    }

    throw new RuntimeException("Could not find process method in type " + typeName);
  }

  private Injectable createInjectable(final MetaClass processedType, final BuildMetaClass classDefinition) {
    return new Injectable() {

      private final InjectableHandle handle = new InjectableHandle(processedType, new DefaultQualifierFactory().forUniversallyQualified());

      @Override
      public InjectableHandle getHandle() {
        return handle;
      }

      @Override
      public MetaClass getInjectedType() {
        return processedType;
      }

      @Override
      public Class<? extends Annotation> getScope() {
        return Dependent.class;
      }

      @Override
      public String getBeanName() {
        return null;
      }

      @Override
      public Qualifier getQualifier() {
        return handle.getQualifier();
      }

      @Override
      public String getFactoryName() {
        return classDefinition.getName();
      }

      @Override
      public Collection<Dependency> getDependencies() {
        return Collections.emptyList();
      }

      @Override
      public InjectableType getInjectableType() {
        return InjectableType.Type;
      }

      @Override
      public Collection<WiringElementType> getWiringElementTypes() {
        return Collections.singletonList(WiringElementType.DependentBean);
      }

      @Override
      public boolean loadAsync() {
        return false;
      }

      @Override
      public boolean requiresProxy() {
        return false;
      }

      @Override
      public void setRequiresProxyTrue() {
      }

      @Override
      public boolean isContextual() {
        return false;
      }

      @Override
      public boolean isExtension() {
        return false;
      }

      @Override
      public int hashContent() {
        return processedType.hashContent();
      }

    };
  }

}
