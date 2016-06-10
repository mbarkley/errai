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


package org.jboss.errai.flow.client.local;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Optional;
import java.util.function.Consumer;

class FlowContext {

    private Optional<Object> initialInput = Optional.empty();
    private Optional<Object> lastOutput = Optional.empty();
    private final RuntimeAppFlow<?, ?> flow;
    private Optional<FlowNode<?, ?>> currentNode = Optional.empty();

    private final Deque<Consumer<?>> callbacks = new LinkedList<>();


    FlowContext( final RuntimeAppFlow<?, ?> flow ) {
        this.flow = flow;
    }

    Object getInitialInput() {
        return initialInput.orElseThrow( () -> new IllegalStateException( "Can't get initial input before flow is started." ) );
    }

    Optional<Object> pollOutput() {
        return lastOutput;
    }

    @SuppressWarnings( { "unchecked", "rawtypes" } )
    void pushOutput(final Object value) {
        lastOutput = Optional.of( value );
        currentNode = (Optional) currentNode.flatMap( node -> node.next );
    }

    void start( final Object initialInput ) {
        if ( isStarted() ) {
            throw new RuntimeException( "Process has already been started." );
        }

        this.initialInput = Optional.of( initialInput );
        currentNode = Optional.of( flow.start );
        lastOutput = Optional.of( initialInput );
    }

    boolean isStarted() {
        return currentNode.isPresent() || lastOutput.isPresent();
    }

    boolean isFinished() {
        return !currentNode.isPresent() && lastOutput.isPresent();
    }

    Optional<FlowNode<?, ?>> getCurrentNode() {
        return currentNode;
    }

    RuntimeAppFlow<?, ?> getProcess() {
        return flow;
    }

    public void pushCallback( final Consumer<?> callback ) {
        callbacks.push( callback );
    }

    public boolean hasCallbacks() {
        return !callbacks.isEmpty();
    }

    @SuppressWarnings( { "unchecked", "rawtypes" } )
    public void applyCallbackAndPop( final Object value ) {
        final Consumer callback = callbacks.peek();
        callback.accept( value );
        callbacks.pop();
    }

}
