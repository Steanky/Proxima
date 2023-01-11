package com.github.steanky.proxima.snapper;

import com.github.steanky.proxima.Direction;
import org.jetbrains.annotations.NotNull;

public interface NodeSnapper {
    long FAIL = 0xFFC0_0001L; //lower 32 bits represent a floating-point qNaN with a payload of all 0s

    long snap(@NotNull Direction direction, int nodeX, int nodeY, int nodeZ, float nodeOffset);

    float checkInitial(double x, double y, double z, double dx, double dy, double dz);

    /**
     * Encodes a double-precision height value into a single long whose most significant 32 bits represent the integer
     * height of a successful node, and whose lower 32 bits represent a single-precision floating point value
     * corresponding to the offset value.
     *
     * @param height the height to encode
     * @return the encoded height
     */
    static long encode(double height, boolean intermediateJump) {
        int blockY = (int)Math.floor(height);
        float offset = (float) (height - blockY);

        //mask off sign bit, upcast to long
        long offsetBits = (Float.floatToRawIntBits(offset) & 0x7FFF_FFFF);

        //if the node requires an intermediate jump to get to
        long jumpBits = intermediateJump ? 0x8000_0000L : 0;

        //first 32 bits are block Y, last 32 encode offset information
        return (((long) blockY) << 32L) | jumpBits | offsetBits;
    }

    static long encode(double height) {
        return encode(height, false);
    }

    static int height(long value) {
        return (int) (value >>> 32);
    }

    static float offset(long value) {
        int valueInt = (int) (value & 0x7FFF_FFFFL);
        return Float.intBitsToFloat(valueInt);
    }

    static boolean intermediateJump(long value) {
        return (value & 0x8000_0000L) != 0;
    }
}
