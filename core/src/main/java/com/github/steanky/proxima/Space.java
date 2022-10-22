package com.github.steanky.proxima;

import org.jetbrains.annotations.NotNull;

public interface Space {
    @NotNull Solid solidAt(int x, int y, int z);
}
