package com.github.steanky.proxima.node;

import com.github.steanky.proxima.snapper.NodeSnapper;
import com.github.steanky.vector.Vec3I;
import com.github.steanky.vector.Vec3I2ObjectMap;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

class DiagonalProcessor implements NodeProcessor {
    private final NodeSnapper nodeSnapper;

    DiagonalProcessor(@NotNull NodeSnapper nodeSnapper) {
        this.nodeSnapper = Objects.requireNonNull(nodeSnapper);
    }

    @Override
    public void processPath(@NotNull Node head, @NotNull Vec3I2ObjectMap<Node> graph) {
        Node previous = null;
        Node current = head;

        while (current != null) {
            if (previous == null) {
                previous = current;
                current = current.parent;
                continue;
            }

            Node next = current.parent;
            if (next == null) {
                break;
            }

            int px = previous.x;
            int py = previous.y;
            int pz = previous.z;

            int nx = next.x;
            int ny = next.y;
            int nz = next.z;

            if (py == ny && px != nx && pz != nz && previous.blockOffset == next.blockOffset &&
                    previous.jumpOffset == next.jumpOffset && Vec3I.distanceSquared(px, py, pz, nx, ny, nz) == 2 &&
                    nodeSnapper.checkDiagonal(px, py, pz, nx, nz, previous.blockOffset)) {
                previous.parent = next;
                current = next.parent;
                previous = next;
            } else {
                previous = current;
                current = next;
            }
        }
    }
}
