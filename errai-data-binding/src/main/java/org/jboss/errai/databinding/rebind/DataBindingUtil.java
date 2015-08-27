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

package org.jboss.errai.databinding.rebind;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;

import org.jboss.errai.codegen.Statement;
import org.jboss.errai.codegen.Variable;
import org.jboss.errai.codegen.exception.GenerationException;
import org.jboss.errai.codegen.meta.HasAnnotations;
import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaClassFactory;
import org.jboss.errai.codegen.meta.MetaConstructor;
import org.jboss.errai.codegen.meta.MetaField;
import org.jboss.errai.codegen.meta.MetaMethod;
import org.jboss.errai.codegen.meta.MetaParameter;
import org.jboss.errai.codegen.util.PrivateAccessType;
import org.jboss.errai.codegen.util.PrivateAccessUtil;
import org.jboss.errai.codegen.util.Stmt;
import org.jboss.errai.common.metadata.RebindUtils;
import org.jboss.errai.config.rebind.EnvUtil;
import org.jboss.errai.config.util.ClassScanner;
import org.jboss.errai.databinding.client.api.Bindable;
import org.jboss.errai.databinding.client.api.DataBinder;
import org.jboss.errai.ioc.rebind.ioc.injector.api.Decorable;
import org.jboss.errai.ioc.rebind.ioc.injector.api.FactoryController;
import org.jboss.errai.ui.shared.api.annotations.AutoBound;
import org.jboss.errai.ui.shared.api.annotations.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gwt.core.ext.GeneratorContext;

/**
 * Utility to retrieve a data binder reference. The reference is either to an
 * injected {@link AutoBound} data binder or to a generated data binder for an
 * injected {@link Model}.
 *
 * @author Christian Sadilek <csadilek@redhat.com>
 * @author Mike Brock
 */
public class DataBindingUtil {
  private static final Logger log = LoggerFactory.getLogger(DataBindingUtil.class);
  public static final String BINDER_VAR_NAME = "DataModelBinder";
  public static final String BINDER_MODEL_TYPE_VALUE = "DataBinderModelType";

  public static final Annotation[] MODEL_QUALIFICATION = new Annotation[] {
    new Model() {
      @Override
      public Class<? extends Annotation> annotationType() {
        return Model.class;
      }
    }
  };

  private DataBindingUtil() {}

  /**
   * Represents a reference to an injected or generated data binder.
   */
  public static class DataBinderRef {
    private final MetaClass dataModelType;
    private final Statement valueAccessor;

    public DataBinderRef(final MetaClass dataModelType, final Statement valueAccessor) {
      this.dataModelType = dataModelType;
      this.valueAccessor = valueAccessor;
    }

    public MetaClass getDataModelType() {
      return dataModelType;
    }

    public Statement getValueAccessor() {
      return valueAccessor;
    }
  }

  /**
   * Tries to find a data binder reference for either an injected {@link Model}
   * or an injected {@link AutoBound} data binder.
   *
   * @param inst
   *          the injectable instance
   *
   * @return the data binder reference or null if not found.
   */
  public static DataBinderRef lookupDataBinderRef(final Decorable decorable, final FactoryController controller) {
    DataBinderRef ref = lookupBinderForModel(decorable, controller);
    if (ref == null) {
      ref = lookupAutoBoundBinder(decorable, controller);
    }
    return ref;
  }

  /**
   * Tries to find a data binder reference for an injected {@link Model}.
   *
   * @param inst
   *          the injectable instance
   *
   * @return the data binder reference or null if not found.
   */
  private static DataBinderRef lookupBinderForModel(final Decorable decorable, final FactoryController controller) {
    Statement dataBinderRef;
    MetaClass dataModelType;

    final Collection<HasAnnotations> allAnnotated = getMembersAndParamsAnnotatedWith(decorable.getEnclosingType(), Model.class);

    if (!allAnnotated.isEmpty()) {
      if (allAnnotated.size() > 1) {
        throw new GenerationException("Multiple @Models injected in " + decorable.getEnclosingType());
      }
      else if (allAnnotated.size() == 1) {
        final HasAnnotations annotated = allAnnotated.iterator().next();

        if (annotated instanceof MetaParameter) {
          final MetaParameter mp = (MetaParameter) annotated;

          dataModelType = mp.getType();
          assertTypeIsBindable(dataModelType);
          dataBinderRef = controller.constructGetReference(BINDER_VAR_NAME, DataBinder.class);
        }
        else {
          final MetaField field = (MetaField) allAnnotated.iterator().next();

          dataModelType = field.getType();
          assertTypeIsBindable(dataModelType);

          dataBinderRef = controller.constructGetReference(BINDER_VAR_NAME, DataBinder.class);
          controller.addExposedField(field, PrivateAccessType.Both);
        }
        return new DataBinderRef(dataModelType, dataBinderRef);
      }
    }
    else {
      List<MetaField> modelFields = decorable.getEnclosingType().getFieldsAnnotatedWith(Model.class);
      if (!modelFields.isEmpty()) {
        throw new GenerationException("Found one or more fields annotated with @Model but missing @Inject "
                + modelFields.toString());
      }

      List<MetaParameter> modelParameters = decorable.getEnclosingType().getParametersAnnotatedWith(Model.class);
      if (!modelParameters.isEmpty()) {
        throw new GenerationException(
                "Found one or more constructor or method parameters annotated with @Model but missing @Inject "
                        + modelParameters.toString());
      }
    }

    return null;
  }

  private static Collection<HasAnnotations> getMembersAndParamsAnnotatedWith(final MetaClass enclosingType, final Class<? extends Annotation> annoType) {
    final Collection<HasAnnotations> annotated = new ArrayList<HasAnnotations>();

    final Target target = annoType.getAnnotation(Target.class);
    final Collection<ElementType> allowedTypes = (target == null) ? null : Arrays.asList(target.value());

    if (allowedTypes == null || allowedTypes.contains(ElementType.FIELD)) {
      annotated.addAll(enclosingType.getFieldsAnnotatedWith(annoType));
    }

    if (allowedTypes == null || allowedTypes.contains(ElementType.METHOD)) {
      annotated.addAll(enclosingType.getMethodsAnnotatedWith(annoType));
    }

    if (allowedTypes == null || allowedTypes.contains(ElementType.CONSTRUCTOR)) {
      for (final MetaConstructor ctor : enclosingType.getConstructors()) {
        if (ctor.isAnnotationPresent(annoType)) {
          annotated.add(ctor);
        }
      }
    }

    if (allowedTypes == null || allowedTypes.contains(ElementType.PARAMETER)) {
      for (final MetaMethod method : enclosingType.getMethods()) {
        for (final MetaParameter param : method.getParameters()) {
          if (param.isAnnotationPresent(annoType)) {
            annotated.add(param);
          }
        }
      }
      for (final MetaConstructor ctor : enclosingType.getConstructors()) {
        for (final MetaParameter param : ctor.getParameters()) {
          if (param.isAnnotationPresent(annoType)) {
            annotated.add(param);
          }
        }
      }
    }

    return annotated;
  }

  /**
   * Tries to find a reference for an injected {@link AutoBound} data binder.
   *
   * @return the data binder reference or null if not found.
   */
  private static DataBinderRef lookupAutoBoundBinder(final Decorable decorable, final FactoryController controller) {
    Statement dataBinderRef = null;
    MetaClass dataModelType = null;

    final Collection<HasAnnotations> allAnnotated = getMembersAndParamsAnnotatedWith(decorable.getEnclosingType(), AutoBound.class);

    if (allAnnotated.size() > 1) {
      throw new GenerationException("Multiple @AutoBound data binders injected in " + decorable.getEnclosingType());
    }
    else if (allAnnotated.size() == 1) {
      final HasAnnotations annotated = allAnnotated.iterator().next();

      if (annotated instanceof MetaParameter) {
        final MetaParameter mp = (MetaParameter) annotated;

        assertTypeIsDataBinder(mp.getType());
        dataModelType = (MetaClass) mp.getType().getParameterizedType().getTypeParameters()[0];
        dataBinderRef = decorable.getInjectionContext().getInlineBeanReference(mp);
      }
      else {
        final MetaField field = (MetaField) allAnnotated.iterator().next();

        assertTypeIsDataBinder(field.getType());
        dataModelType = (MetaClass) field.getType().getParameterizedType().getTypeParameters()[0];
        dataBinderRef = Stmt.invokeStatic(decorable.getInjectionContext().getProcessingContext().getBootstrapClass(),
                PrivateAccessUtil.getPrivateFieldAccessorName(field),
                Variable.get("instance"));
        controller.addExposedField(field, PrivateAccessType.Both);
      }
    }
    else {
      final MetaClass declaringClass = decorable.getEnclosingType();
      for (final MetaField field : declaringClass.getFields()) {
        if (field.isAnnotationPresent(AutoBound.class)) {
          assertTypeIsDataBinder(field.getType());
          dataModelType = (MetaClass) field.getType().getParameterizedType().getTypeParameters()[0];
          dataBinderRef = Stmt.invokeStatic(decorable.getInjectionContext().getProcessingContext().getBootstrapClass(),
                  PrivateAccessUtil.getPrivateFieldAccessorName(field),
                  Variable.get("instance"));
          controller.addExposedField(field, PrivateAccessType.Both);
          break;
        }
      }
    }

    return (dataBinderRef != null) ? new DataBinderRef(dataModelType, dataBinderRef) : null;
  }

  /**
   * Ensures the provided type is a {@link DataBinder} and throws a
   * {@link GenerationException} in case it's not.
   *
   * @param type
   *          the type to check
   */
  private static void assertTypeIsDataBinder(MetaClass type) {
    final MetaClass databinderMetaClass = MetaClassFactory.get(DataBinder.class);

    if (!databinderMetaClass.isAssignableFrom(type)) {
      throw new GenerationException("Type of @AutoBound element must be " + DataBinder.class.getName() + " but is: "
              + type.getFullyQualifiedName());
    }
  }

  /**
   * Ensured the provided type is bindable and throws a
   * {@link GenerationException} in case it's not.
   *
   * @param type
   *          the type to check
   */
  private static void assertTypeIsBindable(MetaClass type) {
    if (!type.isAnnotationPresent(Bindable.class) && !getConfiguredBindableTypes().contains(type)) {
      throw new GenerationException(type.getName() + " must be a @Bindable type when used as @Model");
    }
  }

  /**
   * Checks if the provided type is bindable.
   *
   * @param type
   *          the type to check
   *
   * @return true if the provide type is bindable, otherwise false.
   */
  public static boolean isBindableType(MetaClass type) {
    return (type.isAnnotationPresent(Bindable.class) || getConfiguredBindableTypes().contains(type));
  }

  /**
   * Returns all bindable types on the classpath.
   *
   * @param context
   *          the current generator context
   *
   * @return a set of meta classes representing the all bindable types (both
   *         annotated and configured in ErraiApp.properties).
   */
  public static Set<MetaClass> getAllBindableTypes(final GeneratorContext context) {
    Collection<MetaClass> annotatedBindableTypes = ClassScanner.getTypesAnnotatedWith(Bindable.class,
            RebindUtils.findTranslatablePackages(context), context);

    Set<MetaClass> bindableTypes = new HashSet<MetaClass>(annotatedBindableTypes);
    bindableTypes.addAll(DataBindingUtil.getConfiguredBindableTypes());
    return bindableTypes;
  }

  private static Set<MetaClass> configuredBindableTypes = null;

  /**
   * Reads bindable types from all ErraiApp.properties files on the classpath.
   *
   * @return a set of meta classes representing the configured bindable types.
   */
  public static Set<MetaClass> getConfiguredBindableTypes() {
    if (configuredBindableTypes != null) {
        configuredBindableTypes = refreshConfiguredBindableTypes();
    } else {
        configuredBindableTypes = findConfiguredBindableTypes();
    }

    return configuredBindableTypes;
  }

  private static Set<MetaClass> refreshConfiguredBindableTypes() {
    final Set<MetaClass> refreshedTypes = new HashSet<MetaClass>(configuredBindableTypes.size());

    for (final MetaClass clazz : configuredBindableTypes) {
      refreshedTypes.add(MetaClassFactory.get(clazz.getFullyQualifiedName()));
    }

    return refreshedTypes;
  }

  private static Set<MetaClass> findConfiguredBindableTypes() {
    Set<MetaClass> bindableTypes = new HashSet<MetaClass>();
    final Collection<URL> erraiAppProperties = EnvUtil.getErraiAppProperties();
    for (URL url : erraiAppProperties) {
      InputStream inputStream = null;
      try {
        log.debug("Checking " + url.getFile() + " for bindable types...");
        inputStream = url.openStream();

        final ResourceBundle props = new PropertyResourceBundle(inputStream);
        for (final String key : props.keySet()) {
          if (key.equals("errai.ui.bindableTypes")) {
            for (final String s : props.getString(key).split(" ")) {
              try {
                bindableTypes.add(MetaClassFactory.get(s.trim()));
              } catch (Exception e) {
                throw new RuntimeException("Could not find class defined in ErraiApp.properties as bindable type: " + s);
              }
            }
            break;
          }
        }
      } catch (IOException e) {
        throw new RuntimeException("Error reading ErraiApp.properties", e);
      } finally {
        if (inputStream != null) {
          try {
            inputStream.close();
          } catch (IOException e) {
            log.warn("Failed to close input stream", e);
          }
        }
      }
    }

    return bindableTypes;
  }
}