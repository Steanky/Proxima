package com.github.steanky.proxima;

public interface DirectionalNodeSnapper {
    long FAIL = 0xFFC0_0001L; //lower 32 bits represent a floating-point NaN with a payload of all 0s

    long snap(int dx, int dz, int nodeX, int nodeY, int nodeZ, double nodeOffset);

    /**
     * Encodes a double-precision height value into a single long whose most significant 32 bits represent the integer
     * height of successful node, and whose lower 32 bits represent a single-precision floating point value
     * corresponding to the offset value.
     *
     * @param height the height to encode
     * @return the encoded height
     */
    static long encode(double height) {
        int blockY = (int)Math.floor(height);
        float offset = (float) (height - blockY);

        //mask off sign bit, upcast to long
        long offsetBits = (Float.floatToRawIntBits(offset) & 0x7FFF_FFFF);

        //first 32 bits are block Y, last 32 encode offset information
        return ((long) blockY) << 32L | offsetBits;
    }

    static int height(long value) {
        return (int) (value >>> 32);
    }

    static float offset(long value) {
        int valueInt = (int) (value & 0x7FFF_FFFFL);
        return Float.intBitsToFloat(valueInt);
    }
}
