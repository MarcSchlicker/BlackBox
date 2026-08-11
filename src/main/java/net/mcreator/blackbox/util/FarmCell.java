package net.mcreator.blackbox.util;

import net.minecraft.core.BlockPos;

import java.util.UUID;

public record FarmCell(UUID coreId, int centerChunkX, int centerChunkZ) {
	public static final int CELL_SPACING_CHUNKS = 64;
	private static final int GRID_RADIUS = 20_000;

	public static FarmCell fromCoreId(UUID coreId) {
		long mixedX = coreId.getMostSignificantBits() ^ Long.rotateLeft(coreId.getLeastSignificantBits(), 17);
		long mixedZ = coreId.getLeastSignificantBits() ^ Long.rotateLeft(coreId.getMostSignificantBits(), 29);
		int gridX = (int) Math.floorMod(mixedX, GRID_RADIUS * 2L + 1L) - GRID_RADIUS;
		int gridZ = (int) Math.floorMod(mixedZ, GRID_RADIUS * 2L + 1L) - GRID_RADIUS;
		return new FarmCell(coreId, gridX * CELL_SPACING_CHUNKS, gridZ * CELL_SPACING_CHUNKS);
	}

	public int minChunkX() {
		return centerChunkX - 1;
	}

	public int maxChunkX() {
		return centerChunkX + 1;
	}

	public int minChunkZ() {
		return centerChunkZ - 1;
	}

	public int maxChunkZ() {
		return centerChunkZ + 1;
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
		return new BlockPos((centerChunkX << 4) + 8, 1, (centerChunkZ << 4) + 8);
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
}
