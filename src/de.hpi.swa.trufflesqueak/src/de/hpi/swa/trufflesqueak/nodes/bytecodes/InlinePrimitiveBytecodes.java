/*
 * Copyright (c) 2017-2022 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2022 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import org.bouncycastle.util.Arrays;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObjectWithClassAndHash;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectAtPut0Node;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectClassNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackPopNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackPushNode;

public final class InlinePrimitiveBytecodes {
    protected abstract static class AbstractPushNode extends AbstractInstrumentableBytecodeNode {
        @Child protected FrameStackPushNode pushNode = FrameStackPushNode.create();

        protected AbstractPushNode(final CompiledCodeObject code, final int index) {
            super(code, index, 1);
        }
    }

    protected abstract static class AbstractNullaryInlinePrimitiveNode extends AbstractPushNode {
        @Child protected FrameStackPopNode popNode = FrameStackPopNode.create();

        protected AbstractNullaryInlinePrimitiveNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }
    }

    protected abstract static class AbstractUnaryInlinePrimitiveNode extends AbstractPushNode {
        @Child protected FrameStackPopNode pop1Node = FrameStackPopNode.create();
        @Child protected FrameStackPopNode pop2Node = FrameStackPopNode.create();

        protected AbstractUnaryInlinePrimitiveNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }
    }

    protected abstract static class AbstractTrinaryInlinePrimitiveNode extends AbstractPushNode {
        @Child protected FrameStackPopNode pop1Node = FrameStackPopNode.create();
        @Child protected FrameStackPopNode pop2Node = FrameStackPopNode.create();
        @Child protected FrameStackPopNode pop3Node = FrameStackPopNode.create();

        protected AbstractTrinaryInlinePrimitiveNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }
    }

    protected abstract static class AbstractQuaternaryInlinePrimitiveNode extends AbstractPushNode {
        @Child protected FrameStackPopNode pop1Node = FrameStackPopNode.create();
        @Child protected FrameStackPopNode pop2Node = FrameStackPopNode.create();
        @Child protected FrameStackPopNode pop3Node = FrameStackPopNode.create();
        @Child protected FrameStackPopNode pop4Node = FrameStackPopNode.create();

        protected AbstractQuaternaryInlinePrimitiveNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }
    }

    protected static final class PrimClassNode extends AbstractNullaryInlinePrimitiveNode {
        @Child private SqueakObjectClassNode classNode = SqueakObjectClassNode.create();

        protected PrimClassNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.execute(frame, classNode.executeLookup(popNode.execute(frame)));
        }
    }

    protected static final class PrimNumSlotsNode extends AbstractNullaryInlinePrimitiveNode {
        protected PrimNumSlotsNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            throw SqueakException.create("Not yet implemented"); // TODO
        }
    }

    protected static final class PrimBasicSizeNode extends AbstractNullaryInlinePrimitiveNode {
        protected PrimBasicSizeNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            throw SqueakException.create("Not yet implemented"); // TODO
        }
    }

    protected static final class PrimNumBytesNode extends AbstractNullaryInlinePrimitiveNode {
        protected PrimNumBytesNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            final Object receiver = popNode.execute(frame);
            final long numBytes;
            if (receiver instanceof CompiledCodeObject) {
                numBytes = ((CompiledCodeObject) receiver).getBytes().length;
            } else {
                numBytes = ((NativeObject) receiver).getByteLength();
            }
            pushNode.execute(frame, numBytes);
        }
    }

    protected static final class PrimNumShortsNode extends AbstractNullaryInlinePrimitiveNode {
        protected PrimNumShortsNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.execute(frame, (long) ((NativeObject) popNode.execute(frame)).getShortLength());
        }
    }

    protected static final class PrimNumWordsNode extends AbstractNullaryInlinePrimitiveNode {
        protected PrimNumWordsNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.execute(frame, (long) ((NativeObject) popNode.execute(frame)).getIntLength());
        }
    }

    protected static final class PrimNumDoubleWordsNode extends AbstractNullaryInlinePrimitiveNode {
        protected PrimNumDoubleWordsNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.execute(frame, (long) ((NativeObject) popNode.execute(frame)).getLongLength());
        }
    }

    protected static final class PrimIdentityHashNode extends AbstractNullaryInlinePrimitiveNode {
        protected PrimIdentityHashNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.execute(frame, ((AbstractSqueakObjectWithClassAndHash) popNode.execute(frame)).getSqueakHash());
        }
    }

    protected static final class PrimIdentityHashSmallIntegerNode extends AbstractNullaryInlinePrimitiveNode {
        protected PrimIdentityHashSmallIntegerNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.execute(frame, popNode.execute(frame));
        }
    }

    protected static final class PrimIdentityHashCharacterNode extends AbstractNullaryInlinePrimitiveNode {
        protected PrimIdentityHashCharacterNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.execute(frame, (long) (char) popNode.execute(frame));
        }
    }

    protected static final class PrimIdentityHashSmallFloatNode extends AbstractNullaryInlinePrimitiveNode {
        protected PrimIdentityHashSmallFloatNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.execute(frame, Double.doubleToRawLongBits((double) popNode.execute(frame)));
        }
    }

    protected static final class PrimIdentityHashBehaviorNode extends AbstractNullaryInlinePrimitiveNode {
        protected PrimIdentityHashBehaviorNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.execute(frame, ((ClassObject) popNode.execute(frame)).getSqueakHash());
        }
    }

    protected static final class PrimImmediateAsIntegerCharacterNode extends AbstractNullaryInlinePrimitiveNode {
        protected PrimImmediateAsIntegerCharacterNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.execute(frame, (long) (char) popNode.execute(frame));
        }
    }

    protected static final class PrimImmediateAsIntegerSmallFloatNode extends AbstractNullaryInlinePrimitiveNode {
        protected PrimImmediateAsIntegerSmallFloatNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.execute(frame, Double.doubleToRawLongBits((double) popNode.execute(frame)));
        }
    }

    protected static final class PrimImmediateAsFloatNode extends AbstractNullaryInlinePrimitiveNode {
        protected PrimImmediateAsFloatNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.execute(frame, (double) (long) popNode.execute(frame));
        }
    }

    protected static final class PrimSmallIntegerAddNode extends AbstractUnaryInlinePrimitiveNode {
        protected PrimSmallIntegerAddNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.execute(frame, (long) pop1Node.execute(frame) + (long) pop2Node.execute(frame));
        }
    }

    protected static final class PrimSmallIntegerSubtractNode extends AbstractUnaryInlinePrimitiveNode {
        protected PrimSmallIntegerSubtractNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.execute(frame, (long) pop1Node.execute(frame) - (long) pop2Node.execute(frame));
        }
    }

    protected static final class PrimSmallIntegerMultiplyNode extends AbstractUnaryInlinePrimitiveNode {
        protected PrimSmallIntegerMultiplyNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.execute(frame, (long) pop1Node.execute(frame) * (long) pop2Node.execute(frame));
        }
    }

    protected static final class PrimSmallIntegerDivideNode extends AbstractUnaryInlinePrimitiveNode {
        protected PrimSmallIntegerDivideNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.execute(frame, (long) pop1Node.execute(frame) / (long) pop2Node.execute(frame));
        }
    }

    protected static final class PrimSmallIntegerFloorDivideNode extends AbstractUnaryInlinePrimitiveNode {
        protected PrimSmallIntegerFloorDivideNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.execute(frame, Math.floorDiv((long) pop1Node.execute(frame), (long) pop2Node.execute(frame)));
        }
    }

    protected static final class PrimSmallIntegerFloorModNode extends AbstractUnaryInlinePrimitiveNode {
        protected PrimSmallIntegerFloorModNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.execute(frame, Math.floorMod((long) pop1Node.execute(frame), (long) pop2Node.execute(frame)));
        }
    }

    protected static final class PrimSmallIntegerQuoNode extends AbstractUnaryInlinePrimitiveNode {
        protected PrimSmallIntegerQuoNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.execute(frame, (long) pop1Node.execute(frame) / (long) pop2Node.execute(frame));
        }
    }

    protected static final class PrimSmallIntegerBitAndNode extends AbstractUnaryInlinePrimitiveNode {
        protected PrimSmallIntegerBitAndNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.execute(frame, (long) pop1Node.execute(frame) & (long) pop2Node.execute(frame));
        }
    }

    protected static final class PrimSmallIntegerBitOrNode extends AbstractUnaryInlinePrimitiveNode {
        protected PrimSmallIntegerBitOrNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.execute(frame, (long) pop1Node.execute(frame) | (long) pop2Node.execute(frame));
        }
    }

    protected static final class PrimSmallIntegerBitXorNode extends AbstractUnaryInlinePrimitiveNode {
        protected PrimSmallIntegerBitXorNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.execute(frame, (long) pop1Node.execute(frame) ^ (long) pop2Node.execute(frame));
        }
    }

    protected static final class PrimSmallIntegerBitShiftLeftNode extends AbstractUnaryInlinePrimitiveNode {
        protected PrimSmallIntegerBitShiftLeftNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.execute(frame, (long) pop1Node.execute(frame) << (long) pop2Node.execute(frame));
        }
    }

    protected static final class PrimSmallIntegerBitShiftRightNode extends AbstractUnaryInlinePrimitiveNode {
        protected PrimSmallIntegerBitShiftRightNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.execute(frame, (long) pop1Node.execute(frame) >> (long) pop2Node.execute(frame));
        }
    }

    protected static final class PrimSmallIntegerGreaterThanNode extends AbstractUnaryInlinePrimitiveNode {
        protected PrimSmallIntegerGreaterThanNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.execute(frame, BooleanObject.wrap((long) pop1Node.execute(frame) > (long) pop2Node.execute(frame)));
        }
    }

    protected static final class PrimSmallIntegerLessThanNode extends AbstractUnaryInlinePrimitiveNode {
        protected PrimSmallIntegerLessThanNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.execute(frame, BooleanObject.wrap((long) pop1Node.execute(frame) < (long) pop2Node.execute(frame)));
        }
    }

    protected static final class PrimSmallIntegerGreaterOrEqualNode extends AbstractUnaryInlinePrimitiveNode {
        protected PrimSmallIntegerGreaterOrEqualNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.execute(frame, BooleanObject.wrap((long) pop1Node.execute(frame) >= (long) pop2Node.execute(frame)));
        }
    }

    protected static final class PrimSmallIntegerLessOrEqualNode extends AbstractUnaryInlinePrimitiveNode {
        protected PrimSmallIntegerLessOrEqualNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.execute(frame, BooleanObject.wrap((long) pop1Node.execute(frame) <= (long) pop2Node.execute(frame)));
        }
    }

    protected static final class PrimSmallIntegerEqualNode extends AbstractUnaryInlinePrimitiveNode {
        protected PrimSmallIntegerEqualNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.execute(frame, BooleanObject.wrap((long) pop1Node.execute(frame) == (long) pop2Node.execute(frame)));
        }
    }

    protected static final class PrimSmallIntegerNotEqualNode extends AbstractUnaryInlinePrimitiveNode {
        protected PrimSmallIntegerNotEqualNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.execute(frame, BooleanObject.wrap((long) pop1Node.execute(frame) != (long) pop2Node.execute(frame)));
        }
    }

    protected static final class PrimByteAtNode extends AbstractUnaryInlinePrimitiveNode {
        protected PrimByteAtNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            final long atIndex = (long) pop2Node.execute(frame);
            final NativeObject receiver = (NativeObject) pop1Node.execute(frame);
            pushNode.execute(frame, (long) receiver.getByte(atIndex));
        }
    }

    protected static final class PrimShortAtNode extends AbstractUnaryInlinePrimitiveNode {
        protected PrimShortAtNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            final long atIndex = (long) pop2Node.execute(frame);
            final NativeObject receiver = (NativeObject) pop1Node.execute(frame);
            pushNode.execute(frame, (long) receiver.getShort(atIndex));
        }
    }

    protected static final class PrimWordAtNode extends AbstractUnaryInlinePrimitiveNode {
        protected PrimWordAtNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            final long atIndex = (long) pop2Node.execute(frame);
            final NativeObject receiver = (NativeObject) pop1Node.execute(frame);
            pushNode.execute(frame, (long) receiver.getInt(atIndex));
        }
    }

    protected static final class PrimDoubleWordAtNode extends AbstractUnaryInlinePrimitiveNode {
        protected PrimDoubleWordAtNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            final long atIndex = (long) pop2Node.execute(frame);
            final NativeObject receiver = (NativeObject) pop1Node.execute(frame);
            pushNode.execute(frame, receiver.getLong(atIndex));
        }
    }

    protected static final class PrimByteAtPutNode extends AbstractTrinaryInlinePrimitiveNode {
        protected PrimByteAtPutNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            final long value = (long) pop3Node.execute(frame);
            final long atIndex = (long) pop2Node.execute(frame);
            final NativeObject receiver = (NativeObject) pop1Node.execute(frame);
            receiver.setByte(atIndex, (byte) value);
        }
    }

    protected static final class PrimShortAtPutNode extends AbstractTrinaryInlinePrimitiveNode {
        protected PrimShortAtPutNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            final long value = (long) pop3Node.execute(frame);
            final long atIndex = (long) pop2Node.execute(frame);
            final NativeObject receiver = (NativeObject) pop1Node.execute(frame);
            receiver.setShort(atIndex, (short) value);
        }
    }

    protected static final class PrimWordAtPutNode extends AbstractTrinaryInlinePrimitiveNode {
        protected PrimWordAtPutNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            final long value = (long) pop3Node.execute(frame);
            final long atIndex = (long) pop2Node.execute(frame);
            final NativeObject receiver = (NativeObject) pop1Node.execute(frame);
            receiver.setInt(atIndex, (int) value);
        }
    }

    protected static final class PrimDoubleWordAtPutNode extends AbstractTrinaryInlinePrimitiveNode {
        protected PrimDoubleWordAtPutNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            final long value = (long) pop3Node.execute(frame);
            final long atIndex = (long) pop2Node.execute(frame);
            final NativeObject receiver = (NativeObject) pop1Node.execute(frame);
            receiver.setLong(atIndex, value);
        }
    }

    protected static final class PrimByteEqualsNode extends AbstractTrinaryInlinePrimitiveNode {
        protected PrimByteEqualsNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            // TODO: Make use of `final long length = (long) pop3Node.execute(frame);`
            final NativeObject argument = (NativeObject) pop2Node.execute(frame);
            final NativeObject receiver = (NativeObject) pop1Node.execute(frame);
            pushNode.execute(frame, BooleanObject.wrap(Arrays.areEqual(receiver.getByteStorage(), argument.getByteStorage())));
        }
    }

    protected static final class PrimFillFromToWithNode extends AbstractQuaternaryInlinePrimitiveNode {
        @Child private SqueakObjectAtPut0Node atPutNode = SqueakObjectAtPut0Node.create();

        protected PrimFillFromToWithNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            final Object value = (long) pop4Node.execute(frame);
            final long to = (long) pop3Node.execute(frame);
            final long from = (long) pop2Node.execute(frame);
            final Object receiver = pop1Node.execute(frame);
            // TODO: maybe there's a more efficient way to fill pointers object?
            for (long i = from; i < to; i++) {
                atPutNode.execute(receiver, i, value);
            }
            pushNode.execute(frame, receiver);
        }
    }
}
