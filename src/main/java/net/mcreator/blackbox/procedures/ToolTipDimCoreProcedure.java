package net.mcreator.blackbox.procedures;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import net.mcreator.blackbox.init.BlackboxModItems;
import net.mcreator.blackbox.util.FarmCoreData;

import java.util.List;
import java.util.Locale;

@EventBusSubscriber(value = Dist.CLIENT)
public class ToolTipDimCoreProcedure {
	@OnlyIn(Dist.CLIENT)
	@SubscribeEvent
	public static void onItemTooltip(ItemTooltipEvent event) {
		if (!event.getItemStack().is(BlackboxModItems.DIMENSION_CORE.get())) {
			return;
		}
		addTooltip(event.getItemStack(), event.getToolTip(), event.getContext().registries());
	}

	public static void execute(ItemStack itemstack, List<Component> tooltip) {
		// Kept for compatibility with the existing MCreator procedure hook.
	}

	private static void addTooltip(ItemStack core, List<Component> tooltip, HolderLookup.Provider lookupProvider) {
		String farmName = FarmCoreData.getFarmName(core);
		if (!farmName.isEmpty()) {
			tooltip.add(Component.translatable("tooltip.blackbox.dimension_core.name", farmName).withStyle(ChatFormatting.AQUA));
		}
		FarmCoreData.getCoreId(core).ifPresent(id -> tooltip.add(Component.translatable("tooltip.blackbox.dimension_core.id", id.toString().substring(0, 8)).withStyle(ChatFormatting.DARK_GRAY)));
		tooltip.add(Component.translatable("tooltip.blackbox.dimension_core.environment." + FarmCoreData.getEnvironment(core).id()).withStyle(ChatFormatting.GRAY));
		int size = FarmCoreData.getCellSizeChunks(core);
		tooltip.add(Component.translatable("tooltip.blackbox.dimension_core.cell_size", size, size).withStyle(ChatFormatting.AQUA));
		if (!FarmCoreData.getOwnerName(core).isBlank()) {
			tooltip.add(Component.translatable("tooltip.blackbox.dimension_core.owner", FarmCoreData.getOwnerName(core)).withStyle(ChatFormatting.DARK_GRAY));
		}
		tooltip.add(Component.translatable("tooltip.blackbox.dimension_core.access", Component.translatable("gui.blackbox.access." + FarmCoreData.getAccessMode(core).id())).withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable(FarmCoreData.isMobSpawningEnabled(core) ? "tooltip.blackbox.dimension_core.mob_spawning.enabled" : "tooltip.blackbox.dimension_core.mob_spawning.disabled")
				.withStyle(FarmCoreData.isMobSpawningEnabled(core) ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
		FarmCoreData.Recipe recipe = FarmCoreData.read(core, lookupProvider);
		if (!recipe.isValid()) {
			tooltip.add(Component.translatable("tooltip.blackbox.dimension_core.empty").withStyle(ChatFormatting.GRAY));
			return;
		}

		tooltip.add(Component.translatable("tooltip.blackbox.dimension_core.summary", recipe.inputs().size(), recipe.outputs().size(), recipe.fluidInputs().size(), recipe.fluidOutputs().size()).withStyle(ChatFormatting.GREEN));
		if (!Screen.hasShiftDown()) {
			tooltip.add(Component.translatable("tooltip.blackbox.dimension_core.hold_shift").withStyle(ChatFormatting.DARK_GRAY));
			return;
		}

		tooltip.add(Component.translatable("tooltip.blackbox.dimension_core.inputs_per_minute").withStyle(ChatFormatting.YELLOW));
		if (recipe.inputs().isEmpty()) {
			tooltip.add(Component.translatable("tooltip.blackbox.dimension_core.none").withStyle(ChatFormatting.GRAY));
		} else {
			addEntries(tooltip, recipe.inputs(), recipe.sampleTicks());
		}
		tooltip.add(Component.translatable("tooltip.blackbox.dimension_core.outputs_per_minute").withStyle(ChatFormatting.AQUA));
		if (recipe.outputs().isEmpty()) {
			tooltip.add(Component.translatable("tooltip.blackbox.dimension_core.none_output").withStyle(ChatFormatting.GRAY));
		} else {
			addEntries(tooltip, recipe.outputs(), recipe.sampleTicks());
		}
		if (!recipe.fluidInputs().isEmpty()) {
			tooltip.add(Component.translatable("tooltip.blackbox.dimension_core.fluid_inputs_per_minute").withStyle(ChatFormatting.YELLOW));
			addFluidEntries(tooltip, recipe.fluidInputs(), recipe.sampleTicks());
		}
		if (!recipe.fluidOutputs().isEmpty()) {
			tooltip.add(Component.translatable("tooltip.blackbox.dimension_core.fluid_outputs_per_minute").withStyle(ChatFormatting.AQUA));
			addFluidEntries(tooltip, recipe.fluidOutputs(), recipe.sampleTicks());
		}
		if (recipe.energyInput() > 0 || recipe.energyOutput() > 0) {
			tooltip.add(Component.translatable("tooltip.blackbox.dimension_core.energy_per_minute",
					formatRate(recipe.energyInput(), recipe.sampleTicks()), formatRate(recipe.energyOutput(), recipe.sampleTicks())).withStyle(ChatFormatting.GOLD));
		}
	}

	private static void addEntries(List<Component> tooltip, List<FarmCoreData.StackAmount> entries, int sampleTicks) {
		for (FarmCoreData.StackAmount entry : entries) {
			String amount = formatRate(entry.amount(), sampleTicks);
			tooltip.add(Component.translatable("tooltip.blackbox.dimension_core.entry", amount, entry.stack().getHoverName()).withStyle(ChatFormatting.GRAY));
		}
	}

	private static void addFluidEntries(List<Component> tooltip, List<FarmCoreData.FluidAmount> entries, int sampleTicks) {
		for (FarmCoreData.FluidAmount entry : entries) {
			tooltip.add(Component.translatable("tooltip.blackbox.dimension_core.fluid_entry", formatRate(entry.amount(), sampleTicks), entry.stack().getHoverName()).withStyle(ChatFormatting.GRAY));
		}
	}

	private static String formatRate(long measuredAmount, int sampleTicks) {
		double perMinute = measuredAmount * 1200.0D / sampleTicks;
		return Math.abs(perMinute - Math.rint(perMinute)) < 0.0001D ? Long.toString(Math.round(perMinute)) : String.format(Locale.ROOT, "%.2f", perMinute);
	}
}
