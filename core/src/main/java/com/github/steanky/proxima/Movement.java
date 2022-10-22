package com.github.steanky.proxima;

/**
 * Specifies the directionality of movement to a node's parent. If the parent is null, movement should be unknown.
 */
public enum Movement {
    UNKNOWN,
    BIDIRECTIONAL, UNIDIRECTIONAL
}
