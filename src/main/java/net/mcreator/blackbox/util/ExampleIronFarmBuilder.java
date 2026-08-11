package net.mcreator.blackbox.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;

import net.mcreator.blackbox.init.BlackboxModBlocks;

public final class ExampleIronFarmBuilder {
	private ExampleIronFarmBuilder() {
	}

	public static void ensureBuilt(ServerLevel level, FarmCell cell) {
		BlockPos center = cell.spawnPos().offset(-8, 0, 0);
		BlockPos markerPos = center.offset(-4, 0, -4);
		if (!isCurrentStructure(level, markerPos)) {
			rebuild(level, cell, center, markerPos);
		}
		configureSpawner(level, center.above(4));
	}

	private static boolean isCurrentStructure(ServerLevel level, BlockPos markerPos) {
		return level.getBlockState(markerPos).is(Blocks.CHISELED_TUFF)
				&& level.getBlockEntity(markerPos.above()) == null
				&& level.getBlockState(markerPos.above(3)).is(Blocks.GLASS);
	}

	private static void rebuild(ServerLevel level, FarmCell cell, BlockPos center, BlockPos markerPos) {
		for (int x = -4; x <= 6; x++) {
			for (int y = 0; y <= 7; y++) {
				for (int z = -4; z <= 4; z++) {
					level.removeBlock(center.offset(x, y, z), false);
				}
			}
		}

		level.setBlock(cell.outputPos(), BlackboxModBlocks.OUTPUT_BLOCK.get().defaultBlockState(), 3);
		for (int x = -3; x <= 5; x++) {
			for (int z = -3; z <= 3; z++) {
				Direction direction = z < 0 ? Direction.SOUTH : z > 0 ? Direction.NORTH : Direction.EAST;
				level.setBlock(center.offset(x, 0, z), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, direction), 3);
			}
		}

		for (int y = 1; y <= 6; y++) {
			for (int axis = -4; axis <= 4; axis++) {
				level.setBlock(center.offset(-4, y, axis), Blocks.GLASS.defaultBlockState(), 3);
				level.setBlock(center.offset(4, y, axis), Blocks.GLASS.defaultBlockState(), 3);
				level.setBlock(center.offset(axis, y, -4), Blocks.GLASS.defaultBlockState(), 3);
				level.setBlock(center.offset(axis, y, 4), Blocks.GLASS.defaultBlockState(), 3);
			}
		}
		for (int x = -4; x <= 4; x++) {
			for (int z = -4; z <= 4; z++) {
				level.setBlock(center.offset(x, 7, z), Blocks.GLASS.defaultBlockState(), 3);
			}
		}

		for (int x = -2; x <= 2; x++) {
			for (int z = -2; z <= 2; z++) {
				level.setBlock(center.offset(x, 1, z), Blocks.LAVA.defaultBlockState(), 3);
			}
		}
		level.setBlock(markerPos, Blocks.CHISELED_TUFF.defaultBlockState(), 3);
		level.setBlock(center.above(4), Blocks.SPAWNER.defaultBlockState(), 3);
	}

	private static void configureSpawner(ServerLevel level, BlockPos spawnerPos) {
		SpawnerBlockEntity spawner;
		if (level.getBlockEntity(spawnerPos) instanceof SpawnerBlockEntity existing) {
			spawner = existing;
		} else {
			level.setBlock(spawnerPos, Blocks.SPAWNER.defaultBlockState(), 3);
			if (!(level.getBlockEntity(spawnerPos) instanceof SpawnerBlockEntity created)) {
				return;
			}
			spawner = created;
		}
		spawner.getSpawner().setEntityId(EntityType.IRON_GOLEM, level, level.getRandom(), spawnerPos);
		CompoundTag settings = spawner.getSpawner().save(new CompoundTag());
		CompoundTag spawnData = settings.getCompound("SpawnData");
		CompoundTag entityData = spawnData.getCompound("entity");
		entityData.putFloat("Health", 12.0F);
		spawnData.put("entity", entityData);
		spawnData.put("custom_spawn_rules", new CompoundTag());
		settings.put("SpawnData", spawnData);
		settings.putShort("Delay", (short) 20);
		settings.putShort("MinSpawnDelay", (short) 40);
		settings.putShort("MaxSpawnDelay", (short) 80);
		settings.putShort("SpawnCount", (short) 1);
		settings.putShort("MaxNearbyEntities", (short) 4);
		settings.putShort("RequiredPlayerRange", (short) 32);
		settings.putShort("SpawnRange", (short) 2);
		spawner.getSpawner().load(level, spawnerPos, settings);
		spawner.setChanged();
		level.sendBlockUpdated(spawnerPos, level.getBlockState(spawnerPos), level.getBlockState(spawnerPos), 3);
	}

	public static void spawnTestGolem(ServerLevel level, FarmCell cell) {
		BlockPos spawn = cell.spawnPos().offset(-8, 3, 0);
		IronGolem golem = EntityType.IRON_GOLEM.create(level);
		if (golem != null) {
			golem.setHealth(12.0F);
			golem.moveTo(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D, 0.0F, 0.0F);
			level.addFreshEntity(golem);
		}
	}
}
