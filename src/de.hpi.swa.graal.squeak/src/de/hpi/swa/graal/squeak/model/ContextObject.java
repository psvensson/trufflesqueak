package de.hpi.swa.graal.squeak.model;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.MaterializedFrame;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions;
import de.hpi.swa.graal.squeak.exceptions.SqueakException;
import de.hpi.swa.graal.squeak.image.AbstractImageChunk;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.graal.squeak.nodes.CompiledCodeNodes.CalculcatePCOffsetNode;
import de.hpi.swa.graal.squeak.nodes.GetOrCreateContextNode;
import de.hpi.swa.graal.squeak.util.ArrayUtils;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public final class ContextObject extends AbstractSqueakObject {
    @CompilationFinal protected MaterializedFrame truffleFrame;
    @CompilationFinal(dimensions = 1) protected Object[] pointers;
    @CompilationFinal private boolean hasModifiedSender;
    @CompilationFinal private boolean isDirty;

    public static ContextObject create(final SqueakImageContext image) {
        return new ContextObject(image);
    }

    private ContextObject(final SqueakImageContext image) {
        super(image, image.methodContextClass);
        isDirty = true;
    }

    public static ContextObject create(final SqueakImageContext image, final int size) {
        return new ContextObject(image, size);
    }

    private ContextObject(final SqueakImageContext image, final int size) {
        this(image);
        isDirty = true;
        pointers = ArrayUtils.withAll(CONTEXT.TEMP_FRAME_START + size, null);
    }

    public static ContextObject create(final SqueakImageContext image, final int size, final MaterializedFrame frame) {
        assert frame.getValue(frame.getFrameDescriptor().findFrameSlot(CompiledCodeObject.SLOT_IDENTIFIER.THIS_CONTEXT_OR_MARKER)) instanceof FrameMarker;
        return new ContextObject(image, size, frame);
    }

    private ContextObject(final SqueakImageContext image, final int size, final MaterializedFrame frame) {
        this(image, size);
        isDirty = false;
        truffleFrame = frame;
    }

    public ContextObject(final ContextObject original) {
        super(original.image, original.image.methodContextClass);
        pointers = original.pointers.clone();
        truffleFrame = original.truffleFrame;
        hasModifiedSender = original.hasModifiedSender;
    }

    public void terminate() {
        atput0(CONTEXT.INSTRUCTION_POINTER, image.nil);
        setSender(image.nil); // remove sender without flagging dirty
    }

    @Override
    public void fillin(final AbstractImageChunk chunk) {
        super.fillin(chunk);
        pointers = chunk.getPointers();
    }

    public Object at0(final long longIndex) {
        assert longIndex >= 0;
        final int index = (int) longIndex;
        if (isDirty) {
            if (index == CONTEXT.SENDER_OR_NIL) {
                return getSender(); // sender might need to be reconstructed
            }
            final Object result = pointers[index];
            if (result != null) {
                return result;
            }
            if (truffleFrame == null) {
                return image.nil;
            }
        }
        switch (index) {
            case CONTEXT.SENDER_OR_NIL:
                return GetOrCreateContextNode.getOrCreateFull(truffleFrame, false);
            case CONTEXT.INSTRUCTION_POINTER:
                final CompiledCodeObject blockOrMethod = getMethod();
                final int initalPC;
                if (blockOrMethod instanceof CompiledBlockObject) {
                    initalPC = ((CompiledBlockObject) blockOrMethod).getInitialPC();
                } else {
                    initalPC = ((CompiledMethodObject) blockOrMethod).getInitialPC();
                }
                return (long) FrameUtil.getIntSafe(truffleFrame, getMethod().instructionPointerSlot) + initalPC;
            case CONTEXT.STACKPOINTER:
                return (long) FrameUtil.getIntSafe(truffleFrame, getMethod().stackPointerSlot);
            case CONTEXT.METHOD:
                return truffleFrame.getArguments()[FrameAccess.METHOD];
            case CONTEXT.CLOSURE_OR_NIL:
                final BlockClosureObject closure = (BlockClosureObject) truffleFrame.getArguments()[FrameAccess.CLOSURE_OR_NULL];
                return closure == null ? image.nil : closure;
            case CONTEXT.RECEIVER:
                return truffleFrame.getArguments()[FrameAccess.RECEIVER];
            default:
                return truffleFrame.getValue(getMethod().getStackSlot(index - CONTEXT.TEMP_FRAME_START));
        }
    }

    public void atput0(final long index, final Object value) {
        assert index >= 0 && value != null;
        if (index == CONTEXT.SENDER_OR_NIL) {
            image.traceVerbose("Sender of", this, " set to", value);
            CompilerDirectives.transferToInterpreterAndInvalidate();
            hasModifiedSender = true;
        }
        if (!isDirty) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            isDirty = true;
        }
        assert value != null; // null indicates a problem
        pointers[(int) index] = value;
    }

    public CompiledCodeObject getClosureOrMethod() {
        final BlockClosureObject closure = getClosure();
        if (closure != null) {
            return closure.getCompiledBlock();
        }
        return getMethod();
    }

    public boolean hasMethod() {
        return at0(CONTEXT.METHOD) instanceof CompiledCodeObject;
    }

    public CompiledCodeObject getMethod() {
        return (CompiledCodeObject) at0(CONTEXT.METHOD);
    }

    public AbstractSqueakObject shallowCopy() {
        return new ContextObject(this);
    }

    public boolean isDirty() {
        return isDirty;
    }

    public boolean hasModifiedSender() {
        return hasModifiedSender;
    }

    public boolean hasVirtualSender() {
        return pointers[CONTEXT.SENDER_OR_NIL] instanceof FrameMarker;
    }

    public AbstractSqueakObject getSender() {
        return getSender(true);
    }

    public AbstractSqueakObject getSender(final boolean force) {
        final Object sender = pointers[CONTEXT.SENDER_OR_NIL];
        if (sender instanceof ContextObject) {
            return (AbstractSqueakObject) sender;
        } else if (sender instanceof NilObject) {
            return (AbstractSqueakObject) sender;
        }
        final Object senderOrMarker = truffleFrame.getArguments()[FrameAccess.SENDER_OR_SENDER_MARKER];
        final AbstractSqueakObject actualSender;
        if (senderOrMarker instanceof FrameMarker) {
            final Frame frame = FrameAccess.findFrameForMarker((FrameMarker) senderOrMarker);
            actualSender = GetOrCreateContextNode.getOrCreateFull(frame.materialize(), false);
            assert actualSender != null;
        } else {
            actualSender = (AbstractSqueakObject) senderOrMarker;
        }
        if (force) {
            setSender(actualSender);
        }
        return actualSender;
    }

    // should only be used when sender is not nil
    public ContextObject getNotNilSender() {
        return (ContextObject) getSender();
    }

    /*
     * Set sender without flagging context as dirty.
     */
    public void setSender(final Object sender) {
        pointers[CONTEXT.SENDER_OR_NIL] = sender;
    }

    public void push(final Object value) {
        assert value != null;
        final long newSP = getStackPointer() + 1;
        assert newSP <= CONTEXT.MAX_STACK_SIZE;
        atStackPut(newSP, value);
        setStackPointer(newSP);
    }

    public long getInstructionPointer() {
        return (long) at0(CONTEXT.INSTRUCTION_POINTER);
    }

    public void setInstructionPointer(final long newPC) {
        assert newPC >= 0;
        atput0(CONTEXT.INSTRUCTION_POINTER, newPC);
    }

    public long getStackPointer() {
        return (long) at0(CONTEXT.STACKPOINTER);
    }

    public void setStackPointer(final long newSP) {
        assert 0 <= newSP && newSP <= CONTEXT.MAX_STACK_SIZE;
        atput0(CONTEXT.STACKPOINTER, newSP);
    }

    @Override
    public String toString() {
        if (hasMethod()) {
            final BlockClosureObject closure = getClosure();
            if (closure != null) {
                return "CTX [] in " + getMethod();
            } else {
                return "CTX " + getMethod();
            }
        } else {
            return "CTX without method";
        }
    }

    public int size() {
        return pointers.length;
    }

    public int instsize() {
        return getSqClass().getBasicInstanceSize();
    }

    public Object[] getPointers() {
        return pointers;
    }

    public Object top() {
        return peek(0);
    }

    public Object peek(final int offset) {
        return atStack(getStackPointer() - offset);
    }

    public Object getReceiver() {
        return at0(CONTEXT.RECEIVER);
    }

    public Object atTemp(final long argumentIndex) {
        return at0(CONTEXT.TEMP_FRAME_START + argumentIndex);
    }

    public void atTempPut(final long argumentIndex, final Object value) {
        atput0(CONTEXT.TEMP_FRAME_START + argumentIndex, value);
    }

    public Object atStack(final long argumentIndex) {
        return at0(CONTEXT.TEMP_FRAME_START - 1 + argumentIndex);
    }

    public void atStackPut(final long argumentIndex, final Object value) {
        atput0(CONTEXT.TEMP_FRAME_START - 1 + argumentIndex, value);
    }

    @Override
    public boolean become(final AbstractSqueakObject other) {
        if (!(other instanceof ContextObject)) {
            throw new PrimitiveExceptions.PrimitiveFailed();
        }
        if (!super.become(other)) {
            throw new SqueakException("Should not fail");
        }
        CompilerDirectives.transferToInterpreterAndInvalidate();
        final Object[] pointers2 = ((ContextObject) other).pointers;
        ((ContextObject) other).pointers = this.pointers;
        pointers = pointers2;
        return true;
    }

    @Override
    public void pointersBecomeOneWay(final Object[] from, final Object[] to, final boolean copyHash) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        for (int i = 0; i < from.length; i++) {
            final Object fromPointer = from[i];
            // skip sender (for performance), pc, and sp
            for (int j = CONTEXT.METHOD; j < size(); j++) {
                final Object newPointer = at0(j);
                if (newPointer == fromPointer) {
                    final Object toPointer = to[i];
                    atput0(j, toPointer);
                    if (copyHash && fromPointer instanceof AbstractSqueakObject && toPointer instanceof AbstractSqueakObject) {
                        ((AbstractSqueakObject) toPointer).setSqueakHash(((AbstractSqueakObject) fromPointer).squeakHash());
                    }
                }
            }
        }
    }

    public BlockClosureObject getClosure() {
        final Object closureOrNil = at0(CONTEXT.CLOSURE_OR_NIL);
        return closureOrNil == image.nil ? null : (BlockClosureObject) closureOrNil;
    }

    public boolean isUnwindContext() {
        return getMethod().isUnwindMarked();
    }

    public Object[] getReceiverAndNArguments(final int numArgs) {
        final Object[] arguments = new Object[1 + numArgs];
        arguments[0] = getReceiver();
        for (int i = 0; i < numArgs; i++) {
            arguments[1 + i] = atTemp(i);
        }
        return arguments;
    }

    public MaterializedFrame getTruffleFrame(final int numArgs) {
        if (!isDirty && truffleFrame != null) {
            return truffleFrame;
        }
        final CompiledCodeObject closureOrMethod = getClosureOrMethod();
        final AbstractSqueakObject sender = getSender();
        final BlockClosureObject closure = getClosure();
        final Object[] frameArgs = getReceiverAndNArguments(numArgs);
        final MaterializedFrame frame = Truffle.getRuntime().createMaterializedFrame(FrameAccess.newWith(closureOrMethod, sender, closure, frameArgs), getMethod().getFrameDescriptor());
        frame.setObject(closureOrMethod.thisContextOrMarkerSlot, this);
        return frame;
    }

    /*
     * Helper function for debugging purposes.
     */
    @TruffleBoundary
    public void printSqStackTrace() {
        ContextObject current = this;
        int numArgsAndCopiedValues;
        while (true) {
            final CompiledCodeObject code = current.getClosureOrMethod();
            if (code instanceof CompiledBlockObject) {
                numArgsAndCopiedValues = ((CompiledBlockObject) code).getNumArgs() + ((CompiledBlockObject) code).getNumCopiedValues();
            } else {
                numArgsAndCopiedValues = ((CompiledMethodObject) code).getNumArgs();
            }
            final Object[] rcvrAndArgs = current.getReceiverAndNArguments(numArgsAndCopiedValues);
            final String[] argumentStrings = new String[rcvrAndArgs.length];
            for (int i = 0; i < rcvrAndArgs.length; i++) {
                argumentStrings[i] = rcvrAndArgs[i].toString();
            }
            image.getOutput().println(String.format("%s #(%s)", current, String.join(", ", argumentStrings)));
            final AbstractSqueakObject sender = current.getSender();
            if (sender.isNil()) {
                break;
            } else {
                current = (ContextObject) sender;
            }
        }
    }

    public MaterializedFrame getTruffleFrame() {
        return truffleFrame;
    }

    public boolean hasTruffleFrame() {
        return truffleFrame != null;
    }

    public boolean hasMaterializedSender() {
        return pointers[CONTEXT.SENDER_OR_NIL] != null;
    }

    public void materialize() {
        if (truffleFrame == null) {
            return; // nothing to do
        }
        final Object[] frameArguments = truffleFrame.getArguments();
        if (pointers[CONTEXT.SENDER_OR_NIL] == null) {
            // Materialize sender if sender is a FrameMarker
            final Object senderOrMarker = frameArguments[FrameAccess.SENDER_OR_SENDER_MARKER];
            if (senderOrMarker instanceof FrameMarker) {
                final Frame senderFrame = FrameAccess.findFrameForMarker((FrameMarker) senderOrMarker);
                if (senderFrame == null) {
                    throw new SqueakException("Unable to find senderFrame for FrameMaker");
                }
                setSender(GetOrCreateContextNode.getOrCreateFull(senderFrame.materialize(), false));
            }
        }
        if (isDirty) {
            return; // nothing more to do
        }
        final CompiledCodeObject blockOrMethod = getMethod();

        final long framePC = FrameUtil.getIntSafe(truffleFrame, blockOrMethod.instructionPointerSlot);
        final long frameSP = FrameUtil.getIntSafe(truffleFrame, blockOrMethod.stackPointerSlot);
        atput0(CONTEXT.METHOD, blockOrMethod);
        if (framePC >= 0) {
            setInstructionPointer(framePC + CalculcatePCOffsetNode.create().execute(blockOrMethod));
        } else { // context has been terminated
            atput0(CONTEXT.INSTRUCTION_POINTER, blockOrMethod.image.nil);
        }
        setStackPointer(frameSP + 1); // frame sp is zero-based
        final BlockClosureObject closure = (BlockClosureObject) frameArguments[FrameAccess.CLOSURE_OR_NULL];
        atput0(CONTEXT.CLOSURE_OR_NIL, closure == null ? blockOrMethod.image.nil : closure);
        atput0(CONTEXT.RECEIVER, frameArguments[FrameAccess.RECEIVER]);
    }
}
