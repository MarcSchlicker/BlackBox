package net.mcreator.blackbox.conditions;

import com.mojang.serialization.MapCodec;

import net.neoforged.neoforge.common.conditions.ICondition;

import net.mcreator.blackbox.config.BlackboxConfig;

public final class VillageArchiveEnabledCondition implements ICondition {
	public static final VillageArchiveEnabledCondition INSTANCE = new VillageArchiveEnabledCondition();
	public static final MapCodec<VillageArchiveEnabledCondition> CODEC = MapCodec.unit(INSTANCE).stable();

	private VillageArchiveEnabledCondition() {
	}

	@Override
	public boolean test(IContext context) {
		return BlackboxConfig.isVillageArchiveEnabled();
	}

	@Override
	public MapCodec<? extends ICondition> codec() {
		return CODEC;
	}
}
