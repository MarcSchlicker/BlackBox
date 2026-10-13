package io.github.marcschlicker.blackbox.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import io.github.marcschlicker.blackbox.util.SpawnerIntegration;

import java.util.List;

/** Dedicated key for the optional spawner simulation, intentionally separate from dimensional farm cores. */
public class SpawnerCoreItem extends Item {
	public SpawnerCoreItem() {
		super(new Item.Properties().stacksTo(1));
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable("tooltip.blackbox.spawner_core.requires").withStyle(ChatFormatting.DARK_PURPLE));
		tooltip.add(Component.translatable(SpawnerIntegration.isAvailable()
				? "tooltip.blackbox.spawner_core.ready" : "tooltip.blackbox.spawner_core.missing").withStyle(SpawnerIntegration.isAvailable() ? ChatFormatting.GREEN : ChatFormatting.RED));
	}
}
