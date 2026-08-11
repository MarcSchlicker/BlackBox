package net.mcreator.blackbox.util;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.Set;

@EventBusSubscriber
public final class FarmMobSpawnRules {
	private static final Set<CellKey> ENABLED_CELLS = new HashSet<>();

	private FarmMobSpawnRules() {
	}

	public static void setCellEnabled(ServerLevel level, FarmCell cell, boolean enabled) {
		CellKey key = new CellKey(level.dimension(), cell.centerChunkX(), cell.centerChunkZ());
		if (enabled) {
			ENABLED_CELLS.add(key);
		} else {
			ENABLED_CELLS.remove(key);
		}
	}

	public static void clearCell(ServerLevel level, FarmCell cell) {
		ENABLED_CELLS.remove(new CellKey(level.dimension(), cell.centerChunkX(), cell.centerChunkZ()));
	}

	@SubscribeEvent
	public static void onSpawnPlacement(MobSpawnEvent.SpawnPlacementCheck event) {
		ServerLevel level = event.getLevel().getLevel();
		if (!FarmEnvironment.isFarmDimension(level.dimension()) || !isAutomaticSpawn(event.getSpawnType())) {
			return;
		}
		if (!isEnabledAt(level, event.getPos())) {
			event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
		}
	}

	private static boolean isEnabledAt(ServerLevel level, BlockPos pos) {
		int chunkX = pos.getX() >> 4;
		int chunkZ = pos.getZ() >> 4;
		for (CellKey key : ENABLED_CELLS) {
			if (key.dimension.equals(level.dimension()) && Math.abs(chunkX - key.centerChunkX) <= 1 && Math.abs(chunkZ - key.centerChunkZ) <= 1) {
				return true;
			}
		}
		return false;
	}

	private static boolean isAutomaticSpawn(MobSpawnType spawnType) {
		return spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION || spawnType == MobSpawnType.STRUCTURE || spawnType == MobSpawnType.PATROL;
	}

	private record CellKey(ResourceKey<Level> dimension, int centerChunkX, int centerChunkZ) {
	}
}
