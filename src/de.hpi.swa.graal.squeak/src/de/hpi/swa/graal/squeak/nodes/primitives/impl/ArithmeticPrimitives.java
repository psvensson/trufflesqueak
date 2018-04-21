package de.hpi.swa.graal.squeak.nodes.primitives.impl;

import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;

public final class ArithmeticPrimitives extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return ArithmeticPrimitivesFactory.getFactories();
    }

    public abstract static class AbstractArithmeticPrimitiveNode extends AbstractPrimitiveNode {

        public AbstractArithmeticPrimitiveNode(final CompiledMethodObject method) {
            super(method);
        }

        @Override
        public final Object executeWithArguments(final VirtualFrame frame, final Object... arguments) {
            try {
                return executeWithArgumentsSpecialized(frame, arguments);
            } catch (ArithmeticException e) {
                throw new PrimitiveFailed();
            }
        }

        @Override
        public final Object executePrimitive(final VirtualFrame frame) {
            try {
                return executeArithmeticPrimitive(frame);
            } catch (ArithmeticException e) {
                throw new PrimitiveFailed();
            }
        }

        protected boolean isZero(final double value) {
            return value == 0;
        }

        protected boolean isIntegralWhenDividedBy(final long a, final long b) {
            return a % b == 0;
        }

        protected static final boolean isMinValueDividedByMinusOne(final long a, final long b) {
            return a == Long.MIN_VALUE && b == -1;
        }

        public abstract Object executeArithmeticPrimitive(VirtualFrame frame);

    }

    public abstract static class AbstractArithmeticBinaryPrimitiveNode extends AbstractPrimitiveNode {

        public AbstractArithmeticBinaryPrimitiveNode(final CompiledMethodObject method) {
            super(method);
        }

        @SuppressWarnings("unused")
        protected Object doLong(final long a, final long b) {
            throw new PrimitiveFailed(); // SmallInteger + LargeInteger
        }

        @SuppressWarnings("unused")
        protected Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            throw new PrimitiveFailed(); // LargeInteger
        }

        @SuppressWarnings("unused")
        protected Object doDouble(final double a, final double b) {
            throw new PrimitiveFailed(); // SmallFloat64
        }

        @SuppressWarnings("unused")
        protected Object doFloat(final FloatObject a, final FloatObject b) {
            throw new PrimitiveFailed(); // BoxedFloat64
        }

        @Specialization
        protected final Object doLongLargeInteger(final long a, final LargeIntegerObject b) {
            return doLargeInteger(asLargeInteger(a), b);
        }

        @Specialization
        protected final Object doLongDouble(final long a, final double b) {
            return doDouble(a, b);
        }

        @Specialization
        protected final Object doLongFloat(final long a, final FloatObject b) {
            return doFloat(asFloatObject(a), b);
        }

        @Specialization
        protected final Object doLargeIntegerLong(final LargeIntegerObject a, final long b) {
            return doLargeInteger(a, asLargeInteger(b));
        }

        @Specialization
        protected final Object doDoubleLong(final double a, final long b) {
            return doDouble(a, b);
        }

        @Specialization
        protected final Object doDoubleFloat(final double a, final FloatObject b) {
            return doFloat(asFloatObject(a), b);
        }

        @Specialization
        protected final Object doFloatLong(final FloatObject a, final long b) {
            return doFloat(a, asFloatObject(b));
        }

        @Specialization
        protected final Object doFloatDouble(final FloatObject a, final double b) {
            return doFloat(a, asFloatObject(b));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {3, 23, 43}, numArguments = 2)
    protected abstract static class PrimLessThanNode extends AbstractArithmeticBinaryPrimitiveNode {
        protected PrimLessThanNode(final CompiledMethodObject method) {
            super(method);
        }

        @Override
        @Specialization
        protected final Object doLong(final long a, final long b) {
            return a < b;
        }

        @Override
        @Specialization
        protected final Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.compareTo(b) < 0;
        }

        @Override
        @Specialization
        protected final Object doDouble(final double a, final double b) {
            return a < b;
        }

        @Override
        @Specialization
        protected final Object doFloat(final FloatObject a, final FloatObject b) {
            return a.getValue() < b.getValue();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {4, 24, 44}, numArguments = 2)
    protected abstract static class PrimGreaterThanNode extends AbstractArithmeticBinaryPrimitiveNode {
        protected PrimGreaterThanNode(final CompiledMethodObject method) {
            super(method);
        }

        @Override
        @Specialization
        protected final Object doLong(final long a, final long b) {
            return a > b;
        }

        @Override
        @Specialization
        protected final Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.compareTo(b) > 0;
        }

        @Override
        @Specialization
        protected final Object doDouble(final double a, final double b) {
            return a > b;
        }

        @Override
        @Specialization
        protected final Object doFloat(final FloatObject a, final FloatObject b) {
            return a.getValue() > b.getValue();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {5, 25, 45}, numArguments = 2)
    protected abstract static class PrimLessOrEqualNode extends AbstractArithmeticBinaryPrimitiveNode {
        protected PrimLessOrEqualNode(final CompiledMethodObject method) {
            super(method);
        }

        @Override
        @Specialization
        protected final Object doLong(final long a, final long b) {
            return a <= b;
        }

        @Override
        @Specialization
        protected final Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.compareTo(b) <= 0;
        }

        @Override
        @Specialization
        protected final Object doDouble(final double a, final double b) {
            return a <= b;
        }

        @Override
        @Specialization
        protected final Object doFloat(final FloatObject a, final FloatObject b) {
            return a.getValue() <= b.getValue();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {6, 26, 46}, numArguments = 2)
    protected abstract static class PrimGreaterOrEqualNode extends AbstractArithmeticBinaryPrimitiveNode {
        protected PrimGreaterOrEqualNode(final CompiledMethodObject method) {
            super(method);
        }

        @Override
        @Specialization
        protected final Object doLong(final long a, final long b) {
            return a >= b;
        }

        @Override
        @Specialization
        protected final Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.compareTo(b) >= 0;
        }

        @Override
        @Specialization
        protected final Object doDouble(final double a, final double b) {
            return a >= b;
        }

        @Override
        @Specialization
        protected final Object doFloat(final FloatObject a, final FloatObject b) {
            return a.getValue() >= b.getValue();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {7, 27, 47}, numArguments = 2)
    protected abstract static class PrimEqualNode extends AbstractArithmeticBinaryPrimitiveNode {
        protected PrimEqualNode(final CompiledMethodObject method) {
            super(method);
        }

        @Override
        @Specialization
        protected final Object doLong(final long a, final long b) {
            return a == b;
        }

        @Override
        @Specialization
        protected final Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.compareTo(b) == 0;
        }

        @Override
        @Specialization
        protected final Object doDouble(final double a, final double b) {
            return a == b;
        }

        @Override
        @Specialization
        protected final Object doFloat(final FloatObject a, final FloatObject b) {
            return a.getValue() == b.getValue();
        }

        @SuppressWarnings("unused")
        @Specialization // Specialization for quick nil checks.
        protected final boolean doNil(final Object a, final NilObject b) {
            return code.image.sqFalse;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {8, 28, 48}, numArguments = 2)
    protected abstract static class PrimNotEqualNode extends AbstractArithmeticBinaryPrimitiveNode {
        protected PrimNotEqualNode(final CompiledMethodObject method) {
            super(method);
        }

        @Override
        @Specialization
        protected final Object doLong(final long a, final long b) {
            return a != b;
        }

        @Override
        @Specialization
        protected final Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.compareTo(b) != 0;
        }

        @Override
        @Specialization
        protected final Object doDouble(final double a, final double b) {
            return a != b;
        }

        @Override
        @Specialization
        protected final Object doFloat(final FloatObject a, final FloatObject b) {
            return a.getValue() != b.getValue();
        }

        @SuppressWarnings("unused")
        @Specialization // Specialization for quick nil checks.
        protected final boolean doNil(final Object a, final NilObject b) {
            return code.image.sqTrue;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 10, numArguments = 2)
    protected abstract static class PrimDivideSmallIntegerNode extends AbstractArithmeticPrimitiveNode {
        protected PrimDivideSmallIntegerNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {
                        "isSmallInteger(a)", "isSmallInteger(b)", // both values need to be
                                                                  // SmallInteger
                        "b != 0",                                 // fail on division by zero
                        "isIntegralWhenDividedBy(a, b)"})         // fail if result is not integral
        public static final long doLong(final long a, final long b) {
            return a / b;
        }

        @SuppressWarnings("unused")
        @Fallback
        public static final long doZeroDivide(final Object a, final Object b) {
            throw new PrimitiveFailed();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 11, numArguments = 2)
    protected abstract static class PrimFloorModSmallIntegerNode extends AbstractArithmeticPrimitiveNode {
        protected PrimFloorModSmallIntegerNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"isSmallInteger(a)", "isSmallInteger(a)"})
        protected long doLong(final long a, final long b) {
            return Math.floorMod(a, b);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 12, numArguments = 2)
    protected abstract static class PrimFloorDivideSmallIntegerNode extends AbstractArithmeticPrimitiveNode {
        protected PrimFloorDivideSmallIntegerNode(final CompiledMethodObject method) {
            super(method);
        }

        /*
         * The primitive normally fails if argument is not a SmallInteger. Supporting LargeIntegers
         * anyway as it does not change the behavior.
         */

        @Specialization(guards = {"isSmallInteger(a)"})
        protected static final long doLong(final long a, final long b) {
            return Math.floorDiv(a, b);
        }

        @Specialization(guards = {"isSmallInteger(a)"})
        protected final long doLongLargeInteger(final long a, final LargeIntegerObject b) {
            return (long) asLargeInteger(a).floorDivide(b); // if a is SmallInteger, result must be
                                                            // SmallInteger
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 13, numArguments = 2)
    protected abstract static class PrimQuoSmallIntegerNode extends AbstractArithmeticPrimitiveNode {
        protected PrimQuoSmallIntegerNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"b != 0"})
        protected static final long doLong(final long a, final long b) {
            return a / b;
        }

        @SuppressWarnings("unused")
        @Fallback
        public static final long doZeroDivide(final Object a, final Object b) {
            throw new PrimitiveFailed();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 20, numArguments = 2)
    protected abstract static class PrimRemLargeIntegersNode extends AbstractArithmeticPrimitiveNode {
        protected PrimRemLargeIntegersNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"b != 0"})
        protected static final long doLong(final long a, final long b) {
            return a / b;
        }

        @Specialization(guards = {"!b.isZero()"})
        protected static final Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.remainder(b);
        }

        @Specialization(guards = {"!b.isZero()"})
        protected final Object doLong(final long a, final LargeIntegerObject b) {
            return doLargeInteger(asLargeInteger(a), b);
        }

        @Specialization(guards = {"b != 0"})
        protected final Object doLargeInteger(final LargeIntegerObject a, final long b) {
            return doLargeInteger(a, asLargeInteger(b));
        }

        @SuppressWarnings("unused")
        @Fallback
        public static final long doZeroDivide(final Object a, final Object b) {
            throw new PrimitiveFailed();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 30, numArguments = 2)
    protected abstract static class PrimDivideLargeIntegerNode extends AbstractArithmeticPrimitiveNode {
        protected PrimDivideLargeIntegerNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {
                        "b != 0",                                   // fail on division by zero
                        "isIntegralWhenDividedBy(a, b)",            // fail if result is not
                                                                    // integral
                        "!isMinValueDividedByMinusOne(a, b)"})      // handle special case
                                                                    // separately
        public static final long doLong(final long a, final long b) {
            return a / b;
        }

        @Specialization(guards = "isMinValueDividedByMinusOne(a, b)") // handle special case:
                                                                      // Long.MIN_VALUE / -1
        protected final Object doLongOverflow(final long a, final long b) {
            return doLargeInteger(asLargeInteger(a), asLargeInteger(b));
        }

        @Specialization(guards = {"!b.isZero()", "a.isIntegralWhenDividedBy(b)"})
        protected static final Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.divide(b);
        }

        @Specialization(guards = {"b != 0", "a.isIntegralWhenDividedBy(asLargeInteger(b))"})
        protected final Object doLargeIntegerLong(final LargeIntegerObject a, final long b) {
            return doLargeInteger(a, asLargeInteger(b));
        }

        @Specialization(guards = {"!b.isZero()", "asLargeInteger(a).isIntegralWhenDividedBy(b)"})
        protected final Object doLongLargeInteger(final long a, final LargeIntegerObject b) {
            return doLargeInteger(asLargeInteger(a), b);
        }

        @SuppressWarnings("unused")
        @Fallback
        public static final long doZeroDivide(final Object a, final Object b) {
            throw new PrimitiveFailed();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 31, numArguments = 2)
    protected abstract static class PrimFloorModLargeIntegerNode extends AbstractArithmeticPrimitiveNode {
        protected PrimFloorModLargeIntegerNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"!isSmallInteger(a)"})
        protected long doLong(final long a, final long b) {
            return Math.floorMod(a, b);
        }

        @Specialization
        protected Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.floorMod(b);
        }

        @Specialization
        protected Object doLong(final long a, final LargeIntegerObject b) {
            return doLargeInteger(asLargeInteger(a), b);
        }

        @Specialization
        protected Object doLargeInteger(final LargeIntegerObject a, final long b) {
            return doLargeInteger(a, asLargeInteger(b));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 32, numArguments = 2)
    protected abstract static class PrimFloorDivideLargeIntegerNode extends AbstractArithmeticPrimitiveNode {
        protected PrimFloorDivideLargeIntegerNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final long doLong(final long a, final long b) {
            return Math.floorDiv(a, b);
        }

        @Specialization
        protected static final Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.floorDivide(b);
        }

        @Specialization
        protected final Object doLong(final long a, final LargeIntegerObject b) {
            return doLargeInteger(asLargeInteger(a), b);
        }

        @Specialization
        protected final Object doLargeInteger(final LargeIntegerObject a, final long b) {
            return doLargeInteger(a, asLargeInteger(b));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 33, numArguments = 2)
    protected abstract static class PrimQuoLargeIntegerNode extends AbstractArithmeticPrimitiveNode {
        protected PrimQuoLargeIntegerNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {
                        "b != 0",                                 // fail on division by zero
                        "isMinValueDividedByMinusOne(a, b)"})     // handle special case separately
        public static final long doLong(final long a, final long b) {
            return a / b;
        }

        @Specialization(guards = "isMinValueDividedByMinusOne(a, b)") // handle special case:
                                                                      // Long.MIN_VALUE / -1
        protected final Object doLongOverflow(final long a, final long b) {
            return doLargeInteger(asLargeInteger(a), asLargeInteger(b));
        }

        @Specialization(guards = "!b.isZero()")
        protected static final Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.divide(b);
        }

        @Specialization(guards = "b != 0")
        protected final Object doLargeInteger(final LargeIntegerObject a, final long b) {
            return doLargeInteger(a, asLargeInteger(b));
        }

        @SuppressWarnings("unused")
        @Fallback
        public static final long doZeroDivide(final Object a, final Object b) {
            throw new PrimitiveFailed();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 38, numArguments = 2)
    protected abstract static class PrimFloatAtNode extends AbstractArithmeticPrimitiveNode {
        protected PrimFloatAtNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final Object doDouble(final double receiver, final long index) {
            return doFloatObject(new FloatObject(code.image, receiver), index);
        }

        @Specialization
        protected static final Object doFloatObject(final FloatObject receiver, final long index) {
            return receiver.at0(index - 1);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 39, numArguments = 3)
    protected abstract static class PrimFloatAtPutNode extends AbstractArithmeticPrimitiveNode {
        protected PrimFloatAtPutNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final double doDouble(final double receiver, final long index, final long value) {
            return doFloatObject(new FloatObject(code.image, receiver), index, value);
        }

        @Specialization
        protected static final long doFloatObject(final FloatObject receiver, final long index, final long value) {
            receiver.atput0(index - 1, value);
            return value;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 40, numArguments = 2)
    protected abstract static class PrimAsFloatNode extends AbstractArithmeticPrimitiveNode {
        protected PrimAsFloatNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final FloatObject doLong(final long receiver) {
            return asFloatObject(receiver);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 50, numArguments = 2)
    protected abstract static class PrimFloatDivideNode extends AbstractArithmeticPrimitiveNode {
        protected PrimFloatDivideNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "!isZero(b)")
        protected static final double doDouble(final double a, final double b) {
            return a / b;
        }

        @Specialization(guards = {"b != 0", "isSmallInteger(b)"})
        protected static final double doDoubleLong(final double a, final long b) {
            return doDouble(a, b);
        }

        @Specialization(guards = {"!isZero(b.getValue())"})
        protected final FloatObject doDoubleFloat(final double a, final FloatObject b) {
            return asFloatObject(doDouble(a, b.getValue()));
        }

        @Specialization(guards = "!isZero(b.getValue())")
        protected final FloatObject doFloat(final FloatObject a, final FloatObject b) {
            return asFloatObject(a.getValue() / b.getValue());
        }

        @Specialization(guards = {"!isZero(b)"})
        protected final FloatObject doFloatLong(final FloatObject a, final double b) {
            return asFloatObject(doDouble(a.getValue(), b));
        }

        @SuppressWarnings("unused")
        @Fallback
        public static final long doZeroDivide(final Object a, final Object b) {
            throw new PrimitiveFailed();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 51)
    protected abstract static class PrimFloatTruncatedNode extends AbstractArithmeticPrimitiveNode {
        protected PrimFloatTruncatedNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final long doDouble(final double receiver) {
            final long truncatedValue = Double.valueOf(receiver).longValue();
            if (isSmallInteger(truncatedValue)) {
                return truncatedValue;
            } else {
                throw new PrimitiveFailed();
            }
        }

        @Specialization
        protected static final long doFloatObject(final FloatObject receiver) {
            return doDouble(receiver.getValue());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 53)
    protected abstract static class PrimFloatExponentNode extends AbstractArithmeticPrimitiveNode {
        protected PrimFloatExponentNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final long doLong(final long receiver) {
            return Math.getExponent(receiver);
        }

        @Specialization
        protected static final long doDouble(final double receiver) {
            return Math.getExponent(receiver);
        }

        @Specialization
        protected static final long doFloat(final FloatObject receiver) {
            return Math.getExponent(receiver.getValue());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 54, numArguments = 2)
    protected abstract static class PrimFloatTimesTwoPowerNode extends AbstractArithmeticPrimitiveNode {
        @CompilationFinal private static final int UNDERFLOW_LIMIT = FloatObject.EMIN - FloatObject.PRECISION + 1;

        protected PrimFloatTimesTwoPowerNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final double doLong(final long receiver, final long argument) {
            return receiver * Math.pow(2, argument);
        }

        @Specialization
        protected static final double doLongDouble(final long receiver, final double argument) {
            return receiver * Math.pow(2, argument);
        }

        @Specialization
        protected final FloatObject doLongFloat(final long receiver, final FloatObject argument) {
            return asFloatObject(doDouble(receiver, argument.getValue()));
        }

        @Specialization
        protected static final double doDouble(final double receiver, final double argument) {
            // see Float>>timesTwoPower:
            if (receiver == 0.0 || Double.isInfinite(receiver)) {
                return receiver;
            } else if (argument > FloatObject.EMAX) {
                return receiver * Math.pow(2.0, FloatObject.EMAX) * Math.pow(2, argument - FloatObject.EMAX);
            } else if (argument < UNDERFLOW_LIMIT) {
                int deltaToUnderflow = Math.max(FloatObject.EMIN - Math.getExponent(argument), UNDERFLOW_LIMIT);
                if (deltaToUnderflow >= 0) {
                    deltaToUnderflow = FloatObject.EMIN;
                }
                return receiver * Math.pow(2.0, deltaToUnderflow) * Math.pow(2, argument - deltaToUnderflow);
            } else {
                return receiver * Math.pow(2.0, argument);
            }
        }

        @Specialization
        protected static final double doDoubleLong(final double receiver, final long argument) {
            return doDouble(receiver, argument);
        }

        @Specialization
        protected final FloatObject doDoubleFloat(final double receiver, final FloatObject argument) {
            return asFloatObject(doDouble(receiver, argument.getValue()));
        }

        @Specialization
        protected final FloatObject doFloat(final FloatObject receiver, final FloatObject argument) {
            return asFloatObject(doDouble(receiver.getValue(), argument.getValue()));
        }

        @Specialization
        protected final FloatObject doFloatLong(final FloatObject receiver, final long argument) {
            return asFloatObject(doDouble(receiver.getValue(), argument));
        }

        @Specialization
        protected final FloatObject doFloatDouble(final FloatObject receiver, final double argument) {
            return asFloatObject(doDouble(receiver.getValue(), argument));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 55)
    protected abstract static class PrimSquareRootNode extends AbstractArithmeticPrimitiveNode {
        protected PrimSquareRootNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final double doLong(final long receiver) {
            return Math.sqrt(receiver);
        }

        @Specialization
        protected static final double doLargeInteger(final LargeIntegerObject receiver) {
            return Math.sqrt(receiver.doubleValue());
        }

        @Specialization
        protected static final double doDouble(final double receiver) {
            return Math.sqrt(receiver);
        }

        @Specialization
        protected final FloatObject doFloat(final FloatObject a) {
            return asFloatObject(Math.sqrt(a.getValue()));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 56)
    protected abstract static class PrimSinNode extends AbstractArithmeticPrimitiveNode {
        protected PrimSinNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final double doDouble(final double rcvr) {
            return Math.sin(rcvr);
        }

        @Specialization
        protected final FloatObject doFloat(final FloatObject a) {
            return asFloatObject(Math.sin(a.getValue()));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 57)
    protected abstract static class PrimArcTanNode extends AbstractArithmeticPrimitiveNode {
        protected PrimArcTanNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final double doDouble(final double a) {
            return Math.atan(a);
        }

        @Specialization
        protected final FloatObject doFloat(final FloatObject a) {
            return asFloatObject(Math.atan(a.getValue()));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 58)
    protected abstract static class PrimLogNNode extends AbstractArithmeticPrimitiveNode {
        protected PrimLogNNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final double doDouble(final double a) {
            return Math.log(a);
        }

        @Specialization
        protected final FloatObject doFloat(final FloatObject a) {
            return asFloatObject(Math.log(a.getValue()));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 59)
    protected abstract static class PrimExpNode extends AbstractArithmeticPrimitiveNode {
        protected PrimExpNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final double doDouble(final double rcvr) {
            return Math.exp(rcvr);
        }

        @Specialization
        protected final FloatObject doFloat(final FloatObject a) {
            return asFloatObject(Math.exp(a.getValue()));
        }
    }
}
