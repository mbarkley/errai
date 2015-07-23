package org.jboss.errai.common.rebind;

import java.util.HashMap;
import java.util.Map;

public class PrefixTrieMatcher {

  private class Node {
    Map<Character, Node> children = new HashMap<Character, Node>();
    String chunk = "";
    boolean isTerminal = false;
  }

  private final Node root = new Node();

  /**
   * @param prefix Subsequent calls to {@link PrefixTrieMatcher#startsWith(String)} will return true if the target String begins with {@code prefix}.
   */
  public void add(String prefix) {
    Node curNode = root;
    int i = 0;
    while (i < prefix.length()) {
      Node child = curNode.children.get(prefix.charAt(i));
      if (child == null) {
        child = newTerminalNode(prefix.substring(i));
        curNode.children.put(prefix.charAt(i), child);
        i = prefix.length();
      } else {
        int j = 1;
        while (j < child.chunk.length() && i+j < prefix.length() && child.chunk.charAt(j) == prefix.charAt(i+j)) {
          j += 1;
        }
        if (j < child.chunk.length() && i+j < prefix.length()) {
          splitChildAt(child, j);
          child.children.put(prefix.charAt(i+j), newTerminalNode(prefix.substring(i+j)));
          i = prefix.length();
        } else if (j < child.chunk.length()) {
          splitChildAt(child, j);
          child.isTerminal = true;
        } else if (i+j < prefix.length()) {
          i += j;
        }
      }
    }
  }

  private Node newTerminalNode(String chunk) {
    Node child;
    child = new Node();
    child.chunk = chunk;
    child.isTerminal = true;
    return child;
  }

  private void splitChildAt(final Node child, final int i) {
    final Node newChild = new Node();
    newChild.isTerminal = child.isTerminal;
    newChild.chunk = child.chunk.substring(i);
    newChild.children = child.children;

    child.isTerminal = false;
    child.chunk = child.chunk.substring(0, i);
    child.children = new HashMap<Character, Node>();
    child.children.put(newChild.chunk.charAt(0), newChild);
  }

  /**
   * @param target The string being checked for matching prefices.
   * @return True iff target begins with a prefix that has been added to this matcher.
   */
  public boolean startsWith(String target) {
    Node curNode = root;
    int i = 0;

    while (!curNode.isTerminal && i < target.length()) {
      final Node child = curNode.children.get(target.charAt(i));
      if (child == null) {
        return false;
      }
      int j = 1;
      while (j < child.chunk.length() && i+j < target.length() && child.chunk.charAt(j) == target.charAt(i+j)) {
        j += 1;
      }
      if (j < child.chunk.length()) {
        return false;
      }
      i += j;
      curNode = child;
    }

    return curNode.isTerminal;
  }

}
