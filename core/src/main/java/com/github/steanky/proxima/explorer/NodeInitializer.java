package com.github.steanky.proxima.explorer;

@FunctionalInterface
public interface NodeInitializer {
    void initialize(int x, int y, int z, float blockOffset, float jumpOffset);
}
