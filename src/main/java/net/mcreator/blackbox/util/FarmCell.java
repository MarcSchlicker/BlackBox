package net.mcreator.blackbox.util;

import net.minecraft.core.BlockPos;

import java.util.UUID;

public record FarmCell(UUID coreId, int centerChunkX, int centerChunkZ, int sizeChunks) {
	public static final int CELL_SPACING_CHUNKS = 64;
	public static final int MIN_SIZE_CHUNKS = 1;
	public static final int MAX_SIZE_CHUNKS = 3;
	private static final int GRID_RADIUS = 20_000;

	public FarmCell {
		sizeChunks = Math.max(MIN_SIZE_CHUNKS, Math.min(MAX_SIZE_CHUNKS, sizeChunks));
	}

	public static FarmCell fromCoreId(UUID coreId) {
		return fromCoreId(coreId, MAX_SIZE_CHUNKS);
	}

	public static FarmCell fromCoreId(UUID coreId, int sizeChunks) {
		long mixedX = coreId.getMostSignificantBits() ^ Long.rotateLeft(coreId.getLeastSignificantBits(), 17);
		long mixedZ = coreId.getLeastSignificantBits() ^ Long.rotateLeft(coreId.getMostSignificantBits(), 29);
		int gridX = (int) Math.floorMod(mixedX, GRID_RADIUS * 2L + 1L) - GRID_RADIUS;
		int gridZ = (int) Math.floorMod(mixedZ, GRID_RADIUS * 2L + 1L) - GRID_RADIUS;
		return new FarmCell(coreId, gridX * CELL_SPACING_CHUNKS, gridZ * CELL_SPACING_CHUNKS, sizeChunks);
	}

	public int minChunkX() {
		return centerChunkX - (sizeChunks - 1) / 2;
	}

	public int maxChunkX() {
		return minChunkX() + sizeChunks - 1;
	}

	public int minChunkZ() {
		return centerChunkZ - (sizeChunks - 1) / 2;
	}

	public int maxChunkZ() {
		return minChunkZ() + sizeChunks - 1;
	}

	public int minBlockX() {
		return minChunkX() << 4;
	}

	public int maxBlockX() {
		return ((maxChunkX() + 1) << 4) - 1;
	}

	public int minBlockZ() {
		return minChunkZ() << 4;
	}

	public int maxBlockZ() {
		return ((maxChunkZ() + 1) << 4) - 1;
	}

	public BlockPos spawnPos() {
		return new BlockPos(minBlockX() + sizeChunks * 8, 1, minBlockZ() + sizeChunks * 8);
	}

	public BlockPos inputPos() {
		return spawnPos().offset(2, 0, 0);
	}

	public BlockPos outputPos() {
		return spawnPos().offset(-2, 0, 0);
	}

	public boolean contains(BlockPos pos) {
		return pos.getX() >= minBlockX() && pos.getX() <= maxBlockX() && pos.getZ() >= minBlockZ() && pos.getZ() <= maxBlockZ();
	}

	public FarmCell maximumArea() {
		return fromCoreId(coreId, MAX_SIZE_CHUNKS);
	}
}
