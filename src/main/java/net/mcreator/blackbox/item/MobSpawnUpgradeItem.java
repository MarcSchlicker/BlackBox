package net.mcreator.blackbox.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class MobSpawnUpgradeItem extends Item {
	public MobSpawnUpgradeItem() {
		super(new Item.Properties().stacksTo(16));
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable("tooltip.blackbox.upgrade.category.core").withStyle(ChatFormatting.GOLD));
		tooltip.add(Component.translatable("tooltip.blackbox.upgrade.mob_spawn").withStyle(ChatFormatting.GREEN));
		tooltip.add(Component.translatable("tooltip.blackbox.upgrade.consumed").withStyle(ChatFormatting.DARK_GRAY));
	}
}
