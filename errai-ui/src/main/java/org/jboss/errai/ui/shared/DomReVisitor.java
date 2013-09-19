package org.jboss.errai.ui.shared;

import org.w3c.dom.Element;

public interface DomReVisitor extends DomVisitor {

  /**
   * This method is invoked after this element and all of its children have been visited.
   */
  public void afterVisit(Element element);
}
