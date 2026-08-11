package net.mcreator.blackbox.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import net.mcreator.blackbox.util.FarmEnvironment;

import java.util.List;

public class CoreEnvironmentUpgradeItem extends Item {
	private final FarmEnvironment environment;

	public CoreEnvironmentUpgradeItem(FarmEnvironment environment) {
		super(new Item.Properties().stacksTo(16));
		this.environment = environment;
	}

	public FarmEnvironment environment() {
		return this.environment;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable("tooltip.blackbox.upgrade.category.core").withStyle(ChatFormatting.GOLD));
		tooltip.add(Component.translatable("tooltip.blackbox.upgrade.environment." + this.environment.id()).withStyle(ChatFormatting.AQUA));
		tooltip.add(Component.translatable("tooltip.blackbox.upgrade.consumed").withStyle(ChatFormatting.DARK_GRAY));
	}
}
