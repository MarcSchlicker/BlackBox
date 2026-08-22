package net.mcreator.blackbox.util;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;

@EventBusSubscriber
public final class FarmMobAccounting {
	private static final String ORIGIN_TAG = "BlackboxMobOrigin";
	private static final String NATURAL = "natural";
	private static final String SPAWNER = "spawner";

	private FarmMobAccounting() {
	}

	@SubscribeEvent
	public static void onMobPositionCheck(MobSpawnEvent.PositionCheck event) {
		Mob mob = event.getEntity();
		if (!FarmEnvironment.isFarmDimension(mob.level().dimension())) {
			return;
		}
		MobSpawnType type = event.getSpawnType();
		if (MobSpawnType.isSpawner(type)) {
			mob.getPersistentData().putString(ORIGIN_TAG, SPAWNER);
		} else if (type == MobSpawnType.NATURAL || type == MobSpawnType.CHUNK_GENERATION || type == MobSpawnType.PATROL) {
			mob.getPersistentData().putString(ORIGIN_TAG, NATURAL);
		}
	}

	public static void markPlayerTransported(Entity entity) {
		entity.getPersistentData().remove(ORIGIN_TAG);
	}

	public static boolean shouldCount(Entity entity, boolean activeSpawnerExists) {
		if (!(entity instanceof Mob mob) || !mob.isAlive()) {
			return false;
		}
		String origin = mob.getPersistentData().getString(ORIGIN_TAG);
		return !NATURAL.equals(origin) && !(SPAWNER.equals(origin) && activeSpawnerExists);
	}
}
