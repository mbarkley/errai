package org.jboss.errai.ioc.rebind.ioc.bootstrapper;

import java.util.List;
import java.util.Set;

import org.jboss.errai.codegen.Context;
import org.jboss.errai.codegen.Statement;
import org.jboss.errai.codegen.VariableReference;
import org.jboss.errai.codegen.builder.BlockBuilder;
import org.jboss.errai.codegen.builder.ClassStructureBuilder;
import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.impl.build.BuildMetaClass;
import org.jboss.errai.ioc.client.BootstrapInjectionContext;
import org.jboss.errai.ioc.client.container.CreationalContext;
import org.jboss.errai.ioc.rebind.ioc.injector.api.InjectableInstance;
import org.jboss.errai.ioc.rebind.ioc.injector.api.TypeDiscoveryListener;
import org.jboss.errai.ioc.rebind.ioc.metadata.QualifyingMetadataFactory;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;

public interface IOCProcessingContext {

  BlockBuilder<?> getBlockBuilder();

  BlockBuilder<?> append(Statement statement);

  void insertBefore(Statement statement);

  void pushBlockBuilder(BlockBuilder<?> blockBuilder);

  void popBlockBuilder();

  void appendToEnd(Statement statement);

  List<Statement> getAppendToEnd();

  BuildMetaClass getBootstrapClass();

  ClassStructureBuilder getBootstrapBuilder();

  Context getContext();

  Set<String> getPackages();

  TreeLogger getTreeLogger();

  GeneratorContext getGeneratorContext();

  VariableReference getContextVariableReference();

  boolean isGwtTarget();

  QualifyingMetadataFactory getQualifyingMetadataFactory();

  void registerTypeDiscoveryListener(TypeDiscoveryListener discoveryListener);

  void handleDiscoveryOfType(InjectableInstance injectionPoint, MetaClass discoveredType);

  Class<? extends BootstrapInjectionContext> getBootstrapContextClass();

  Class<? extends CreationalContext> getCretionalContextClass();
  
  TreeLogger getLogger();

}