/*
 * Copyright (c) 2017-2022 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2022 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.context.frame;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public abstract class GetOrCreateContextNode extends AbstractNode {

    public static GetOrCreateContextNode create() {
        return GetOrCreateContextNodeGen.create();
    }

    public static final ContextObject getOrCreateUncached(final Frame frame) {
        CompilerAsserts.neverPartOfCompilation();
        final ContextObject context = FrameAccess.getContext(frame);
        if (context != null) {
            return context;
        } else {
            final CompiledCodeObject code = FrameAccess.getMethodOrBlock(frame);
            return ContextObject.create(code.getSqueakClass().getImage(), frame.materialize(), code);
        }
    }

    public abstract ContextObject executeGet(VirtualFrame frame);

    @Specialization
    protected final ContextObject doGetOrCreate(final VirtualFrame frame,
                    @Cached("getMethodOrBlock(frame)") final CompiledCodeObject code,
                    @Cached("createCountingProfile()") final ConditionProfile hasContextProfile) {
        final ContextObject context = FrameAccess.getContext(frame);
        if (hasContextProfile.profile(context != null)) {
            return context;
        } else {
            return ContextObject.create(getContext(), frame.materialize(), code);
        }
    }
}
