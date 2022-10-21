package com.github.steanky.proxima;

public interface Heuristic {
    float heuristic(int fromX, int fromY, int fromZ, int toX, int toY, int toZ);

    float distance(int fromX, int fromY, int fromZ, int toX, int toY, int toZ);
}
