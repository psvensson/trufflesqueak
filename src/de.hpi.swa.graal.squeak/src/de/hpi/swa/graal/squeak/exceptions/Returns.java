package de.hpi.swa.graal.squeak.exceptions;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.nodes.ControlFlowException;

import de.hpi.swa.graal.squeak.model.ContextObject;

public final class Returns {
    private abstract static class AbstractReturn extends ControlFlowException {
        @CompilationFinal private static final long serialVersionUID = 1L;
        @CompilationFinal protected final Object returnValue;

        private AbstractReturn(final Object result) {
            assert result != null;
            returnValue = result;
        }

        public final Object getReturnValue() {
            return returnValue;
        }
    }

    public static final class FreshLocalReturn extends AbstractReturn {
        @CompilationFinal private static final long serialVersionUID = 1L;

        public FreshLocalReturn(final Object returnValue) {
            super(returnValue);
        }

        public void raise() {
            throw new LocalReturn(returnValue);
        }

        @Override
        public String toString() {
            return "FreshLR (value: " + returnValue + ")";
        }
    }

    public static final class FreshNonLocalReturn extends AbstractReturn {
        @CompilationFinal private static final long serialVersionUID = 1L;
        @CompilationFinal private ContextObject targetContext;

        public FreshNonLocalReturn(final Object returnValue, final ContextObject targetContext) {
            super(returnValue);
            assert targetContext.getSender() != targetContext.image.nil;
            this.targetContext = targetContext;
        }

        public void raise() {
            throw new NonLocalReturn(returnValue, targetContext);
        }

        @Override
        public String toString() {
            return "FreshNLR (value: " + returnValue + ", target: " + targetContext + ")";
        }
    }

    public static final class LocalReturn extends AbstractReturn {
        @CompilationFinal private static final long serialVersionUID = 1L;

        public LocalReturn(final Object result) {
            super(result);
        }

        @Override
        public String toString() {
            return "LR (value: " + returnValue + ")";
        }
    }

    public static final class NonLocalReturn extends AbstractReturn {
        @CompilationFinal private static final long serialVersionUID = 1L;
        @CompilationFinal private ContextObject targetContext;
        @CompilationFinal private boolean arrivedAtTargetContext = false;

        public NonLocalReturn(final Object returnValue, final ContextObject targetContext) {
            super(returnValue);
            this.targetContext = targetContext;
        }

        public ContextObject getTargetContext() {
            return targetContext;
        }

        public boolean hasArrivedAtTargetContext() {
            return arrivedAtTargetContext;
        }

        public void setArrivedAtTargetContext() {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            arrivedAtTargetContext = true;
        }

        @Override
        public String toString() {
            return "NLR (value: " + returnValue + ", arrived: " + arrivedAtTargetContext + ", target: " + targetContext + ")";
        }
    }

    public static final class NonVirtualContextModification extends Exception {
        @CompilationFinal private static final long serialVersionUID = 1L;
    }

    public static final class NonVirtualReturn extends AbstractReturn {
        @CompilationFinal private static final long serialVersionUID = 1L;
        @CompilationFinal private final ContextObject targetContext;
        @CompilationFinal private final ContextObject currentContext;

        public NonVirtualReturn(final Object returnValue, final ContextObject targetContext, final ContextObject currentContext) {
            super(returnValue);
            assert !targetContext.hasVirtualSender();
            assert !currentContext.hasVirtualSender();
            this.targetContext = targetContext;
            this.currentContext = currentContext;
        }

        public ContextObject getTargetContext() {
            return targetContext;
        }

        public ContextObject getCurrentContext() {
            return currentContext;
        }

        @Override
        public String toString() {
            return "NVR (value: " + returnValue + ", current: " + currentContext + ", target: " + targetContext + ")";
        }
    }

    public static class TopLevelReturn extends AbstractReturn {
        @CompilationFinal private static final long serialVersionUID = 1L;

        public TopLevelReturn(final Object result) {
            super(result);
        }

        @Override
        public final String toString() {
            return "TLR (value: " + returnValue + ")";
        }
    }
}
