package com.github.steanky.proxima;

@FunctionalInterface
public interface NodeHandler {
    void handle(Node node, int x, int y, int z);
}
