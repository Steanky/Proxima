package com.github.steanky.proxima.snapper;

import com.github.steanky.proxima.Direction;
import org.jetbrains.annotations.NotNull;

public interface NodeSnapper {
    long FAIL = 0xFFC0_0001L; //lower 32 bits represent a floating-point qNaN with a payload of all 0s

    static long encode(double height, boolean intermediateJump, float offset) {
        int heightBits = Float.floatToRawIntBits((float) height);

        //if the node requires an intermediate jump to get to
        long jumpBits = intermediateJump ? 0x8000_0000L : 0;

        //in case of an intermediate jump, stores the additional height needed to make the jump
        long offsetBits = Float.floatToRawIntBits(offset) & 0x7FFF_FFFF;

        //first 32 bits are block Y, last 32 encode offset information
        return (((long) heightBits) << 32L) | jumpBits | offsetBits;
    }

    static float height(long value) {
        return Float.intBitsToFloat((int) (value >>> 32));
    }

    static int blockHeight(long value) {
        return (int) Math.floor(height(value));
    }

    static float blockOffset(long value) {
        float height = height(value);
        return height - (float) Math.floor(height);
    }

    static boolean intermediateJump(long value) {
        return (value & 0x8000_0000L) != 0;
    }

    static float jumpOffset(long value) {
        return Float.intBitsToFloat((int) (value & 0x7FFF_FFFFL));
    }

    long snap(@NotNull Direction direction, int nodeX, int nodeY, int nodeZ, float nodeOffset);

    long checkInitial(double x, double y, double z, int tx, int ty, int tz);

    boolean checkDiagonal(int x, int y, int z, int tx, int tz, float nodeOffset);
}
