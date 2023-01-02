package com.github.steanky.proxima;

import com.github.steanky.proxima.solid.Solid;
import com.github.steanky.vector.Bounds3I;
import com.github.steanky.vector.HashVec3I2ObjectMap;
import com.github.steanky.vector.Vec3I;
import com.github.steanky.vector.Vec3I2ObjectMap;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.locks.StampedLock;

/**
 * A thread-safe {@link Space} implementation.
 */
public abstract class ConcurrentCachingSpace implements Space {
    private final Vec3I2ObjectMap<Solid> cache;
    private final StampedLock stampedLock;

    public ConcurrentCachingSpace(@NotNull Bounds3I cacheBounds) {
        this.cache = new HashVec3I2ObjectMap<>(cacheBounds);
        this.stampedLock = new StampedLock();
    }

    @Override
    public final @NotNull Solid solidAt(int x, int y, int z) {
        long stamp = stampedLock.readLock();

        Solid solid = cache.get(x, y, z);
        if (solid == null) {
            //load the solid before acquiring the write lock
            try {
                solid = loadSolid(x, y, z);
            }
            catch (Throwable e) {
                //unexpected errors in loadSolid could corrupt our lock state if we didn't catch this
                //so, unlock first and rethrow as a RuntimeException
                stampedLock.unlockRead(stamp);
                throw new RuntimeException(e);
            }

            //if we successfully loaded the solid, convert to a write lock
            //this also closes the read lock
            stamp = stampedLock.tryConvertToWriteLock(stamp);
            try {
                cache.put(x, y, z, solid);
            }
            finally {
                stampedLock.unlockWrite(stamp);
            }
        }
        else {
            stampedLock.unlockRead(stamp);
        }

        return solid;
    }

    @Override
    public final @NotNull Solid solidAt(@NotNull Vec3I vec) {
        return solidAt(vec.x(), vec.y(), vec.z());
    }

    public void invalidateCache(int x, int y, int z) {
        long stamp = stampedLock.readLock();

        //we can check containsKey just with the read lock
        //this avoids costly and unnecessary write locks
        if (cache.containsKey(x, y, z)) {
            stamp = stampedLock.tryConvertToWriteLock(stamp);

            try {
                cache.remove(x, y, z);
            }
            finally {
                stampedLock.unlockWrite(stamp);
            }
        }
        else {
            stampedLock.unlockRead(stamp);
        }
    }

    public void clearCache() {
        long stamp = stampedLock.readLock();

        if (!cache.isEmpty()) {
            stamp = stampedLock.tryConvertToWriteLock(stamp);

            try {
                cache.clear();
            }
            finally {
                stampedLock.unlockWrite(stamp);
            }
        }
        else {
            stampedLock.unlockRead(stamp);
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
