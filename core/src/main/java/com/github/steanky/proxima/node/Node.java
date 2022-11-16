package com.github.steanky.proxima.node;

import com.github.steanky.vector.Vec3I;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Set;

public final class Node implements Comparable<Node> {
    public final int x;
    public final int y;
    public final int z;

    public float g;
    public final float h;
    public final double yOffset;

    public Node parent;

    int heapIndex;

    public Node(int x, int y, int z, float g, float h, @Nullable Node parent, double yOffset) {
        this.x = x;
        this.y = y;
        this.z = z;

        this.g = g;
        this.h = h;

        this.yOffset = yOffset;

        this.parent = parent;
        this.heapIndex = -1;
    }

    public @NotNull Vec3I vector() {
        return Vec3I.immutable(x, y, z);
    }

    public int size() {
        Node current = this;
        int size = 0;
        do {
            size++;
            current = current.parent;
        }
        while (current != null);

        return size;
    }

    public @NotNull @Unmodifiable Set<Vec3I> reverseToVectorSet() {
        if (parent == null) {
            return Set.of(vector());
        }

        Node prev = null;
        Node current = this;

        int size = 0;
        while (current != null) {
            //invert the linked list represented by this node, calculate size while we do this
            Node next = current.parent;
            current.parent = prev;

            prev = current;
            current = next;
            size++;
        }

        ObjectSet<Vec3I> set = new ObjectLinkedOpenHashSet<>(size);
        do {
            set.add(prev.vector());
            prev = prev.parent;
        }
        while (prev != null);

        return ObjectSets.unmodifiable(set);
    }

    public boolean onHeap() {
        return heapIndex > -1;
    }

    @Override
    public int compareTo(@NotNull Node o) {
        return Float.compare(g + h, o.g + o.h);
    }

    @Override
    public String toString() {
        return "Node{x=" + x + ", y=" + y + ", z=" + z + ", g=" + g + ", h=" + h + ", yOffset=" + yOffset +
                ", heapIndex=" + heapIndex + "}";
    }
}
