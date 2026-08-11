package net.mcreator.blackbox.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class StabilityUpgradeItem extends Item {
	public StabilityUpgradeItem() {
		super(new Item.Properties().stacksTo(1));
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable("tooltip.blackbox.upgrade.category.machine").withStyle(ChatFormatting.BLUE));
		tooltip.add(Component.translatable("tooltip.blackbox.upgrade.stability").withStyle(ChatFormatting.AQUA));
		tooltip.add(Component.translatable("tooltip.blackbox.upgrade.reusable").withStyle(ChatFormatting.DARK_GRAY));
	}
}
