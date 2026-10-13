package io.github.marcschlicker.blackbox.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class OutputFilterUpgradeItem extends Item {
	public OutputFilterUpgradeItem() {
		super(new Item.Properties().stacksTo(1));
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable("tooltip.blackbox.upgrade.category.blackbox").withStyle(ChatFormatting.BLUE));
		tooltip.add(Component.translatable("tooltip.blackbox.upgrade.output_filter").withStyle(ChatFormatting.AQUA));
		tooltip.add(Component.translatable("tooltip.blackbox.upgrade.filter_storage").withStyle(ChatFormatting.DARK_GRAY));
	}
}
