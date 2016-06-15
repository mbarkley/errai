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
import java.util.function.Consumer;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.jboss.errai.flow.api.Step;
import org.jboss.errai.flow.cdi.api.FlowInput;
import org.jboss.errai.flow.cdi.api.FlowOutput;

@ApplicationScoped
public class CDIStepFactory {

    private static class StepFrame {
        final Object input;
        final Consumer<?> callback;
        final Runnable closer;
        StepFrame( final Object input, final Consumer<?> callback, final Runnable closer ) {
            this.input = input;
            this.callback = callback;
            this.closer = closer;
        }
    }

    private final Deque<StepFrame> inputStack = new LinkedList<>();

    public <INPUT, OUTPUT> Step<INPUT, OUTPUT> createCdiStep( final Runnable starter,
                                                                 final Runnable closer,
                                                                 final String name ) {
        return new Step<INPUT, OUTPUT>() {

            @Override
            public void execute( final INPUT input,
                                 final Consumer<OUTPUT> callback ) {
                final StepFrame frame = new StepFrame( input, callback, closer );
                inputStack.push( frame );
                starter.run();
            }

            @Override
            public String getName() {
                return name;
            }
        };
    }

    @SuppressWarnings( { "unchecked", "rawtypes" } )
    private void consumeOutput( final Object t ) {
        final StepFrame frame = inputStack.pop();
        frame.closer.run();
        ((Consumer) frame.callback).accept( t );
    }

    private Object getInput() {
        return inputStack.peek().input;
    }

    @SuppressWarnings( "unchecked" )
    @Produces @ApplicationScoped
    public <T> FlowInput<T> createInput() {
        return () -> (T) getInput();
    }

    @Produces @ApplicationScoped
    public <T> FlowOutput<T> createOutput() {
        return ( final T output ) -> consumeOutput( output );
    }
}
