package com.github.steanky.proxima.snapper;

import com.github.steanky.proxima.Direction;
import org.jetbrains.annotations.NotNull;

public interface FlightNodeSnapper {
    boolean snap(@NotNull Direction direction, int nodeX, int nodeY, int nodeZ, double nodeOffset);
}
