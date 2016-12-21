/*
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

package org.jboss.errai.bus.server.servlet;

import static org.jboss.errai.common.client.framework.Constants.CSRF_TOKEN_ATTRIBUTE_NAME;
import static org.jboss.errai.common.client.framework.Constants.ERRAI_CSRF_TOKEN_VAR;
import static org.jboss.errai.common.server.FilterCacheUtil.getCharResponseWrapper;
import static org.jboss.errai.common.server.FilterCacheUtil.noCache;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.jboss.errai.bus.server.util.SecureHashUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Max Barkley <mbarkley@redhat.com>
 */
public class CSRFTokenFilter implements Filter {

  private static Logger log = LoggerFactory.getLogger(CSRFTokenFilter.class);
  private static CSRFTokenCheck check = new CSRFTokenCheck();

  @Override
  public void init(final FilterConfig filterConfig) throws ServletException {
  }

  @Override
  public void destroy() {
  }

  @Override
  public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
          throws IOException, ServletException {
    final HttpServletRequest httpRequest = (HttpServletRequest) request;
    switch (httpRequest.getMethod().toUpperCase()) {
    case "POST":
    case "PUT":
    case "DELETE": {
      if (check.isInsecure(httpRequest, log)) {
        check.prepareResponse(httpRequest, (HttpServletResponse) response, log);
        return;
      }
    }
    case "GET": {
      final HttpSession session = httpRequest.getSession();
      final String csrfToken = getCSRFToken(session);
      final HttpServletResponse responseWrapper = noCache(getCharResponseWrapper((HttpServletResponse) response));
      chain.doFilter(httpRequest, responseWrapper);

      final byte[] bytes;
      final String responseContentType = responseWrapper.getContentType();
      if (responseContentType != null && responseContentType.equalsIgnoreCase("text/html")) {
        final Document document = Jsoup.parse(responseWrapper.toString());
        document.head().prepend("<script>var " + ERRAI_CSRF_TOKEN_VAR + " = '" + csrfToken + "';</script>");
        bytes = document.html().getBytes("UTF-8");
      }
      else {
        bytes = responseWrapper.toString().getBytes("UTF-8");
      }

      response.setContentLength(bytes.length);
      response.getOutputStream().write(bytes);

      return;
    }
    }

    chain.doFilter(request, response);
  }

  private String getCSRFToken(final HttpSession session) {
    final String csrfToken;
    if (session.getAttribute(CSRF_TOKEN_ATTRIBUTE_NAME) != null) {
      csrfToken = (String) session.getAttribute(CSRF_TOKEN_ATTRIBUTE_NAME);
    }
    else {
      csrfToken = SecureHashUtil.nextSecureHash();
      session.setAttribute(CSRF_TOKEN_ATTRIBUTE_NAME, csrfToken);
    }
    return csrfToken;
  }

}
