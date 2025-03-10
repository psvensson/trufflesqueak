/*
 * Copyright (c) 2017-2022 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2022 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.util;

import java.lang.reflect.Field;

import com.oracle.truffle.api.CompilerDirectives;

import de.hpi.swa.trufflesqueak.model.AbstractPointersObject;
import sun.misc.Unsafe;

public final class UnsafeUtils {

    private static final Unsafe UNSAFE = initUnsafe();

    private UnsafeUtils() {
    }

    public static void copyBytes(final byte[] src, final long srcPos, final byte[] dest, final long destPos, final long length) {
        assert 0 <= srcPos && srcPos + length <= src.length && 0 <= destPos && destPos + length <= dest.length;
        UNSAFE.copyMemory(src, Unsafe.ARRAY_BYTE_BASE_OFFSET + srcPos * Unsafe.ARRAY_BYTE_INDEX_SCALE,
                        dest, Unsafe.ARRAY_BYTE_BASE_OFFSET + destPos * Unsafe.ARRAY_BYTE_INDEX_SCALE, Byte.BYTES * length);
    }

    public static void copyChars(final char[] src, final long srcPos, final char[] dest, final long destPos, final long length) {
        assert 0 <= srcPos && srcPos + length <= src.length && 0 <= destPos && destPos + length <= dest.length;
        UNSAFE.copyMemory(src, Unsafe.ARRAY_CHAR_BASE_OFFSET + srcPos * Unsafe.ARRAY_CHAR_INDEX_SCALE,
                        dest, Unsafe.ARRAY_CHAR_BASE_OFFSET + destPos * Unsafe.ARRAY_CHAR_INDEX_SCALE, Character.BYTES * length);
    }

    public static void copyDoubles(final double[] src, final long srcPos, final double[] dest, final long destPos, final long length) {
        assert 0 <= srcPos && srcPos + length <= src.length && 0 <= destPos && destPos + length <= dest.length;
        UNSAFE.copyMemory(src, Unsafe.ARRAY_DOUBLE_BASE_OFFSET + srcPos * Unsafe.ARRAY_DOUBLE_INDEX_SCALE,
                        dest, Unsafe.ARRAY_DOUBLE_BASE_OFFSET + destPos * Unsafe.ARRAY_DOUBLE_INDEX_SCALE, Double.BYTES * length);
    }

    public static void copyInts(final int[] src, final long srcPos, final int[] dest, final long destPos, final long length) {
        assert 0 <= srcPos && srcPos + length <= src.length && 0 <= destPos && destPos + length <= dest.length;
        UNSAFE.copyMemory(src, Unsafe.ARRAY_INT_BASE_OFFSET + srcPos * Unsafe.ARRAY_INT_INDEX_SCALE,
                        dest, Unsafe.ARRAY_INT_BASE_OFFSET + destPos * Unsafe.ARRAY_INT_INDEX_SCALE, Integer.BYTES * length);
    }

    public static void copyLongs(final long[] src, final long srcPos, final long[] dest, final long destPos, final long length) {
        assert 0 <= srcPos && srcPos + length <= src.length && 0 <= destPos && destPos + length <= dest.length;
        UNSAFE.copyMemory(src, Unsafe.ARRAY_LONG_BASE_OFFSET + srcPos * Unsafe.ARRAY_LONG_INDEX_SCALE,
                        dest, Unsafe.ARRAY_LONG_BASE_OFFSET + destPos * Unsafe.ARRAY_LONG_INDEX_SCALE, Long.BYTES * length);
    }

    public static void copyShorts(final short[] src, final long srcPos, final short[] dest, final long destPos, final long length) {
        assert 0 <= srcPos && srcPos + length <= src.length && 0 <= destPos && destPos + length <= dest.length;
        UNSAFE.copyMemory(src, Unsafe.ARRAY_SHORT_BASE_OFFSET + srcPos * Unsafe.ARRAY_SHORT_INDEX_SCALE,
                        dest, Unsafe.ARRAY_SHORT_BASE_OFFSET + destPos * Unsafe.ARRAY_SHORT_INDEX_SCALE, Short.BYTES * length);
    }

    public static long getAddress(final Class<?> javaClass, final String fieldName) {
        try {
            return UNSAFE.objectFieldOffset(javaClass.getField(fieldName));
        } catch (NoSuchFieldException | SecurityException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static boolean getBoolAt(final AbstractPointersObject object, final long address) {
        return UNSAFE.getBoolean(object, address);
    }

    public static boolean getBoolFromLongs(final long[] array, final long index) {
        assert 0 <= index && index < array.length;
        return UNSAFE.getBoolean(array, Unsafe.ARRAY_LONG_BASE_OFFSET + index * Unsafe.ARRAY_LONG_INDEX_SCALE);
    }

    public static byte getByte(final byte[] storage, final long index) {
        assert 0 <= index && index < storage.length;
        return UNSAFE.getByte(storage, Unsafe.ARRAY_BYTE_BASE_OFFSET + index * Unsafe.ARRAY_BYTE_INDEX_SCALE);
    }

    public static char getChar(final char[] storage, final long index) {
        assert 0 <= index && index < storage.length;
        return UNSAFE.getChar(storage, Unsafe.ARRAY_CHAR_BASE_OFFSET + index * Unsafe.ARRAY_CHAR_INDEX_SCALE);
    }

    public static char getCharAt(final AbstractPointersObject object, final long address) {
        return UNSAFE.getChar(object, address);
    }

    public static char getCharFromLongs(final long[] array, final long index) {
        assert 0 <= index && index < array.length;
        return UNSAFE.getChar(array, Unsafe.ARRAY_LONG_BASE_OFFSET + index * Unsafe.ARRAY_LONG_INDEX_SCALE);
    }

    public static double getDouble(final double[] storage, final long index) {
        assert 0 <= index && index < storage.length;
        return UNSAFE.getDouble(storage, Unsafe.ARRAY_DOUBLE_BASE_OFFSET + index * Unsafe.ARRAY_DOUBLE_INDEX_SCALE);
    }

    public static double getDoubleAt(final AbstractPointersObject object, final long address) {
        return UNSAFE.getDouble(object, address);
    }

    public static double getDoubleFromBytes(final byte[] bytes, final long index) {
        assert 0 <= index && index <= bytes.length;
        return UNSAFE.getDouble(bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET + index * Unsafe.ARRAY_BYTE_INDEX_SCALE);
    }

    public static double getDoubleFromLongs(final long[] array, final long index) {
        assert 0 <= index && index < array.length;
        return UNSAFE.getDouble(array, Unsafe.ARRAY_LONG_BASE_OFFSET + index * Unsafe.ARRAY_LONG_INDEX_SCALE);
    }

    public static float getFloatFromBytes(final byte[] bytes, final long index) {
        assert 0 <= index && index <= bytes.length;
        return UNSAFE.getFloat(bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET + index * Unsafe.ARRAY_BYTE_INDEX_SCALE);
    }

    public static int getInt(final byte[] bytes, final long index) {
        assert 0 <= index && index * Integer.BYTES < bytes.length;
        return UNSAFE.getInt(bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET + index * Integer.BYTES * Unsafe.ARRAY_BYTE_INDEX_SCALE);
    }

    public static int getInt(final int[] storage, final long index) {
        assert 0 <= index && index < storage.length;
        return UNSAFE.getInt(storage, Unsafe.ARRAY_INT_BASE_OFFSET + index * Unsafe.ARRAY_INT_INDEX_SCALE);
    }

    public static int getIntAt(final Object object, final long address) {
        return UNSAFE.getInt(object, address);
    }

    public static int getIntFromBytes(final byte[] bytes, final long index) {
        assert 0 <= index && index <= bytes.length;
        return UNSAFE.getInt(bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET + index * Unsafe.ARRAY_BYTE_INDEX_SCALE);
    }

    public static int getIntReversed(final byte[] bytes, final long index) {
        return Integer.reverseBytes(getInt(bytes, index));
    }

    public static long getLong(final byte[] bytes, final long index) {
        assert 0 <= index && index * Long.BYTES < bytes.length;
        return UNSAFE.getLong(bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET + index * Long.BYTES * Unsafe.ARRAY_BYTE_INDEX_SCALE);
    }

    public static long getLong(final long[] storage, final long index) {
        assert 0 <= index && index < storage.length;
        return UNSAFE.getLong(storage, Unsafe.ARRAY_LONG_BASE_OFFSET + index * Unsafe.ARRAY_LONG_INDEX_SCALE);
    }

    public static long getLongAt(final AbstractPointersObject object, final long address) {
        return UNSAFE.getLong(object, address);
    }

    public static long getLongAtByteIndex(final byte[] bytes, final long index) {
        assert 0 <= index && index < bytes.length - Long.BYTES;
        return UNSAFE.getLong(bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET + index * Unsafe.ARRAY_BYTE_INDEX_SCALE);
    }

    public static Object getObject(final Object[] storage, final long index) {
        assert 0 <= index && index < storage.length;
        return UNSAFE.getObject(storage, Unsafe.ARRAY_OBJECT_BASE_OFFSET + index * Unsafe.ARRAY_OBJECT_INDEX_SCALE);
    }

    public static Object getObjectAt(final AbstractPointersObject object, final long address) {
        return UNSAFE.getObject(object, address);
    }

    public static short getShort(final byte[] bytes, final long index) {
        assert 0 <= index && index * Short.BYTES < bytes.length;
        return UNSAFE.getShort(bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET + index * Short.BYTES * Unsafe.ARRAY_BYTE_INDEX_SCALE);
    }

    public static short getShort(final int[] ints, final long index) {
        assert 0 <= index && index * Short.BYTES / Integer.BYTES < ints.length;
        return UNSAFE.getShort(ints, Unsafe.ARRAY_INT_BASE_OFFSET + index * Unsafe.ARRAY_SHORT_INDEX_SCALE);
    }

    public static short getShort(final short[] storage, final long index) {
        assert 0 <= index && index < storage.length;
        return UNSAFE.getShort(storage, Unsafe.ARRAY_SHORT_BASE_OFFSET + index * Unsafe.ARRAY_SHORT_INDEX_SCALE);
    }

    public static short getShortFromBytes(final byte[] bytes, final long index) {
        assert 0 <= index && index <= bytes.length;
        return UNSAFE.getShort(bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET + index * Unsafe.ARRAY_BYTE_INDEX_SCALE);
    }

    private static Unsafe initUnsafe() {
        try {
            // Fast path when we are trusted.
            return Unsafe.getUnsafe();
        } catch (final SecurityException se) {
            // Slow path when we are not trusted.
            try {
                final Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
                theUnsafe.setAccessible(true);
                return (Unsafe) theUnsafe.get(Unsafe.class);
            } catch (final Exception e) {
                throw new RuntimeException("exception while trying to get Unsafe", e);
            }
        }
    }

    public static void putBoolAt(final AbstractPointersObject object, final long address, final boolean value) {
        UNSAFE.putBoolean(object, address, value);
    }

    public static void putBoolIntoLongs(final long[] array, final long index, final boolean value) {
        assert 0 <= index && index < array.length;
        UNSAFE.putBoolean(array, Unsafe.ARRAY_LONG_BASE_OFFSET + index * Unsafe.ARRAY_LONG_INDEX_SCALE, value);
    }

    public static void putByte(final byte[] storage, final long index, final byte value) {
        assert 0 <= index && index < storage.length;
        UNSAFE.putByte(storage, Unsafe.ARRAY_BYTE_BASE_OFFSET + index * Unsafe.ARRAY_BYTE_INDEX_SCALE, value);
    }

    public static void putChar(final char[] storage, final long index, final char value) {
        assert 0 <= index && index < storage.length;
        UNSAFE.putChar(storage, Unsafe.ARRAY_CHAR_BASE_OFFSET + index * Unsafe.ARRAY_CHAR_INDEX_SCALE, value);
    }

    public static void putCharAt(final AbstractPointersObject object, final long address, final char value) {
        UNSAFE.putChar(object, address, value);
    }

    public static void putCharIntoLongs(final long[] array, final long index, final char value) {
        assert 0 <= index && index < array.length;
        UNSAFE.putChar(array, Unsafe.ARRAY_LONG_BASE_OFFSET + index * Unsafe.ARRAY_LONG_INDEX_SCALE, value);
    }

    public static void putDouble(final double[] storage, final long index, final double value) {
        assert 0 <= index && index < storage.length;
        UNSAFE.putDouble(storage, Unsafe.ARRAY_DOUBLE_BASE_OFFSET + index * Unsafe.ARRAY_DOUBLE_INDEX_SCALE, value);
    }

    public static void putDoubleAt(final AbstractPointersObject object, final long address, final double value) {
        UNSAFE.putDouble(object, address, value);
    }

    public static void putDoubleIntoBytes(final byte[] bytes, final long index, final double value) {
        assert 0 <= index && index <= bytes.length;
        UNSAFE.putDouble(bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET + index * Unsafe.ARRAY_BYTE_INDEX_SCALE, value);
    }

    public static void putDoubleIntoLongs(final long[] array, final long index, final double value) {
        UNSAFE.putDouble(array, Unsafe.ARRAY_LONG_BASE_OFFSET + index * Unsafe.ARRAY_LONG_INDEX_SCALE, value);
    }

    public static void putFloatIntoBytes(final byte[] bytes, final long index, final float value) {
        assert 0 <= index && index <= bytes.length;
        UNSAFE.putFloat(bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET + index * Unsafe.ARRAY_BYTE_INDEX_SCALE, value);
    }

    public static void putInt(final byte[] bytes, final long index, final int value) {
        assert 0 <= index && index * Short.BYTES < bytes.length;
        UNSAFE.putInt(bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET + index * Integer.BYTES * Unsafe.ARRAY_BYTE_INDEX_SCALE, value);
    }

    public static void putInt(final int[] storage, final long index, final int value) {
        assert 0 <= index && index < storage.length;
        UNSAFE.putInt(storage, Unsafe.ARRAY_INT_BASE_OFFSET + index * Unsafe.ARRAY_INT_INDEX_SCALE, value);
    }

    public static void putIntAt(final Object object, final long offset, final int value) {
        UNSAFE.putInt(object, offset, value);
    }

    public static void putIntIntoBytes(final byte[] bytes, final long index, final int value) {
        assert 0 <= index && index <= bytes.length;
        UNSAFE.putInt(bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET + index * Unsafe.ARRAY_BYTE_INDEX_SCALE, value);
    }

    public static void putIntReversed(final byte[] bytes, final long index, final int value) {
        putInt(bytes, index, Integer.reverseBytes(value));
    }

    public static void putLong(final byte[] bytes, final long index, final long value) {
        assert 0 <= index && index * Integer.BYTES < bytes.length;
        UNSAFE.putLong(bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET + index * Long.BYTES * Unsafe.ARRAY_BYTE_INDEX_SCALE, value);
    }

    public static void putLong(final long[] storage, final long index, final long value) {
        assert 0 <= index && index < storage.length;
        UNSAFE.putLong(storage, Unsafe.ARRAY_LONG_BASE_OFFSET + index * Unsafe.ARRAY_LONG_INDEX_SCALE, value);
    }

    public static void putLongAt(final AbstractPointersObject object, final long address, final long value) {
        UNSAFE.putLong(object, address, value);
    }

    public static void putLongIntoBytes(final byte[] bytes, final long index, final long value) {
        assert 0 <= index && index <= bytes.length;
        UNSAFE.putLong(bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET + index * Unsafe.ARRAY_BYTE_INDEX_SCALE, value);
    }

    public static void putObject(final Object[] storage, final long index, final Object value) {
        assert 0 <= index && index < storage.length;
        UNSAFE.putObject(storage, Unsafe.ARRAY_OBJECT_BASE_OFFSET + index * Unsafe.ARRAY_OBJECT_INDEX_SCALE, value);
    }

    public static void putObjectAt(final AbstractPointersObject object, final long address, final Object value) {
        UNSAFE.putObject(object, address, value);
    }

    public static void putShort(final byte[] bytes, final long index, final short value) {
        assert 0 <= index && index * Short.BYTES < bytes.length;
        UNSAFE.putShort(bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET + index * Short.BYTES * Unsafe.ARRAY_BYTE_INDEX_SCALE, value);
    }

    public static void putShort(final int[] ints, final long index, final short value) {
        assert 0 <= index && index / Short.BYTES < ints.length;
        UNSAFE.putShort(ints, Unsafe.ARRAY_INT_BASE_OFFSET + index * Unsafe.ARRAY_SHORT_INDEX_SCALE, value);
    }

    public static void putShort(final short[] storage, final long index, final short value) {
        assert 0 <= index && index < storage.length;
        UNSAFE.putShort(storage, Unsafe.ARRAY_SHORT_BASE_OFFSET + index * Unsafe.ARRAY_SHORT_INDEX_SCALE, value);
    }

    public static RuntimeException throwException(final Throwable e) {
        UNSAFE.throwException(e);
        return CompilerDirectives.shouldNotReachHere();
    }

    public static void putShortIntoBytes(final byte[] bytes, final long index, final short value) {
        assert 0 <= index && index <= bytes.length;
        UNSAFE.putShort(bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET + index * Unsafe.ARRAY_BYTE_INDEX_SCALE, value);
    }

    public static byte[] toBytes(final int[] ints) {
        final int numBytes = ints.length * Integer.BYTES;
        final byte[] bytes = new byte[numBytes];
        UNSAFE.copyMemory(ints, Unsafe.ARRAY_INT_BASE_OFFSET, bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET, numBytes);
        return bytes;
    }

    public static byte[] toBytes(final long[] longs) {
        final int numBytes = longs.length * Long.BYTES;
        final byte[] bytes = new byte[numBytes];
        UNSAFE.copyMemory(longs, Unsafe.ARRAY_LONG_BASE_OFFSET, bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET, numBytes);
        return bytes;
    }

    public static byte[] toBytes(final short[] shorts) {
        final int numBytes = shorts.length * Short.BYTES;
        final byte[] bytes = new byte[numBytes];
        UNSAFE.copyMemory(shorts, Unsafe.ARRAY_SHORT_BASE_OFFSET, bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET, numBytes);
        return bytes;
    }

    public static int[] toInts(final byte[] bytes) {
        final int numBytes = bytes.length;
        assert numBytes % Integer.BYTES == 0;
        final int[] ints = new int[numBytes / Integer.BYTES];
        UNSAFE.copyMemory(bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET, ints, Unsafe.ARRAY_INT_BASE_OFFSET, numBytes);
        return ints;
    }

    public static int[] toInts(final long[] longs) {
        final int numBytes = longs.length * Long.BYTES;
        final int[] ints = new int[numBytes / Integer.BYTES];
        UNSAFE.copyMemory(longs, Unsafe.ARRAY_LONG_BASE_OFFSET, ints, Unsafe.ARRAY_INT_BASE_OFFSET, numBytes);
        return ints;
    }

    public static int[] toInts(final short[] shorts) {
        final int numBytes = shorts.length * Short.BYTES;
        assert numBytes % Integer.BYTES == 0;
        final int[] ints = new int[numBytes / Integer.BYTES];
        UNSAFE.copyMemory(shorts, Unsafe.ARRAY_SHORT_BASE_OFFSET, ints, Unsafe.ARRAY_INT_BASE_OFFSET, numBytes);
        return ints;
    }

    public static int[] toIntsExact(final byte[] bytes) {
        final int numBytes = bytes.length;
        final int[] ints = new int[Math.floorDiv(numBytes + 3, Integer.BYTES)];
        UNSAFE.copyMemory(bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET, ints, Unsafe.ARRAY_INT_BASE_OFFSET, numBytes);
        return ints;
    }

    public static long[] toLongs(final byte[] bytes) {
        final int numBytes = bytes.length;
        assert numBytes % Long.BYTES == 0;
        final long[] longs = new long[numBytes / Long.BYTES];
        UNSAFE.copyMemory(bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET, longs, Unsafe.ARRAY_LONG_BASE_OFFSET, numBytes);
        return longs;
    }

    public static long[] toLongs(final int[] ints) {
        final int numBytes = ints.length * Integer.BYTES;
        assert numBytes % Long.BYTES == 0;
        final long[] longs = new long[numBytes / Long.BYTES];
        UNSAFE.copyMemory(ints, Unsafe.ARRAY_INT_BASE_OFFSET, longs, Unsafe.ARRAY_LONG_BASE_OFFSET, numBytes);
        return longs;
    }

    public static long[] toLongs(final short[] shorts) {
        final int numBytes = shorts.length * Short.BYTES;
        assert numBytes % Long.BYTES == 0;
        final long[] longs = new long[numBytes / Long.BYTES];
        UNSAFE.copyMemory(shorts, Unsafe.ARRAY_SHORT_BASE_OFFSET, longs, Unsafe.ARRAY_LONG_BASE_OFFSET, numBytes);
        return longs;
    }

    public static short[] toShorts(final byte[] bytes) {
        final int numBytes = bytes.length;
        assert numBytes % Short.BYTES == 0;
        final short[] shorts = new short[numBytes / Short.BYTES];
        UNSAFE.copyMemory(bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET, shorts, Unsafe.ARRAY_SHORT_BASE_OFFSET, numBytes);
        return shorts;
    }

    public static short[] toShorts(final int[] ints) {
        final int numBytes = ints.length * Integer.BYTES;
        final short[] shorts = new short[numBytes / Short.BYTES];
        UNSAFE.copyMemory(ints, Unsafe.ARRAY_INT_BASE_OFFSET, shorts, Unsafe.ARRAY_SHORT_BASE_OFFSET, numBytes);
        return shorts;
    }

    public static short[] toShorts(final long[] longs) {
        final int numBytes = longs.length * Long.BYTES;
        final short[] shorts = new short[numBytes / Short.BYTES];
        UNSAFE.copyMemory(longs, Unsafe.ARRAY_LONG_BASE_OFFSET, shorts, Unsafe.ARRAY_SHORT_BASE_OFFSET, numBytes);
        return shorts;
    }
}
