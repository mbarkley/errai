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


package org.jboss.errai.flow.unit;

import static org.jboss.errai.flow.client.local.StepUtil.identity;
import static org.jboss.errai.flow.client.local.StepUtil.wrap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.function.Function;

import org.jboss.errai.flow.api.AppFlow;
import org.jboss.errai.flow.api.AppFlowExecutor;
import org.jboss.errai.flow.api.AppFlowFactory;
import org.jboss.errai.flow.api.Step;
import org.jboss.errai.flow.api.Unit;
import org.jboss.errai.flow.client.local.RuntimeAppFlowExecutor;
import org.jboss.errai.flow.client.local.RuntimeAppFlowFactory;
import org.jboss.errai.flow.client.local.StepUtil;
import org.jboss.errai.flow.util.Ref;
import org.junit.Before;
import org.junit.Test;

public class FlowBuildAndExecutionTest {

    private AppFlowFactory factory;
    private AppFlowExecutor executor;

    @Before
    public void setup() {
        factory = new RuntimeAppFlowFactory();
        executor = new RuntimeAppFlowExecutor();
    }

    @Test
    public void sequentialSteps() throws Exception {
        final Step<Unit, Integer> zero = wrap( "Produce Zero", () -> 0 );
        final Step<Integer, Integer> add10 = wrap( "Increment", (final Integer n) -> n + 10);
        final Step<Object, String> stringify = wrap( "Stringify", o -> o.toString() );
        final Step<String, String> reverse = wrap( "Reverse String", (final String s) -> new StringBuilder( s ).reverse().toString() );

        final AppFlow<Unit, String> flow = factory
            .buildFrom( zero )
            .andThen( add10 )
            .andThen( stringify )
            .andThen( reverse );

        assertEquals( "01", getSyncFlowOutput( flow ) );
    }

    @Test
    public void simpleTransition() throws Exception {
        final Step<Unit, Boolean> t = wrap( "True", () -> true );
        final Step<Unit, Boolean> f = wrap( "False", () -> false );
        final Step<Unit, String> tString = wrap( "True String", () -> "true" );
        final Step<Unit, String> fString = wrap( "False String", () -> "false" );
        final Function<Boolean, AppFlow<Unit, String>> transition = b -> factory.buildFrom( b ? tString : fString );

        final AppFlow<Unit, String> tFlow = factory
            .buildFrom( t )
            .transitionTo( transition );

        final AppFlow<Unit, String> fFlow = factory
            .buildFrom( f )
            .transitionTo( transition );

        assertEquals( "true", getSyncFlowOutput( tFlow ) );
        assertEquals( "false", getSyncFlowOutput( fFlow ) );
    }

    @Test
    public void sequentialStepsWithTransformationsAfter() throws Exception {
        final Step<Unit, Integer> zero = wrap( "Produce Zero", () -> 0 );
        final Function<Integer, Integer> add10 = n -> n + 10;
        final Function<Object, String> stringify = o -> o.toString();
        final Function<String, String> reverse = s -> new StringBuilder( s ).reverse().toString();

        final AppFlow<Unit, String> flow = factory
            .buildFrom( zero )
            .andThen( add10 )
            .andThen( stringify )
            .andThen( reverse );

        assertEquals( "01", getSyncFlowOutput( flow ) );
    }

    @Test
    public void sequentialFlowsWithPreTransformation() throws Exception {
        final Step<Unit, Integer> zero = wrap( "Produce Zero", () -> 0 );
        final Function<Integer, Integer> add10 = n -> n + 10;
        final Function<Object, String> stringify = o -> o.toString();
        final Step<String, String> reverse = wrap( "Reverse String", (final String s) -> new StringBuilder( s ).reverse().toString() );

        final AppFlow<Unit, String> flow = factory
            .buildFrom( zero )
            .andThen( factory
                          .buildFrom( reverse )
                          .butFirst( stringify )
                          .butFirst( add10 ) );

        assertEquals( "01", getSyncFlowOutput( flow ) );
    }

    @Test
    public void executeFlowInStep() throws Exception {
        final Step<Unit, Integer> zero = wrap( "Produce Zero", () -> 0 );
        final Step<Integer, Integer> add10 = wrap( "Add 10", n -> n + 10 );
        final AppFlow<Integer, Integer> add10Flow = factory.buildFrom( add10 );
        final Step<Integer, Integer> stepCallingFlow = wrap( "Step Calling Flow", (n, callback) -> executor.execute( n, add10Flow, callback ) );

        final AppFlow<Unit, Integer> flow = factory
            .buildFrom( zero )
            .andThen( stepCallingFlow );

        assertEquals( Integer.valueOf( 10 ), getSyncFlowOutput( flow ) );
    }

    @Test
    public void callingStepAndThenOnFlowDoesNotModifyOriginalFlow() throws Exception {
        final Step<Unit, Integer> zero = wrap( "Produce Zero", () -> 0 );
        final Step<Integer, Integer> add10 = wrap( "Add 10", n -> n + 10 );
        final Step<Integer, Unit> throwing = wrap( "Throwing", n -> { throw new RuntimeException(); } );

        final AppFlow<Unit, Integer> original =
                factory.buildFrom( zero )
                       .andThen( add10 );

        original.andThen( throwing );

        try {
            executor.execute( original );
        } catch ( final RuntimeException e ) {
            fail();
        }
    }

    @Test
    public void callingStepButFirstOnFlowDoesNotModifyOriginalFlow() throws Exception {
        final Step<Unit, Integer> zero = wrap( "Produce Zero", () -> 0 );
        final Step<Integer, Integer> add10 = wrap( "Add 10", n -> n + 10 );
        final Step<Unit, Unit> throwing = wrap( "Throwing", n -> { throw new RuntimeException(); } );

        final AppFlow<Unit, Integer> original =
                factory.buildFrom( zero )
                       .andThen( add10 );

        original.butFirst( throwing );

        try {
            executor.execute( original );
        } catch ( final RuntimeException e ) {
            fail();
        }
    }

    @Test
    public void callingFlowAndThenOnFlowDoesNotModifyOriginalFlow() throws Exception {
        final Step<Unit, Integer> zero = wrap( "Produce Zero", () -> 0 );
        final Step<Integer, Integer> add10 = wrap( "Add 10", n -> n + 10 );
        final AppFlow<Integer, Unit> throwing = factory.buildFrom( wrap( "Throwing", n -> { throw new RuntimeException(); } ) );

        final AppFlow<Unit, Integer> original =
                factory.buildFrom( zero )
                       .andThen( add10 );

        original.andThen( throwing );

        try {
            executor.execute( original );
        } catch ( final RuntimeException e ) {
            fail();
        }
    }

    @Test
    public void callingFunctionAndThenOnFlowDoesNotModifyOriginalFlow() throws Exception {
        final Step<Unit, Integer> zero = wrap( "Produce Zero", () -> 0 );
        final Step<Integer, Integer> add10 = wrap( "Add 10", n -> n + 10 );
        final Function<Integer, Unit> throwing = n -> { throw new RuntimeException(); };

        final AppFlow<Unit, Integer> original =
                factory.buildFrom( zero )
                       .andThen( add10 );

        original.andThen( throwing );

        try {
            executor.execute( original );
        } catch ( final RuntimeException e ) {
            fail();
        }
    }

    @Test
    public void callingFunctionButFirstOnFlowDoesNotModifyOriginalFlow() throws Exception {
        final Step<Unit, Integer> zero = wrap( "Produce Zero", () -> 0 );
        final Step<Integer, Integer> add10 = wrap( "Add 10", n -> n + 10 );
        final Function<Unit, Unit> throwing = n -> { throw new RuntimeException(); };

        final AppFlow<Unit, Integer> original =
                factory.buildFrom( zero )
                       .andThen( add10 );

        original.butFirst( throwing );

        try {
            executor.execute( original );
        } catch ( final RuntimeException e ) {
            fail();
        }
    }

    @Test
    public void callingTransitionToOnFlowDoesNotModifyOriginalFlow() throws Exception {
        final Step<Unit, Integer> zero = wrap( "Produce Zero", () -> 0 );
        final Step<Integer, Integer> add10 = wrap( "Add 10", n -> n + 10 );
        final Function<Integer, AppFlow<Unit, Unit>> throwing = n -> { throw new RuntimeException(); };

        final AppFlow<Unit, Integer> original =
                factory.buildFrom( zero )
                       .andThen( add10 );

        original.andThen( throwing );

        try {
            executor.execute( original );
        } catch ( final RuntimeException e ) {
            fail();
        }
    }

    @Test
    public void transitionFlowReceviesInitialInput() throws Exception {
        final Step<Integer, Integer> doubler = wrap( "Doubler", n -> 2 * n );
        final Step<Integer, Integer> add10 = wrap( "Add 10", n -> n + 10 );

        final AppFlow<Integer, Integer> flow = factory
            .buildFrom( doubler )
            .transitionTo( ignore -> factory.buildFrom( add10 ) );

        assertEquals( Integer.valueOf( 11 ), getSyncFlowOutput( 1, flow ) );
    }

    @Test
    public void flowFromSecondTransitionToInSequenceReceivesInitialInput() throws Exception {
        final Function<Integer, AppFlow<Integer, Integer>> identityTransition = ignore -> factory.buildFrom( identity() );

        final AppFlow<Integer, Integer> flow = factory
            .buildFrom( StepUtil.<Integer>identity() )
            .transitionTo( identityTransition )
            .andThen( n -> n + 1 )
            .transitionTo( identityTransition );

        assertEquals( Integer.valueOf( 1 ), getSyncFlowOutput( 1, flow ) );
    }

    private <OUTPUT> OUTPUT getSyncFlowOutput( final AppFlow<Unit, OUTPUT> flow ) {
        return getSyncFlowOutput( Unit.INSTANCE, flow );
    }

    private <INPUT, OUTPUT> OUTPUT getSyncFlowOutput( final INPUT in, final AppFlow<INPUT, OUTPUT> flow ) {
        final Ref<OUTPUT> ref = new Ref<>();
        executor.execute( in, flow, val -> { ref.val = val; } );

        return ref.val;
    }

}
