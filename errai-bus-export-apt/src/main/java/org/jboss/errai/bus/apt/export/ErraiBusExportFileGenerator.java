/*
 * Copyright (C) 2017 Red Hat, Inc. and/or its affiliates.
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

package org.jboss.errai.bus.apt.export;

import org.jboss.errai.common.apt.generator.AbstractExportFileGenerator;

import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

import static org.jboss.errai.bus.apt.export.ErraiBusExportFileGenerator.SupportedAnnotationTypes.FEATURE_INTERCEPTOR;
import static org.jboss.errai.bus.apt.export.ErraiBusExportFileGenerator.SupportedAnnotationTypes.INTERCEPTED_CALL;
import static org.jboss.errai.bus.apt.export.ErraiBusExportFileGenerator.SupportedAnnotationTypes.INTERCEPTS_REMOTE_CALL;
import static org.jboss.errai.bus.apt.export.ErraiBusExportFileGenerator.SupportedAnnotationTypes.REMOTE;

/**
 * @author Tiago Bento <tfernand@redhat.com>
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes({ REMOTE, INTERCEPTED_CALL, FEATURE_INTERCEPTOR, INTERCEPTS_REMOTE_CALL })
public class ErraiBusExportFileGenerator extends AbstractExportFileGenerator {

  @Override
  protected String getCamelCaseErraiModuleName() {
    return "bus";
  }

  interface SupportedAnnotationTypes {
    String REMOTE = "org.jboss.errai.bus.server.annotations.Remote";
    String FEATURE_INTERCEPTOR = "org.jboss.errai.common.client.api.interceptor.FeatureInterceptor";
    String INTERCEPTED_CALL = "org.jboss.errai.common.client.api.interceptor.InterceptedCall";
    String INTERCEPTS_REMOTE_CALL = "org.jboss.errai.common.client.api.interceptor.InterceptsRemoteCall";
  }
}