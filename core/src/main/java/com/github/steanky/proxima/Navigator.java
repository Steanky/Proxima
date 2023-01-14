package com.github.steanky.proxima;

import com.github.steanky.proxima.path.PathResult;
import com.github.steanky.proxima.path.PathTarget;
import org.jetbrains.annotations.NotNull;

public interface Navigator {
    void navigate(double x, double y, double z, @NotNull PathTarget target);

    boolean navigationComplete();

    @NotNull PathResult getResult();

    void cancel();
}
