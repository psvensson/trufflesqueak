/*
 * Copyright (c) 2017-2021 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.GenerateWrapper;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.ProbeNode;

@GenerateWrapper
public abstract class AbstractExecuteContextNode extends AbstractNode implements InstrumentableNode {

    public abstract Object execute(VirtualFrame frame, int startPC);

    @Override
    public WrapperNode createWrapper(final ProbeNode probeNode) {
        return new AbstractExecuteContextNodeWrapper(this, probeNode);
    }
}
