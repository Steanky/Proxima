package com.github.steanky.proxima.space;

import com.github.steanky.proxima.solid.Solid;
import com.github.steanky.vector.Vec3I;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A thread-safe {@link Space} implementation. Must be subclassed by a class which provides actual {@link Solid}
 * instances from some source, for example a Minecraft chunk.
 * <p>
 * This class is designed to minimize lock contention in the common case of heavily concurrent reads and relatively few
 * writes. It is often the case that simply <i>reading</i> {@link Solid} objects is poorly performant or not conducive
 * to massively parallel reads. The {@link ConcurrentCachingSpace#loadSolid(int, int, int)} method is expected to
 * utilize algorithms like this; it may be slow to read from, or the data may be synchronized in such a way that
 * concurrent reads are not possible. Solids returned from this method are cached in a parallelism-friendly data
 * structure that allows as much concurrent access as possible.
 */
public abstract class ConcurrentCachingSpace implements Space {
    private final SpatialCache<Solid> cache;

    public ConcurrentCachingSpace() {
        this.cache = new ConcurrentChunkedSpatialCache<>();
    }

    @Override
    public final @NotNull Solid solidAt(int x, int y, int z) {
        return cache.computeIfAbsent(x, y, z, this::loadSolid);
    }

    @Override
    public final @NotNull Solid solidAt(@NotNull Vec3I vec) {
        return solidAt(vec.x(), vec.y(), vec.z());
    }

    /**
     * Updates the solid at the given position. If the solid is null, any solid that was cached at that location will
     * be removed, to be re-computed when needed.
     *
     * @param x the x-coordinate of the solid to update
     * @param y the y-coordinate of the solid to update
     * @param z the z-coordinate of the solid to update
     * @param solid the new solid, or null to remove any cached solid (if present)
     */
    public void updateSolid(int x, int y, int z, @Nullable Solid solid) {
        if (solid == null) {
            cache.remove(x, y, z);
        }
        else {
            cache.put(x, y, z, solid);
        }
    }

    /**
     * Loads a solid, which will be cached in this space until it is invalidated. This method is called by
     * {@link ConcurrentCachingSpace#solidAt(int, int, int)} when it encounters a cache miss.
     * <p>
     * Threads calling this method will typically not hold any write locks. Therefore, implementations should expect
     * that this method will be called concurrently by two or more threads.
     *
     * @param x the x-coordinate of the solid to load
     * @param y the y-coordinate of the solid to load
     * @param z the z-coordinate of the solid to load
     * @return a Solid object
     */
    public abstract @NotNull Solid loadSolid(int x, int y, int z);
}
