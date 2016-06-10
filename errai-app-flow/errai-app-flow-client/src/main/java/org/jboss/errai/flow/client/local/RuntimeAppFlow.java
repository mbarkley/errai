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

import java.util.Optional;
import java.util.function.Function;

import org.jboss.errai.flow.api.AppFlow;
import org.jboss.errai.flow.api.Step;

class RuntimeAppFlow<INPUT, OUTPUT> implements AppFlow<INPUT, OUTPUT> {

    FlowNode<INPUT, ?> start;
    FlowNode<?, OUTPUT> end;

    public RuntimeAppFlow( final Step<INPUT, OUTPUT> step ) {
        final StepNode<INPUT, OUTPUT> stepNode = new StepNode<>( step );
        start = stepNode;
        end = stepNode;
    }

    private RuntimeAppFlow( final FlowNode<INPUT, ?> start, final FlowNode<?, OUTPUT> end ) {
        this.start = start;
        this.end = end;
    }

    @SuppressWarnings( { "unchecked", "rawtypes" } )
    @Override
    public <T> AppFlow<INPUT, T> andThen( final Step<? super OUTPUT, T> nextStep ) {
        final RuntimeAppFlow<INPUT, OUTPUT> copy = copy();
        copy.addLast( new StepNode( nextStep ) );

        return (AppFlow<INPUT, T>) copy;
    }

    @SuppressWarnings( { "unchecked", "rawtypes" } )
    @Override
    public <T> AppFlow<INPUT, T> andThen( final Function<? super OUTPUT, T> transformation ) {
        final RuntimeAppFlow<INPUT, OUTPUT> copy = copy();
        copy.addLast( new TransformationNode( transformation ) );

        return (AppFlow<INPUT, T>) copy;
    }

    @SuppressWarnings( { "unchecked", "rawtypes" } )
    @Override
    public <T> AppFlow<T, OUTPUT> butFirst( final Function<T, ? extends INPUT> transformation ) {
        final RuntimeAppFlow<INPUT, OUTPUT> copy = copy();
        copy.addFirst( new TransformationNode( transformation ) );

        return (AppFlow<T, OUTPUT>) copy;
    }

    @SuppressWarnings( { "unchecked", "rawtypes" } )
    @Override
    public <T> AppFlow<T, OUTPUT> butFirst( final Step<T, ? extends INPUT> prevStep ) {
        final RuntimeAppFlow<INPUT, OUTPUT> copy = copy();
        copy.addFirst( new StepNode( prevStep ) );

        return (AppFlow<T, OUTPUT>) copy;
    }

    @SuppressWarnings( { "unchecked", "rawtypes" } )
    @Override
    public <T> AppFlow<INPUT, T> transitionTo( final Function<? super OUTPUT, AppFlow<INPUT, T>> transition ) {
        final RuntimeAppFlow<INPUT, OUTPUT> copy = copy();
        copy.addLast( new TransitionNode( transition ) );

        return (AppFlow<INPUT, T>) copy;
    }


    @SuppressWarnings( "unchecked" )
    private void addLast( final FlowNode<OUTPUT, ?> node ) {
        node.prev = Optional.of( end );
        end.next = Optional.of( node );
        end = (FlowNode< ? , OUTPUT>) node;
    }

    @SuppressWarnings( "unchecked" )
    private void addFirst( final FlowNode<?, INPUT> node ) {
        node.next = Optional.of( start );
        start.prev = Optional.of( node );
        start = (FlowNode<INPUT, ? >) node;
    }

    @SuppressWarnings( { "rawtypes", "unchecked" } )
    private RuntimeAppFlow<INPUT, OUTPUT> copy() {
        final FlowNode newStart = start.copy();
        FlowNode curCopy = newStart;
        FlowNode curOriginal = start;
        while ( curOriginal.next.isPresent() ) {
            final FlowNode nextOriginal = (FlowNode) curOriginal.next.get();
            final FlowNode nextCopy = nextOriginal.copy();
            curCopy.next = Optional.of( nextCopy );
            nextCopy.prev = Optional.of( curCopy );

            curCopy = nextCopy;
            curOriginal = nextOriginal;
        }

        return new RuntimeAppFlow<>( newStart, curCopy );
    }

}
