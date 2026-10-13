package io.github.marcschlicker.blackbox.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import net.neoforged.neoforge.fluids.FluidStack;

import io.github.marcschlicker.blackbox.init.BlackboxModItems;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class OutputFilterData {
	private static final String DISABLED_OUTPUTS_TAG = "disabled_item_outputs";
	private static final String DISABLED_SPAWNER_GROUPS_TAG = "disabled_spawner_output_groups";
	private static final String DISABLED_SPAWNER_ARMOR_TAG = "disabled_spawner_armor";
	private static final String DISABLED_SPAWNER_GEMS_TAG = "disabled_spawner_gems";
	private static final String DISABLED_FLUID_OUTPUTS_TAG = "disabled_fluid_outputs";
	private static final String DISABLED_ENERGY_OUTPUT_TAG = "disabled_energy_output";

	private OutputFilterData() {
	}

	public static boolean isEnabled(ItemStack upgrade, ItemStack output, HolderLookup.Provider lookupProvider) {
		if (!upgrade.is(BlackboxModItems.OUTPUT_FILTER_UPGRADE.get()) || output.isEmpty()) {
			return true;
		}
		return disabledOutputs(upgrade, lookupProvider).stream().noneMatch(saved -> ItemStack.isSameItemSameComponents(saved, output));
	}

	public static void toggle(ItemStack upgrade, ItemStack output, HolderLookup.Provider lookupProvider) {
		setEnabled(upgrade, output, lookupProvider, !isEnabled(upgrade, output, lookupProvider));
	}

	public static void setEnabled(ItemStack upgrade, ItemStack output, HolderLookup.Provider lookupProvider, boolean enabled) {
		if (!upgrade.is(BlackboxModItems.OUTPUT_FILTER_UPGRADE.get()) || output.isEmpty()) {
			return;
		}
		List<ItemStack> disabled = new ArrayList<>(disabledOutputs(upgrade, lookupProvider));
		disabled.removeIf(saved -> ItemStack.isSameItemSameComponents(saved, output));
		if (!enabled) {
			disabled.add(output.copyWithCount(1));
		}
		writeDisabledOutputs(upgrade, disabled, lookupProvider);
	}

	/** Uses the stable Apotheosis base-item + rarity key when one is available. */
	public static boolean isSpawnerDropEnabled(ItemStack upgrade, ItemStack output, HolderLookup.Provider lookupProvider) {
		if (!isEnabled(upgrade, output, lookupProvider)) {
			return false;
		}
		String group = SpawnerIntegration.apothicFilterGroup(output);
		return group.isEmpty() || !disabledSpawnerOutputGroups(upgrade).contains(group);
	}

	public static void toggleSpawnerDrop(ItemStack upgrade, ItemStack output, String group, HolderLookup.Provider lookupProvider) {
		if (group == null || group.isEmpty()) {
			toggle(upgrade, output, lookupProvider);
			return;
		}
		boolean enabled = disabledSpawnerOutputGroups(upgrade).contains(group);
		setSpawnerGroupEnabled(upgrade, group, enabled);
		// Keep a concrete representative too. If an optional mod temporarily
		// withholds its rarity component, that observed stack remains filtered.
		setEnabled(upgrade, output, lookupProvider, enabled);
	}

	private static void setSpawnerGroupEnabled(ItemStack upgrade, String group, boolean enabled) {
		if (!upgrade.is(BlackboxModItems.OUTPUT_FILTER_UPGRADE.get())) {
			return;
		}
		Set<String> disabled = new LinkedHashSet<>(disabledSpawnerOutputGroups(upgrade));
		if (enabled) {
			disabled.remove(group);
		} else {
			disabled.add(group);
		}
		writeDisabledSpawnerOutputGroups(upgrade, disabled);
	}

	public static boolean isFluidEnabled(ItemStack upgrade, FluidStack output, HolderLookup.Provider lookupProvider) {
		if (!upgrade.is(BlackboxModItems.OUTPUT_FILTER_UPGRADE.get()) || output.isEmpty()) {
			return true;
		}
		return disabledFluids(upgrade, lookupProvider).stream().noneMatch(saved -> FluidStack.isSameFluidSameComponents(saved, output));
	}

	public static void toggleFluid(ItemStack upgrade, FluidStack output, HolderLookup.Provider lookupProvider) {
		setFluidEnabled(upgrade, output, lookupProvider, !isFluidEnabled(upgrade, output, lookupProvider));
	}

	public static void setFluidEnabled(ItemStack upgrade, FluidStack output, HolderLookup.Provider lookupProvider, boolean enabled) {
		if (!upgrade.is(BlackboxModItems.OUTPUT_FILTER_UPGRADE.get()) || output.isEmpty()) {
			return;
		}
		List<FluidStack> disabled = new ArrayList<>(disabledFluids(upgrade, lookupProvider));
		disabled.removeIf(saved -> FluidStack.isSameFluidSameComponents(saved, output));
		if (!enabled) {
			disabled.add(output.copyWithAmount(1));
		}
		writeDisabledFluids(upgrade, disabled, lookupProvider);
	}

	public static boolean isSpawnerArmorEnabled(ItemStack upgrade) {
		return isFlagEnabled(upgrade, DISABLED_SPAWNER_ARMOR_TAG);
	}

	public static void toggleSpawnerArmor(ItemStack upgrade) {
		setFlagEnabled(upgrade, DISABLED_SPAWNER_ARMOR_TAG, !isSpawnerArmorEnabled(upgrade));
	}

	public static boolean isSpawnerGemsEnabled(ItemStack upgrade) {
		return isFlagEnabled(upgrade, DISABLED_SPAWNER_GEMS_TAG);
	}

	public static void toggleSpawnerGems(ItemStack upgrade) {
		setFlagEnabled(upgrade, DISABLED_SPAWNER_GEMS_TAG, !isSpawnerGemsEnabled(upgrade));
	}

	public static boolean isEnergyEnabled(ItemStack upgrade) {
		return !upgrade.is(BlackboxModItems.OUTPUT_FILTER_UPGRADE.get())
				|| !upgrade.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean(DISABLED_ENERGY_OUTPUT_TAG);
	}

	public static void toggleEnergy(ItemStack upgrade) {
		setEnergyEnabled(upgrade, !isEnergyEnabled(upgrade));
	}

	public static void setEnergyEnabled(ItemStack upgrade, boolean enabled) {
		setFlagEnabled(upgrade, DISABLED_ENERGY_OUTPUT_TAG, enabled);
	}

	public static void enableAll(ItemStack upgrade) {
		if (!upgrade.is(BlackboxModItems.OUTPUT_FILTER_UPGRADE.get())) {
			return;
		}
		CustomData.update(DataComponents.CUSTOM_DATA, upgrade, tag -> {
			tag.remove(DISABLED_OUTPUTS_TAG);
			tag.remove(DISABLED_SPAWNER_GROUPS_TAG);
			tag.remove(DISABLED_SPAWNER_ARMOR_TAG);
			tag.remove(DISABLED_SPAWNER_GEMS_TAG);
			tag.remove(DISABLED_FLUID_OUTPUTS_TAG);
			tag.remove(DISABLED_ENERGY_OUTPUT_TAG);
		});
	}

	public static boolean hasDisabledOutputs(ItemStack upgrade, HolderLookup.Provider lookupProvider) {
		return !disabledOutputs(upgrade, lookupProvider).isEmpty() || !disabledSpawnerOutputGroups(upgrade).isEmpty()
				|| !disabledFluids(upgrade, lookupProvider).isEmpty() || !isEnergyEnabled(upgrade)
				|| !isSpawnerArmorEnabled(upgrade) || !isSpawnerGemsEnabled(upgrade);
	}

	/** Parsed once by screens that render a full grid every frame. */
	public static List<ItemStack> disabledItemOutputs(ItemStack upgrade, HolderLookup.Provider lookupProvider) {
		return List.copyOf(disabledOutputs(upgrade, lookupProvider));
	}

	/** Parsed once by screens that render a full grid every frame. */
	public static List<FluidStack> disabledFluidOutputs(ItemStack upgrade, HolderLookup.Provider lookupProvider) {
		return List.copyOf(disabledFluids(upgrade, lookupProvider));
	}

	/** Parsed once by the Spawner Blackbox screen and compared using a saved group key. */
	public static Set<String> disabledSpawnerOutputGroups(ItemStack upgrade) {
		CompoundTag tag = upgrade.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		ListTag saved = tag.getList(DISABLED_SPAWNER_GROUPS_TAG, Tag.TAG_STRING);
		Set<String> groups = new LinkedHashSet<>();
		for (int index = 0; index < saved.size(); index++) {
			String group = saved.getString(index);
			if (!group.isBlank()) {
				groups.add(group);
			}
		}
		return Set.copyOf(groups);
	}

	private static List<ItemStack> disabledOutputs(ItemStack upgrade, HolderLookup.Provider lookupProvider) {
		CompoundTag tag = upgrade.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		ListTag saved = tag.getList(DISABLED_OUTPUTS_TAG, Tag.TAG_COMPOUND);
		List<ItemStack> outputs = new ArrayList<>();
		for (int index = 0; index < saved.size(); index++) {
			ItemStack stack = ItemStack.parseOptional(lookupProvider, saved.getCompound(index));
			if (!stack.isEmpty()) {
				outputs.add(stack.copyWithCount(1));
			}
		}
		return outputs;
	}

	private static List<FluidStack> disabledFluids(ItemStack upgrade, HolderLookup.Provider lookupProvider) {
		CompoundTag tag = upgrade.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		ListTag saved = tag.getList(DISABLED_FLUID_OUTPUTS_TAG, Tag.TAG_COMPOUND);
		List<FluidStack> outputs = new ArrayList<>();
		for (int index = 0; index < saved.size(); index++) {
			FluidStack stack = FluidStack.parseOptional(lookupProvider, saved.getCompound(index));
			if (!stack.isEmpty()) {
				outputs.add(stack.copyWithAmount(1));
			}
		}
		return outputs;
	}

	private static void writeDisabledOutputs(ItemStack upgrade, List<ItemStack> disabled, HolderLookup.Provider lookupProvider) {
		CustomData.update(DataComponents.CUSTOM_DATA, upgrade, tag -> {
			if (disabled.isEmpty()) {
				tag.remove(DISABLED_OUTPUTS_TAG);
				return;
			}
			ListTag saved = new ListTag();
			for (ItemStack stack : disabled) {
				saved.add(stack.copyWithCount(1).saveOptional(lookupProvider));
			}
			tag.put(DISABLED_OUTPUTS_TAG, saved);
		});
	}

	private static void writeDisabledFluids(ItemStack upgrade, List<FluidStack> disabled, HolderLookup.Provider lookupProvider) {
		CustomData.update(DataComponents.CUSTOM_DATA, upgrade, tag -> {
			if (disabled.isEmpty()) {
				tag.remove(DISABLED_FLUID_OUTPUTS_TAG);
				return;
			}
			ListTag saved = new ListTag();
			for (FluidStack stack : disabled) {
				saved.add(stack.copyWithAmount(1).saveOptional(lookupProvider));
			}
			tag.put(DISABLED_FLUID_OUTPUTS_TAG, saved);
		});
	}

	private static void writeDisabledSpawnerOutputGroups(ItemStack upgrade, Set<String> disabled) {
		CustomData.update(DataComponents.CUSTOM_DATA, upgrade, tag -> {
			if (disabled.isEmpty()) {
				tag.remove(DISABLED_SPAWNER_GROUPS_TAG);
				return;
			}
			ListTag saved = new ListTag();
			for (String group : disabled) {
				saved.add(StringTag.valueOf(group));
			}
			tag.put(DISABLED_SPAWNER_GROUPS_TAG, saved);
		});
	}

	private static boolean isFlagEnabled(ItemStack upgrade, String disabledTag) {
		return !upgrade.is(BlackboxModItems.OUTPUT_FILTER_UPGRADE.get())
				|| !upgrade.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean(disabledTag);
	}

	private static void setFlagEnabled(ItemStack upgrade, String disabledTag, boolean enabled) {
		if (!upgrade.is(BlackboxModItems.OUTPUT_FILTER_UPGRADE.get())) {
			return;
		}
		CustomData.update(DataComponents.CUSTOM_DATA, upgrade, tag -> {
			if (enabled) {
				tag.remove(disabledTag);
			} else {
			tag.putBoolean(disabledTag, true);
			}
		});
	}
}
