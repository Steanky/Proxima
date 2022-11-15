package com.github.steanky.proxima;

import com.github.steanky.proxima.path.PathResult;
import org.jetbrains.annotations.NotNull;

public interface Navigator {
    void navigate(double x, double y, double z, double toX, double toY, double toZ);

    boolean navigationComplete();

    @NotNull PathResult getResult();

    void cancel();
}
