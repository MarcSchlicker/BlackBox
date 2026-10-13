package io.github.marcschlicker.blackbox.util;

import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import io.github.marcschlicker.blackbox.BlackboxMod;
import io.github.marcschlicker.blackbox.block.entity.BlackboxBlockBlockEntity;
import io.github.marcschlicker.blackbox.block.entity.DimensionalWorkbenchBlockEntity;
import io.github.marcschlicker.blackbox.init.BlackboxModBlocks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/** Converts legacy Workbenches without dropping or rewriting their stored farm data. */
@EventBusSubscriber(modid = BlackboxMod.MODID)
public final class LegacyWorkbenchMigration {
	private static final Set<MigrationPosition> ACTIVE_MIGRATIONS = new HashSet<>();

	private LegacyWorkbenchMigration() {
	}

	@SubscribeEvent
	public static void migrateLoadedChunk(ChunkEvent.Load event) {
		if (!(event.getLevel() instanceof ServerLevel level) || !(event.getChunk() instanceof LevelChunk chunk)) {
			return;
		}
		for (BlockPos position : new ArrayList<>(chunk.getBlockEntities().keySet())) {
			migrateAt(level, position);
		}
	}

	/** Also covers a legacy Workbench item placed after the migration update is installed. */
	public static boolean migrateAt(ServerLevel level, BlockPos position) {
		if (!(level.getBlockEntity(position) instanceof DimensionalWorkbenchBlockEntity legacy)) {
			return false;
		}

		CompoundTag saved = legacy.saveWithFullMetadata(level.registryAccess());
		MigrationPosition key = new MigrationPosition(level.dimension(), position.immutable());
		ACTIVE_MIGRATIONS.add(key);
		try {
			if (!level.setBlock(position, BlackboxModBlocks.BLACKBOX_BLOCK.get().defaultBlockState(), Block.UPDATE_ALL)) {
				return false;
			}
		} finally {
			ACTIVE_MIGRATIONS.remove(key);
		}

		BlockEntity replacement = level.getBlockEntity(position);
		if (!(replacement instanceof BlackboxBlockBlockEntity blackbox)) {
			BlackboxMod.LOGGER.error("Could not migrate legacy Workbench at {} in {}: replacement BlockEntity is missing", position, level.dimension().location());
			return false;
		}
		blackbox.loadLegacyWorkbenchData(saved, level.registryAccess());
		level.invalidateCapabilities(position);
		level.sendBlockUpdated(position, level.getBlockState(position), level.getBlockState(position), Block.UPDATE_ALL);
		BlackboxMod.LOGGER.info("Migrated legacy Dimensional Workbench at {} in {} to Blackbox", position, level.dimension().location());
		return true;
	}

	public static boolean isMigrating(ServerLevel level, BlockPos position) {
		return ACTIVE_MIGRATIONS.contains(new MigrationPosition(level.dimension(), position));
	}

	private record MigrationPosition(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, BlockPos position) {
	}
}
