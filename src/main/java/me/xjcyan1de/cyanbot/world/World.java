package me.xjcyan1de.cyanbot.world;

import com.github.steveice10.mc.protocol.data.game.chunk.Column;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockState;
import me.xjcyan1de.cyanbot.Bot;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class World {

    private Map<ChunkCoord, Chunk> chunkMap = new HashMap<>();

    public World() {
    }

    public void mergeChunk(Bot bot, Column column) {
        final ChunkCoord chunkCoord = toKey(column.getX(), column.getZ());
        final Chunk chunk = chunkMap.computeIfAbsent(chunkCoord, key->new Chunk(this, key.x, key.z));
        chunk.addView(bot);

        chunk.merge(column);
    }

    private ChunkCoord toKey(Chunk chunk) {
        return toKey(chunk.getX(), chunk.getZ());
    }

    private ChunkCoord toKey(int x, int z) {
        return new ChunkCoord(x, z);
    }

    public void removeChunk(Chunk chunk) {
        chunkMap.remove(toKey(chunk));
    }

    public Chunk getChunkAt(int x, int z) {
        return chunkMap.get(toKey(x, z));
    }

    public void setBlockState(int x, int y, int z, BlockState state) {
        Chunk chunkAt = this.getChunkAt(x >> 4, z >> 4);
        chunkAt.setBlockState(y >> 4, x & 15, y & 15, z & 15, state);
    }

    public Block getBlockAt(Location location) {
        return getBlockAt(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public Block getBlockAt(int x, int y, int z) {
        if (y < 0 || y > 255) {
            return null;
        }
        int sectionY = y >> 4;
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        Chunk chunk = getChunkAt(chunkX, chunkZ);
        if (chunk != null) {
            return chunk.getBlockAt(sectionY, x - (chunkX << 4), y - (sectionY << 4), z - (chunkZ << 4));
        } else {
            return null;
        }
    }

    public boolean isChunkLoaded(Location location) {
        return getChunkAt(location.getBlockX() >> 4, location.getBlockZ() >> 4) != null;
    }

    public boolean hasChunkAt(Location loc) {
        return chunkMap.containsKey(toKey(loc.getBlockX() >> 4, loc.getBlockZ() >> 4));
    }

    public void removeView(Bot bot, Chunk chunk) {
        chunk.removeView(bot);
        if (chunk.getView().isEmpty()) {
            this.removeChunk(chunk);
        }
    }

    public void removeViewAll(Bot bot) {
        chunkMap.values().removeIf(chunk -> {
           chunk.removeView(bot);
           return chunk.getView().isEmpty();
        });
    }

    public void checkRemoveChunks() {
        chunkMap.values().removeIf(chunk -> chunk.getView().isEmpty());
    }

    public Map<ChunkCoord, Chunk> getChunkMap() {
        return chunkMap;
    }

    private static class ChunkCoord {

        private int x, z;

        private ChunkCoord(int x, int z) {
            this.x = x;
            this.z = z;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ChunkCoord that = (ChunkCoord) o;
            return x == that.x &&
                    z == that.z;
        }

        @Override
        public int hashCode() {

            return Objects.hash(x, z);
        }
    }
}
