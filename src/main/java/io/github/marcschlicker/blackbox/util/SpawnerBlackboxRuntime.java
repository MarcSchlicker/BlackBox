package io.github.marcschlicker.blackbox.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import io.github.marcschlicker.blackbox.block.entity.SpawnerBlackboxBlockEntity;

/** Runs stored spawners as individual loot rolls, preserving naturally uneven rare drops. */
public final class SpawnerBlackboxRuntime {
	public static final int TICK_INTERVAL = 20;
	private static final int MAX_CATCH_UP_CYCLES = 4;
	private static final int MAX_MOBS_PER_CYCLE = 128;

	private SpawnerBlackboxRuntime() {
	}

	public static void tick(ServerLevel level, BlockPos pos) {
		if (!(level.getBlockEntity(pos) instanceof SpawnerBlackboxBlockEntity blackbox)) {
			return;
		}
		blackbox.outputBuffer().flushItemsInto(blackbox, SpawnerBlackboxBlockEntity.OUTPUT_START, SpawnerBlackboxBlockEntity.OUTPUT_END);
		blackbox.flushExperienceFluid();
		if (!blackbox.isReady()) {
			blackbox.syncToClient();
			return;
		}
		long now = level.getGameTime();
		java.util.List<ItemStack> upgrades = blackbox.masherUpgrades();
		for (int slot = SpawnerBlackboxBlockEntity.SPAWNER_START; slot < SpawnerBlackboxBlockEntity.SPAWNER_END; slot++) {
			int index = slot - SpawnerBlackboxBlockEntity.SPAWNER_START;
			ItemStack spawner = blackbox.getItem(slot);
			SpawnerIntegration.SpawnerProfile profile = SpawnerIntegration.readProfile(spawner);
			if (profile == null) {
				blackbox.setNextSpawnTick(index, 0L);
				continue;
			}
			long nextTick = blackbox.nextSpawnTick(index);
			if (nextTick <= 0L) {
				blackbox.setNextSpawnTick(index, now + nextDelay(blackbox, profile));
				continue;
			}
			int cycles = 0;
			while (now >= nextTick && cycles++ < MAX_CATCH_UP_CYCLES) {
				rollSpawnerCycle(level, pos, blackbox, slot, spawner, profile, upgrades);
				nextTick += nextDelay(blackbox, profile);
			}
			if (now >= nextTick) {
				// Avoid unloading a chunk for hours and then resolving an unbounded pile of mobs in one tick.
				nextTick = now + nextDelay(blackbox, profile);
			}
			blackbox.setNextSpawnTick(index, nextTick);
		}
		blackbox.outputBuffer().flushItemsInto(blackbox, SpawnerBlackboxBlockEntity.OUTPUT_START, SpawnerBlackboxBlockEntity.OUTPUT_END);
		blackbox.flushExperienceFluid();
		blackbox.syncToClient();
	}

	private static void rollSpawnerCycle(ServerLevel level, BlockPos pos, SpawnerBlackboxBlockEntity blackbox, int sourceSlot, ItemStack spawner,
			SpawnerIntegration.SpawnerProfile profile, java.util.List<ItemStack> upgrades) {
		int mobs = Math.min(MAX_MOBS_PER_CYCLE, profile.spawnCount());
		for (int mob = 0; mob < mobs; mob++) {
			SpawnerIntegration.LootRoll roll = SpawnerIntegration.rollMobLoot(level, profile, blackbox.nextRandomInt(Integer.MAX_VALUE), upgrades, pos);
			blackbox.recordLootRoll(sourceSlot, spawner, roll.experience());
			for (ItemStack drop : roll.drops()) {
				blackbox.recordObservedDrop(sourceSlot, spawner, drop);
				if (blackbox.isDropAllowed(drop)) {
					blackbox.outputBuffer().addItem(drop, drop.getCount());
				}
			}
		}
	}

	private static int nextDelay(SpawnerBlackboxBlockEntity blackbox, SpawnerIntegration.SpawnerProfile profile) {
		int spread = profile.maxDelay() - profile.minDelay() + 1;
		return profile.minDelay() + blackbox.nextRandomInt(spread);
	}
}
