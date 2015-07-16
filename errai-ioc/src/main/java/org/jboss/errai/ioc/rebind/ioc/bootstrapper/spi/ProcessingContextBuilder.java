package org.jboss.errai.ioc.rebind.ioc.bootstrapper.spi;

import java.util.Set;

import org.jboss.errai.codegen.Context;
import org.jboss.errai.codegen.builder.BlockBuilder;
import org.jboss.errai.codegen.builder.ClassStructureBuilder;
import org.jboss.errai.codegen.meta.impl.build.BuildMetaClass;
import org.jboss.errai.ioc.client.BootstrapInjectionContext;
import org.jboss.errai.ioc.client.container.CreationalContext;
import org.jboss.errai.ioc.rebind.ioc.metadata.QualifyingMetadataFactory;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;

public interface ProcessingContextBuilder {

  ProcessingContextBuilder logger(TreeLogger treeLogger);

  ProcessingContextBuilder generatorContext(GeneratorContext generatorContext);

  ProcessingContextBuilder bootstrapContextClass(Class<? extends BootstrapInjectionContext> bootstrapClass);

  ProcessingContextBuilder creationalContextClass(Class<? extends CreationalContext> creationalContextClass);

  ProcessingContextBuilder context(Context context);

  ProcessingContextBuilder bootstrapClassInstance(BuildMetaClass bootstrapClassInstance);

  ProcessingContextBuilder bootstrapBuilder(ClassStructureBuilder classStructureBuilder);

  ProcessingContextBuilder blockBuilder(BlockBuilder<?> blockBuilder);

  ProcessingContextBuilder packages(Set<String> packages);

  ProcessingContextBuilder qualifyingMetadata(QualifyingMetadataFactory qualifyingMetadataFactory);

  ProcessingContextBuilder gwtTarget(boolean gwtTarget);

  IOCProcessingContext build();

}