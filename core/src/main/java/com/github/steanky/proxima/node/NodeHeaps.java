package com.github.steanky.proxima.node;

import it.unimi.dsi.fastutil.objects.ObjectHeaps;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

/**
 * Utility method providing binary min-heap operations on {@link Node} arrays.
 *
 * @see ObjectHeaps
 * @see NodeQueue
 */
final class NodeHeaps {
    /**
     * Method based on {@link ObjectHeaps#downHeap(Object[], int, int, Comparator)}, but specialized for {@link Node}
     * objects. This will ensure that the heap index of all moved nodes is updated to match its position in the actual
     * array.
     *
     * @param heap the min-heap of nodes
     * @param size the size of the heap
     * @param i    the index of the node to potentially move down
     */
    static void downHeap(@NotNull Node @NotNull [] heap, int size, int i) {
        Node first = heap[i];
        int child;
        while ((child = (i << 1) + 1) < size) {
            Node childNode = heap[child];
            int right = child + 1;
            if (right < size && (heap[right].compareTo(childNode) < 0)) {
                childNode = heap[child = right];
            }

            if (first.compareTo(childNode) <= 0) {
                break;
            }

            heap[i] = childNode;
            childNode.heapIndex = i;
            i = child;
        }
        heap[i] = first;
        first.heapIndex = i;
    }

    /**
     * Method based on {@link ObjectHeaps#upHeap(Object[], int, int, Comparator)}, but specialized for {@link Node}
     * objects. This will ensure that the heap index of all moved nodes is updated to match its position in the actual
     * array.
     *
     * @param heap the min-heap of nodes
     * @param i    the index of the node to potentially move up
     *
     * @return the final index of the node which was moved
     */
    static int upHeap(@NotNull Node @NotNull [] heap, int i) {
        Node e = heap[i];
        while (i != 0) {
            int parentIndex = (i - 1) >>> 1;
            Node parentNode = heap[parentIndex];
            if (parentNode.compareTo(e) <= 0) {
                break;
            }

            heap[i] = parentNode;
            parentNode.heapIndex = i;
            i = parentIndex;
        }

        heap[i] = e;
        e.heapIndex = i;
        return i;
    }
}