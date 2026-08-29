package net.mcreator.blackbox.init;

import com.google.common.collect.ImmutableSet;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;

import net.mcreator.blackbox.BlackboxMod;

import java.util.function.Predicate;

public final class BlackboxModVillagers {
	private static final Predicate<Holder<PoiType>> NO_JOB_SITE = holder -> false;
	public static final DeferredRegister<VillagerProfession> REGISTRY = DeferredRegister.create(Registries.VILLAGER_PROFESSION, BlackboxMod.MODID);
	public static final DeferredHolder<VillagerProfession, VillagerProfession> DIMENSIONAL_ARCHIVIST = REGISTRY.register("dimensional_archivist",
			() -> new VillagerProfession("dimensional_archivist", NO_JOB_SITE, NO_JOB_SITE, ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_WORK_LIBRARIAN));

	private BlackboxModVillagers() {
	}
}
