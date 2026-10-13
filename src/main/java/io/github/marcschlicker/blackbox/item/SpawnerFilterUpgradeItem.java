package io.github.marcschlicker.blackbox.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** A physical module installed in the Spawner Blackbox, never in a Dimension Core. */
public class SpawnerFilterUpgradeItem extends Item {
	public enum Kind {
		ARMOR("armor"),
		GEM("gem"),
		DROP("drop");

		private final String id;

		Kind(String id) {
			this.id = id;
		}

		public String id() {
			return this.id;
		}
	}

	private final Kind kind;

	public SpawnerFilterUpgradeItem(Kind kind) {
		super(new Item.Properties().stacksTo(1));
		this.kind = kind;
	}

	public Kind kind() {
		return this.kind;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable("tooltip.blackbox.spawner_filter." + this.kind.id()).withStyle(ChatFormatting.AQUA));
	}
}
