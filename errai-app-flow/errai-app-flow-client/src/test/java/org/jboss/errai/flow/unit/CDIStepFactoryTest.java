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

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.jboss.errai.flow.api.FlowInput;
import org.jboss.errai.flow.api.FlowOutput;
import org.jboss.errai.flow.api.Step;
import org.jboss.errai.flow.client.local.CDIStepFactory;
import org.jboss.errai.flow.util.Ref;
import org.junit.Before;
import org.junit.Test;

public class CDIStepFactoryTest {

    private CDIStepFactory factory;

    @Before
    public void setup() {
        factory = new CDIStepFactory();
    }

    @Test
    public void sequentialStepsInputsAndOutputs() throws Exception {
        final List<Integer> inputs = new ArrayList<>();
        final List<Integer> outputs = new ArrayList<>();
        final FlowInput<Integer> flowInput = factory.createInput();
        final FlowOutput<Integer> flowOutput = factory.createOutput();

        final Step<Integer, Integer> step = factory.createCdiStep( () -> {
            inputs.add( flowInput.get() );
            final int output = flowInput.get() + 1;
            outputs.add( output );
            flowOutput.submit( output );
        }, () -> {}, "Adder" );

        step.execute( 0, n1 -> {
            step.execute( n1, n2 -> {
                step.execute( n2, n3 -> {} );
            } );
        } );

        assertEquals( asList( 0, 1, 2 ), inputs );
        assertEquals( asList( 1, 2, 3 ), outputs );
    }

    @Test
    public void nestedStepsInputsAndOutputs() throws Exception {
        final List<Integer> inputs = new ArrayList<>();
        final List<Integer> outputs = new ArrayList<>();
        final FlowInput<Integer> flowInput = factory.createInput();
        final FlowOutput<Integer> flowOutput = factory.createOutput();

        final Function<Function<Integer, Integer>, Step<Integer, Integer>> stepFunc =
                consumer -> factory.createCdiStep( () -> {
            final Integer beforeInput = flowInput.get();
            inputs.add( beforeInput );
            final int intermediateOutput = beforeInput + 1;
            final Integer finalOutput = consumer.apply( intermediateOutput );
            assertEquals( beforeInput, flowInput.get() );
            outputs.add( finalOutput );
            flowOutput.submit( finalOutput );
        }, () -> {}, "Adder" );

        stepFunc.apply( n1 -> {
            final Ref<Integer> ref1 = new Ref<>();
            stepFunc.apply( n2 -> {
                final Ref<Integer> ref2 = new Ref<>();
                stepFunc.apply( n3 -> n3 + 1 ).execute( n2, val -> { ref2.val = val; } );
                return ref2.val;
            } ).execute( n1, val -> { ref1.val = val; } );
            return ref1.val;
        } ).execute( 0, val -> {} );

        assertEquals( asList( 0, 1, 2 ), inputs );
        assertEquals( asList( 4, 4, 4 ), outputs );
    }

}
