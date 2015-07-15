/*
 * Copyright 2011 JBoss, by Red Hat, Inc
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

package org.jboss.errai.ioc.rebind.ioc.bootstrapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.jboss.errai.codegen.Context;
import org.jboss.errai.codegen.Statement;
import org.jboss.errai.codegen.Variable;
import org.jboss.errai.codegen.VariableReference;
import org.jboss.errai.codegen.builder.BlockBuilder;
import org.jboss.errai.codegen.builder.ClassStructureBuilder;
import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.impl.build.BuildMetaClass;
import org.jboss.errai.common.client.api.Assert;
import org.jboss.errai.ioc.client.BootstrapInjectionContext;
import org.jboss.errai.ioc.client.container.CreationalContext;
import org.jboss.errai.ioc.rebind.ioc.injector.api.InjectableInstance;
import org.jboss.errai.ioc.rebind.ioc.injector.api.TypeDiscoveryListener;
import org.jboss.errai.ioc.rebind.ioc.metadata.JSR330QualifyingMetadataFactory;
import org.jboss.errai.ioc.rebind.ioc.metadata.QualifyingMetadataFactory;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;

/**
 * @author Mike Brock <cbrock@redhat.com>
 */
public class IOCProcessingContextImpl implements IOCProcessingContext {
  protected final Set<String> packages;

  protected final Context context;
  protected final BuildMetaClass bootstrapClass;
  protected final ClassStructureBuilder bootstrapBuilder;

  protected final Stack<BlockBuilder<?>> blockBuilder;

  protected final List<Statement> appendToEnd;
  protected final List<TypeDiscoveryListener> typeDiscoveryListeners;
  protected final Set<MetaClass> discovered = new HashSet<MetaClass>();

  protected final TreeLogger treeLogger;
  protected final GeneratorContext generatorContext;

  protected final Variable contextVariable;
  protected final Class<? extends BootstrapInjectionContext> bootstrapContextClass;
  protected final Class<? extends CreationalContext> cretionalContextClass;

  protected final QualifyingMetadataFactory qualifyingMetadataFactory;

  protected final boolean gwtTarget;

  private IOCProcessingContextImpl(final BuilderImpl builder) {
    this.treeLogger = builder.treeLogger;
    this.generatorContext = builder.generatorContext;
    this.context = builder.context;
    this.bootstrapClass = builder.bootstrapClassInstance;
    this.bootstrapBuilder = builder.bootstrapBuilder;

    this.blockBuilder = new Stack<BlockBuilder<?>>();
    this.blockBuilder.push(builder.blockBuilder);

    this.appendToEnd = new ArrayList<Statement>();
    this.typeDiscoveryListeners = new ArrayList<TypeDiscoveryListener>();
    this.packages = builder.packages;
    this.qualifyingMetadataFactory = builder.qualifyingMetadataFactory;
    this.gwtTarget = builder.gwtTarget;
    this.bootstrapContextClass = builder.bootstrapClass;
    this.cretionalContextClass = builder.creationalContextClass;
    this.contextVariable = Variable.create("injContext", bootstrapContextClass);
  }

  public static class BuilderImpl implements ProcessingContextBuilder {
    private TreeLogger treeLogger;
    private GeneratorContext generatorContext;
    private Context context;
    private BuildMetaClass bootstrapClassInstance;
    private ClassStructureBuilder bootstrapBuilder;
    private BlockBuilder<?> blockBuilder;
    private Set<String> packages;
    private QualifyingMetadataFactory qualifyingMetadataFactory;
    private boolean gwtTarget;

    private Class<? extends BootstrapInjectionContext> bootstrapClass;
    private Class<? extends CreationalContext> creationalContextClass;

    public static ProcessingContextBuilder create() {
      return new BuilderImpl();
    }

    @Override
    public ProcessingContextBuilder logger(final TreeLogger treeLogger) {
      this.treeLogger = treeLogger;
      return this;
    }

    @Override
    public ProcessingContextBuilder generatorContext(final GeneratorContext generatorContext) {
      this.generatorContext = generatorContext;
      return this;
    }

    @Override
    public ProcessingContextBuilder bootstrapContextClass(final Class<? extends BootstrapInjectionContext> bootstrapClass) {
      this.bootstrapClass = bootstrapClass;
      return this;
    }

    @Override
    public ProcessingContextBuilder creationalContextClass(final Class<? extends CreationalContext> creationalContextClass) {
      this.creationalContextClass = creationalContextClass;
      return this;
    }

    @Override
    public ProcessingContextBuilder context(final Context context) {
      this.context = context;
      return this;
    }

    @Override
    public ProcessingContextBuilder bootstrapClassInstance(final BuildMetaClass bootstrapClassInstance) {
      this.bootstrapClassInstance = bootstrapClassInstance;
      return this;
    }

    @Override
    public ProcessingContextBuilder bootstrapBuilder(final ClassStructureBuilder classStructureBuilder) {
      this.bootstrapBuilder = classStructureBuilder;
      return this;
    }

    @Override
    public ProcessingContextBuilder blockBuilder(final BlockBuilder<?> blockBuilder) {
      this.blockBuilder = blockBuilder;
      return this;
    }

    @Override
    public ProcessingContextBuilder packages(final Set<String> packages) {
      this.packages = packages;
      return this;
    }

    @Override
    public ProcessingContextBuilder qualifyingMetadata(final QualifyingMetadataFactory qualifyingMetadataFactory) {
      this.qualifyingMetadataFactory = qualifyingMetadataFactory;
      return this;
    }

    @Override
    public ProcessingContextBuilder gwtTarget(final boolean gwtTarget) {
      this.gwtTarget = gwtTarget;
      return this;
    }

    @Override
    public IOCProcessingContextImpl build() {
      Assert.notNull("treeLogger cannot be null", treeLogger);
      // Assert.notNull("sourceWriter cannot be null", sourceWriter);
      Assert.notNull("context cannot be null", context);
      Assert.notNull("bootstrapClassInstance cannot be null", bootstrapClassInstance);
      Assert.notNull("bootstrapBuilder cannot be null", bootstrapBuilder);
      Assert.notNull("blockBuilder cannot be null", blockBuilder);
      Assert.notNull("packages cannot be null", packages);
      Assert.notNull("bootstrap class must not be null", bootstrapClass);
      Assert.notNull("creationalContextClass must not be null", creationalContextClass);

      if (qualifyingMetadataFactory == null) {
        qualifyingMetadataFactory = new JSR330QualifyingMetadataFactory();
      }

      return new IOCProcessingContextImpl(this);
    }
  }

  @Override
  public BlockBuilder<?> getBlockBuilder() {
    return blockBuilder.peek();
  }

  @Override
  public BlockBuilder<?> append(final Statement statement) {
    return getBlockBuilder().append(statement);
  }

  @Override
  public void insertBefore(final Statement statement) {
     getBlockBuilder().insertBefore(statement);
  }


  @Override
  public void pushBlockBuilder(final BlockBuilder<?> blockBuilder) {
    this.blockBuilder.push(blockBuilder);
  }

  @Override
  public void popBlockBuilder() {
    this.blockBuilder.pop();

    if (this.blockBuilder.size() == 0) {
      throw new AssertionError("block builder was over popped! something is wrong.");
    }
  }

  @Override
  public void appendToEnd(final Statement statement) {
    appendToEnd.add(statement);
  }

  @Override
  public List<Statement> getAppendToEnd() {
    return appendToEnd;
  }

  @Override
  public BuildMetaClass getBootstrapClass() {
    return bootstrapClass;
  }

  @Override
  public ClassStructureBuilder getBootstrapBuilder() {
    return bootstrapBuilder;
  }

  @Override
  public Context getContext() {
    return context;
  }

  @Override
  public Set<String> getPackages() {
    return packages;
  }

  @Override
  public TreeLogger getTreeLogger() {
    return treeLogger;
  }

  @Override
  public GeneratorContext getGeneratorContext() {
    return generatorContext;
  }

  @Override
  public VariableReference getContextVariableReference() {
    return contextVariable.getReference();
  }

  @Override
  public boolean isGwtTarget() {
    return gwtTarget;
  }

  @Override
  public QualifyingMetadataFactory getQualifyingMetadataFactory() {
    return qualifyingMetadataFactory;
  }

  @Override
  public void registerTypeDiscoveryListener(final TypeDiscoveryListener discoveryListener) {
    this.typeDiscoveryListeners.add(discoveryListener);
  }

  @Override
  public void handleDiscoveryOfType(final InjectableInstance injectionPoint, final MetaClass discoveredType) {
    if (discovered.contains(injectionPoint.getElementTypeOrMethodReturnType())) {
      return;
    }
    for (final TypeDiscoveryListener listener : typeDiscoveryListeners) {
      listener.onDiscovery(this, injectionPoint, discoveredType);
    }
    discovered.add(injectionPoint.getElementTypeOrMethodReturnType());
  }

  @Override
  public Class<? extends BootstrapInjectionContext> getBootstrapContextClass() {
    return bootstrapContextClass;
  }

  @Override
  public Class<? extends CreationalContext> getCretionalContextClass() {
    return cretionalContextClass;
  }

  @Override
  public TreeLogger getLogger() {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
  }
}
