package com.github.steanky.proxima;

import com.github.steanky.vector.Vec3I;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Set;

final class Node implements Comparable<Node> {
    final int x;
    final int y;
    final int z;

    float g;
    final float h;

    final Movement movement;
    Node parent;

    int heapIndex;

    public Node(int x, int y, int z, float g, float h, @NotNull Movement movement, @Nullable Node parent) {
        this.x = x;
        this.y = y;
        this.z = z;

        this.g = g;
        this.h = h;

        this.movement = movement;
        this.parent = parent;
        this.heapIndex = -1;
    }

    @NotNull @Unmodifiable Set<Vec3I> reverseToVectorSet() {
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
            set.add(Vec3I.immutable(prev.x, prev.y, prev.z));
            prev = prev.parent;
        }
        while (prev != null);

        return ObjectSets.unmodifiable(set);
    }

    @NotNull Vec3I[] asVectorArray() {
        Node current = this;
        int size = 0;
        while (current != null) {
            current = current.parent;
            size++;
        }

        current = this;
        Vec3I[] array = new Vec3I[size];
        for (int i = 0; i < size && current != null; i++) {
            array[i] = Vec3I.immutable(current.x, current.y, current.z);
            current = current.parent;
        }

        return array;
    }

    boolean onHeap() {
        return heapIndex > -1;
    }

    @Override
    public int compareTo(@NotNull Node o) {
        return Float.compare(g + h, o.g + o.h);
    }
}
