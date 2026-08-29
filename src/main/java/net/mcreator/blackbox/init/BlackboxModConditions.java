package net.mcreator.blackbox.init;

import com.mojang.serialization.MapCodec;

import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import net.mcreator.blackbox.BlackboxMod;
import net.mcreator.blackbox.conditions.VillageArchiveEnabledCondition;

public final class BlackboxModConditions {
	public static final DeferredRegister<MapCodec<? extends ICondition>> REGISTRY = DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, BlackboxMod.MODID);

	static {
		REGISTRY.register("village_archive_enabled", () -> VillageArchiveEnabledCondition.CODEC);
	}

	private BlackboxModConditions() {
	}
}
