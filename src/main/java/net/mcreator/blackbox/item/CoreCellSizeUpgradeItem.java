package net.mcreator.blackbox.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class CoreCellSizeUpgradeItem extends Item {
	private final int sizeChunks;

	public CoreCellSizeUpgradeItem(int sizeChunks) {
		super(new Item.Properties().stacksTo(16));
		this.sizeChunks = Math.max(1, Math.min(3, sizeChunks));
	}

	public int sizeChunks() {
		return this.sizeChunks;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable("tooltip.blackbox.upgrade.category.core").withStyle(ChatFormatting.GOLD));
		tooltip.add(Component.translatable("tooltip.blackbox.upgrade.cell_size", this.sizeChunks, this.sizeChunks).withStyle(ChatFormatting.AQUA));
		tooltip.add(Component.translatable("tooltip.blackbox.upgrade.before_first_entry").withStyle(ChatFormatting.DARK_GRAY));
	}
}
