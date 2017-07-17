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

package org.jboss.errai.ioc.rebind.ioc.graph.impl;

import static java.util.Collections.singleton;
import static java.util.Spliterator.DISTINCT;
import static java.util.Spliterator.ORDERED;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Spliterators;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.jboss.errai.ioc.client.api.EntryPoint;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraph;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder.Dependency;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder.Resolution;
import org.jboss.errai.ioc.rebind.ioc.graph.api.Fragment;
import org.jboss.errai.ioc.rebind.ioc.graph.api.Injectable;
import org.jboss.errai.ioc.rebind.ioc.graph.impl.DependencyGraphImpl.Graph.Component;
import org.jboss.errai.ioc.rebind.ioc.graph.impl.DependencyGraphImpl.Graph.DFSFrame;
import org.jboss.errai.ioc.rebind.ioc.graph.impl.DependencyGraphImpl.Graph.Node;

import com.google.common.collect.Iterators;
import com.google.gwt.thirdparty.guava.common.base.Objects;

/**
 * @see DependencyGraph
 * @author Max Barkley <mbarkley@redhat.com>
 */
class DependencyGraphImpl implements DependencyGraph {

  private final Map<String, Injectable> injectablesByName;
  private final Map<Dependency, Resolution> resolved;

  DependencyGraphImpl(final Map<String, Injectable> injectablesByName, final Map<Dependency, Resolution> resolved) {
    this.injectablesByName = injectablesByName;
    this.resolved = resolved;
  }

  @Override
  public Iterator<Injectable> iterator() {
    return Iterators.unmodifiableIterator(injectablesByName.values().iterator());
  }

  @Override
  public Injectable getConcreteInjectable(final String injectableName) {
    return injectablesByName.get(injectableName);
  }

  @Override
  public int getNumberOfInjectables() {
    return injectablesByName.size();
  }

  @Override
  public Resolution getResolved(final Dependency dependency) {
    return resolved.get(dependency);
  }

  @Override
  public Collection<Fragment> getFragments() {
    final Graph<Injectable> rawGraph = generateRawGraph();

    final Graph<Component<Injectable>> dag = rawGraph.createComponentDAG();
    final Set<Node<Component<Injectable>>> initialCut = new HashSet<>();

    final Predicate<? super Node<Component<Injectable>>> entrypointPred = node -> node
                      .getValue()
                      .getNodes()
                      .stream()
                      .map(n -> n.getValue())
                      .anyMatch(inj -> EntryPoint.class.equals(inj.getScope()));
    dag
      .nodes()
      .filter(entrypointPred)
      .flatMap(node -> dag.getReachable(node))
      .forEach(node -> initialCut.add(node));

    final Set<Set<Node<Component<Injectable>>>> cuts = getFragmentCuts(dag, initialCut);
    final Set<Set<Node<Injectable>>> rawCuts =
            cuts
            .stream()
            .map(cut ->
                  cut.stream()
                     .flatMap(node ->
                               node.getValue()
                                   .getNodes()
                                   .stream())
                     .collect(toSet()))
            .collect(toSet());

    final Graph<Component<Injectable>> fragGraph = rawGraph.createCutDAG(rawCuts);
    final Iterator<Integer> fragIdIterator = Stream.iterate(fragGraph.nodeCount(), n -> n - 1).iterator();
    final BiFunction<Component<Injectable>, List<Fragment>, Fragment> folder =
            (comp, frags) -> {
              final boolean initialFrag = comp
                .getNodes()
                .stream()
                .anyMatch(node -> EntryPoint.class.equals(node.getValue().getScope()));
              final List<Injectable> injectables = comp.getNodes().stream().map(node -> node.getValue()).collect(toList());
              if (initialFrag) {
                return new Fragment("Initial", injectables, frags);
              }
              else {
                return new Fragment(fragIdIterator.next().toString(), injectables, frags);
              }
            };

    fragGraph.assertAcylic();

    return fragGraph
            .doDAGFoldKeepingAll(folder)
            .stream()
            .sorted((a, b) -> {
              if ("Initial".equals(a.getName())) {
                return -1;
              }
              else if ("Initial".equals(b.getName())) {
                return 1;
              }
              else {
                return (Integer.valueOf(a.getName()) - Integer.valueOf(b.getName()));
              }
            })
            .collect(toList());
  }

  private Graph<Injectable> generateRawGraph() {
    final Graph<Injectable> rawGraph = new Graph<>();
    final Map<Injectable, Node<Injectable>> nodesByInjectable = injectablesByName
      .values()
      .stream()
      .map(inj -> rawGraph.createNode(inj))
      .collect(toMap(node -> node.getValue(), Function.identity()));

    nodesByInjectable
      .forEach((inj, node) -> {
        inj
          .getDependencies()
          .stream()
          .flatMap(dep -> {
            final Resolution r = getResolved(dep);
            if (r == null) {
              return Stream.empty();
            }
            else {
              return r.stream();
            }
          })
          .filter(dep -> nodesByInjectable.containsKey(dep))
          .map(dep -> nodesByInjectable.get(dep))
          .forEach(dep -> rawGraph.createEdge(node, dep));
      });
    return rawGraph;
  }

  private Set<Set<Node<Component<Injectable>>>> getFragmentCuts(final Graph<Component<Injectable>> dag,
          final Set<Node<Component<Injectable>>> initialCut) {
    final Queue<Node<Component<Injectable>>> queue = new LinkedList<>();
    final Set<Node<Component<Injectable>>> roots = new HashSet<>();
    dag
      .getRoots()
      .filter(node -> !initialCut.contains(node))
      .forEach(node -> {
        queue.add(node);
        roots.add(node);
      });
    final Deque<DFSFrame<Node<Component<Injectable>>>> forwardStack = new LinkedList<>();
    final Deque<DFSFrame<Node<Component<Injectable>>>> backwardStack = new LinkedList<>();
    final Set<Node<Component<Injectable>>> discovered = new HashSet<>(initialCut);
    final Set<Set<Node<Component<Injectable>>>> cuts = new HashSet<>();
    cuts.add(initialCut);

    while (!queue.isEmpty()) {
      final Set<Node<Component<Injectable>>> cut = new HashSet<>();
      final Node<Component<Injectable>> fragRoot = queue.poll();
      cut.add(fragRoot);
      forwardStack.push(new DFSFrame<>(fragRoot, dag.outboundNodes(fragRoot).iterator()));
      discovered.add(fragRoot);
      do {
        final DFSFrame<Node<Component<Injectable>>> forwardFrame = forwardStack.getFirst();
        if (forwardFrame.iter.hasNext()) {
          final Node<Component<Injectable>> forwardNext = forwardFrame.iter.next();
          if (!discovered.contains(forwardNext)) {
            discovered.add(forwardNext);
            backwardStack.push(new DFSFrame<>(forwardNext, dag.inboundNodes(forwardNext).iterator()));
            boolean otherRootFound = false;
            do {
              final DFSFrame<Node<Component<Injectable>>> backwardFrame = backwardStack.getFirst();
              if (backwardFrame.iter.hasNext()) {
                final Node<Component<Injectable>> backwardNext = backwardFrame.iter.next();
                if (!roots.contains(backwardNext)) {
                  backwardStack.push(new DFSFrame<>(backwardNext, dag.inboundNodes(backwardNext).iterator()));
                }
                else if (forwardStack.getLast().value != backwardNext) {
                  otherRootFound = true;
                  backwardStack.clear();
                  break;
                }
              }
              else {
                backwardStack.pop();
              }
            } while (!backwardStack.isEmpty());

            if (otherRootFound) {
              queue.add(forwardNext);
              roots.add(forwardNext);
            }
            else {
              forwardStack.push(new DFSFrame<>(forwardNext, dag.outboundNodes(forwardNext).iterator()));
            }
          }
        }
        else {
          cut.add(forwardStack.pop().value);
        }
      } while (!forwardStack.isEmpty());
      cuts.add(cut);
    }
    return cuts;
  }

  public static class Graph<T> {
    private final Map<Node<T>, List<Edge<T>>> nodeEdges = new HashMap<>();

    private class DAGFoldResult<U> {
      private final List<U> topLevel;
      private final Collection<U> all;
      private DAGFoldResult(final List<U> topLevel, final Collection<U> all) {
        this.topLevel = topLevel;
        this.all = all;
      }
    }

    public <U> List<U> doDAGFold(final BiFunction<T, List<U>, U> folder) {
      final DAGFoldResult<U> result = doDAGFoldHelper(folder);
      return result.topLevel;
    }

    public int nodeCount() {
      return nodeEdges.size();
    }

    public void assertAcylic() {
      final Deque<DFSFrame<Node<T>>> stack = new LinkedList<>();
      final Set<Node<T>> visiting = new HashSet<>();
      final Set<Node<T>> visited = new HashSet<>();

      nodes()
        .forEach(node -> {
          if (!visited.contains(node)) {
            stack.push(new DFSFrame<>(node, outboundNodes(node).iterator()));
            visiting.add(node);
            do {
              final DFSFrame<Node<T>> frame = stack.getFirst();
              if (frame.iter.hasNext()) {
                final Node<T> next = frame.iter.next();
                if (visiting.contains(next)) {
                  final StringBuilder msg = new StringBuilder();
                  msg.append("Cycle found:\n");
                  while (stack.getFirst().value != next) {
                    msg.append('\t').append(stack.pop().value).append('\n');
                  }
                  msg.append('\t').append(next);

                  throw new RuntimeException(msg.toString());
                }
                else if (!visited.contains(next)) {
                  stack.push(new DFSFrame<>(next, outboundNodes(next).iterator()));
                  visiting.add(next);
                }
              }
              else {
                stack.pop();
                visiting.remove(frame.value);
                visited.add(frame.value);
              }
            } while (!stack.isEmpty());
          }
        });
    }

    public <U> Collection<U> doDAGFoldKeepingAll(final BiFunction<T, List<U>, U> folder) {
      final DAGFoldResult<U> result = doDAGFoldHelper(folder);
      return result.all;
    }

    private <U> DAGFoldResult<U> doDAGFoldHelper(final BiFunction<T, List<U>, U> folder) {
      class Partial {
        final Node<T> node;
        final List<U> subValues = new ArrayList<>();
        Partial(final Node<T> node) {
          this.node = node;
        }
      }

      final Deque<DFSFrame<Partial>> stack = new LinkedList<>();
      final Queue<Node<T>> rootQueue = new LinkedList<>();
      getRoots().forEach(root -> rootQueue.add(root));

      final Function<Node<T>, Iterator<Partial>> makeIter = node -> outboundNodes(node)
                                                      .map(n -> new Partial(n))
                                                      .iterator();

      final List<U> topLevelFolded = new ArrayList<>();
      final Map<Node<T>, U> visited = new HashMap<>();
      while (!rootQueue.isEmpty()) {
        final Node<T> root = rootQueue.remove();
        stack.push(new DFSFrame<>(new Partial(root), makeIter.apply(root)));
        do {
          final DFSFrame<Partial> frame = stack.getFirst();
          if (frame.iter.hasNext()) {
            final Partial next = frame.iter.next();
            if (visited.containsKey(next.node)) {
              frame.value.subValues.add(visited.get(next.node));
            }
            else {
              stack.push(new DFSFrame<>(next, makeIter.apply(next.node)));
            }
          }
          else {
            stack.pop();
            final T t = frame.value.node.getValue();
            final List<U> subValues = frame.value.subValues;
            final U folded = folder.apply(t, subValues);
            visited.put(frame.value.node, folded);
            if (stack.isEmpty()) {
              topLevelFolded.add(folded);
            }
            else {
              stack.getFirst().value.subValues.add(folded);
            }
          }
        } while (!stack.isEmpty());
      }

      return new DAGFoldResult<>(topLevelFolded, visited.values());
    }

    public Stream<Node<T>> getRoots() {
      return nodes()
              .filter(node -> !inEdges(node).findAny().isPresent());
    }

    public Stream<Node<T>> getLeaves() {
      return nodes()
              .filter(node -> !outEdges(node).findAny().isPresent());
    }

    public Graph<Component<T>> createCutDAG(final Collection<Set<Node<T>>> cuts) {
      class EdgePair {
        final Node<Component<T>> source;
        final Node<Component<T>> target;
        EdgePair(final Node<Component<T>> source, final Node<Component<T>> target) {
          this.source = source;
          this.target = target;
        }
        @Override
        public boolean equals(final Object obj) {
          return EdgePair.class.isInstance(obj)
                  && EdgePair.class.cast(obj).source == source
                  && EdgePair.class.cast(obj).target == target;
        }
        @Override
        public int hashCode() {
          return Objects.hashCode(source, target);
        }
      }

      final Graph<Component<T>> graph = new Graph<>();
      final Map<Node<T>, Node<Component<T>>> componentsByNode = new HashMap<>();
      cuts
        .forEach(cut -> {
          final Node<Component<T>> compNode = graph.createNode(new Component<>(this, cut));
          cut.forEach(node -> componentsByNode.put(node, compNode));
        });

      edges()
        .filter(edge -> componentsByNode.containsKey(edge.getSource()) && componentsByNode.containsKey(edge.getTarget()))
        .map(edge -> {
          final Node<Component<T>> source = componentsByNode.get(edge.getSource());
          final Node<Component<T>> target = componentsByNode.get(edge.getTarget());
          return new EdgePair(source, target);
        })
        .filter(edgePair -> edgePair.source != edgePair.target)
        .distinct()
        .forEach(edgePair -> graph.createEdge(edgePair.source, edgePair.target));

      return graph;
    }

    public boolean hasInbound(final Node<T> node) {
      return inEdges(node).findAny().isPresent();
    }

    public Graph<Component<T>> createComponentDAG() {
      final List<Set<Node<T>>> sccs = getStronglyConnectedComponents();

      return createCutDAG(sccs);
    }

    public Stream<Node<T>> getReachable(final Node<T> from) {
      return getReachable(singleton(from));
    }

    public Stream<Node<T>> getReachable(final Collection<Node<T>> from) {
      return StreamSupport.stream(Spliterators.spliteratorUnknownSize(reachableIterator(from), ORDERED | DISTINCT), false);
    }

    private Iterator<Node<T>> reachableIterator(final Collection<Node<T>> from) {
      final Set<Node<T>> discovered = new HashSet<>();
      final Queue<Node<T>> queue = new LinkedList<>();
      queue.addAll(from);
      discovered.addAll(from);
      return new Iterator<Node<T>>() {
        @Override
        public boolean hasNext() {
          return !queue.isEmpty();
        }

        @Override
        public Node<T> next() {
          final Node<T> next = queue.poll();
          outboundNodes(next)
            .filter(node -> !discovered.contains(node))
            .forEach(node -> {
              discovered.add(node);
              queue.add(node);
            });
          return next;
        }
      };
    }

    public List<Set<Node<T>>> getStronglyConnectedComponents() {

      final Map<Node<T>, TarjanNode<T>> tarjanNodes = new HashMap<>();
      final Deque<DFSFrame<TarjanNode<T>>> visiting = new LinkedList<>();
      final Deque<TarjanNode<T>> tarjanStack = new LinkedList<>();
      final List<Set<Node<T>>> components = new ArrayList<>();
      Set<Node<T>> curComponent = new HashSet<>(1);

      final Iterator<Integer> indexProducer = Stream.iterate(0, n -> n + 1).iterator();
      for (final Node<T> node : nodeEdges.keySet()) {
        if (!tarjanNodes.containsKey(node)) {
          pushNode(tarjanNodes, visiting, tarjanStack, indexProducer, node);
          do {
            final DFSFrame<TarjanNode<T>> curFrame = visiting.peekFirst();
            final TarjanNode<T> curNode = curFrame.value;
            if (curFrame.iter.hasNext()) {
              final TarjanNode<T> next = curFrame.iter.next();
              if (!next.visited && !next.onStack) {
                pushTarjanNode(tarjanNodes, visiting, tarjanStack, indexProducer, next);
              }
              else if (next.onStack) {
                curNode.lowIndex = Math.min(curNode.lowIndex, next.index);
              }
            }
            else {
              visiting.pop();
              curNode.visited = true;
              if (curNode.index == curNode.lowIndex) {
                TarjanNode<T> componentNode;
                do {
                  componentNode = tarjanStack.pop();
                  componentNode.onStack = false;
                  curComponent.add(componentNode.node);
                } while (componentNode != curNode);
                components.add(curComponent);
                curComponent = new HashSet<>(1);
              }
            }
          } while (!visiting.isEmpty());
        }
      }

      return components;
    }

    private void pushNode(final Map<Node<T>, TarjanNode<T>> tarjanNodes, final Deque<DFSFrame<TarjanNode<T>>> visiting,
            final Deque<TarjanNode<T>> tarjanStack, final Iterator<Integer> indexProducer, final Node<T> node) {
      final TarjanNode<T> indices = new TarjanNode<>(node, indexProducer.next());
      tarjanNodes.put(indices.node, indices);
      pushTarjanNode(tarjanNodes, visiting, tarjanStack, indexProducer, indices);
    }

    private void pushTarjanNode(final Map<Node<T>, TarjanNode<T>> tarjanIndices, final Deque<DFSFrame<TarjanNode<T>>> visiting,
            final Deque<TarjanNode<T>> tarjanStack, final Iterator<Integer> indexProducer,
            final TarjanNode<T> indices) {
      visiting.push(new DFSFrame<>(indices, outboundNodes(indices.node).map(next -> {
        return tarjanIndices.computeIfAbsent(next, n -> new TarjanNode<>(n, indexProducer.next()));
      }).iterator()));
      tarjanStack.push(indices);
      indices.onStack = true;
    }

    public Stream<Node<T>> nodes() {
      return nodeEdges.keySet().stream();
    }

    public Stream<Edge<T>> edges() {
      return nodeEdges.values().stream().flatMap(edges -> edges.stream()).distinct();
    }

    public Stream<Edge<T>> edges(final Node<T> node) {
      return nodeEdges.get(node).stream();
    }

    public Stream<Edge<T>> inEdges(final Node<T> node) {
      return nodeEdges.get(node).stream().filter(e -> e.target == node);
    }

    public Stream<Edge<T>> outEdges(final Node<T> node) {
      return nodeEdges.get(node).stream().filter(e -> e.source == node);
    }

    public Stream<Node<T>> outboundNodes(final Node<T> source) {
      return outEdges(source).map(edge -> edge.target);
    }

    public Stream<Node<T>> inboundNodes(final Node<T> source) {
      return inEdges(source).map(edge -> edge.source);
    }

    public Node<T> createNode(final T value) {
      final Node<T> node = new Node<>(value);
      nodeEdges.put(node, new ArrayList<>());
      return node;
    }

    public Edge<T> createEdge(final Node<T> source, final Node<T> target) {
      final Edge<T> edge = new Edge<>(source, target);
      final List<Edge<T>> srcEdges = nodeEdges.get(source);
      final List<Edge<T>> tgtEdges = nodeEdges.get(target);
      if (srcEdges != null && tgtEdges != null) {
        srcEdges.add(edge);
        tgtEdges.add(edge);

        return edge;
      }
      else {
        throw new IllegalStateException("Cannot create edge for node that is not part of this graph: " + (srcEdges == null ? source : target));
      }
    }

    public static class Node<T> {
      private final T value;
      private Node(final T value) {
        this.value = value;
      }
      public T getValue() {
        return value;
      }
      @Override
      public String toString() {
        return "Node{ " + value + " }";
      }
    }

    public static class Edge<T> {
      private final Node<T> source;
      private final Node<T> target;
      private Edge(final Node<T> source, final Node<T> target) {
        this.source = source;
        this.target = target;
      }
      public Node<T> getSource() {
        return source;
      }
      public Node<T> getTarget() {
        return target;
      }
      @Override
      public String toString() {
        return "Edge { source = " + source + ", target = " + target + " }";
      }
    }

    public static class Component<T> {
      private final Graph<T> graph;
      private final Collection<Node<T>> nodes;
      Component(final Graph<T> graph, final Collection<Node<T>> nodes) {
        this.graph = graph;
        this.nodes = nodes;
      }
      public Graph<T> getGraph() {
        return graph;
      }
      public Collection<Node<T>> getNodes() {
        return nodes;
      }
      @Override
      public boolean equals(final Object obj) {
        return (obj instanceof Component)
                && Objects.equal(getGraph(), ((Component<?>) obj).getGraph())
                && Objects.equal(getNodes(), ((Component<?>) obj).getNodes());
      }
      @Override
      public int hashCode() {
        return Objects.hashCode(getGraph(), getNodes());
      }
      @Override
      public String toString() {
        return "Component { nodes = " + nodes + " }";
      }
    }

    static class DFSFrame<T> {
      final T value;
      final Iterator<T> iter;
      DFSFrame(final T value, final Iterator<T> iter) {
        this.value = value;
        this.iter = iter;
      }
    }

    private static class TarjanNode<T> {
      final Node<T> node;
      final int index;
      int lowIndex;
      boolean onStack = false;
      boolean visited = false;
      TarjanNode(final Node<T> node, final int index) {
        this.node = node;
        this.index = index;
        this.lowIndex = index;
      }
    }

  }

}