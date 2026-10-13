package io.github.marcschlicker.blackbox.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import io.github.marcschlicker.blackbox.block.entity.SpawnerBlackboxBlockEntity;
import io.github.marcschlicker.blackbox.util.SpawnerIntegration;
import io.github.marcschlicker.blackbox.world.inventory.SpawnerBlackboxMenu;

/** Compact machine UI: purple spawner modules, red Masher upgrades, and green generated loot. */
public class SpawnerBlackboxScreen extends AbstractContainerScreen<SpawnerBlackboxMenu> {
	public SpawnerBlackboxScreen(SpawnerBlackboxMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		this.imageWidth = 176;
		this.imageHeight = 223;
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFF18151E);
		graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + 2, 0xFF9C6BCC);
		graphics.fill(this.leftPos, this.topPos, this.leftPos + 1, this.topPos + this.imageHeight, 0xFF5E506C);
		graphics.fill(this.leftPos + this.imageWidth - 1, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFF08070A);
		for (int column = 0; column < 5; column++) {
			drawSlot(graphics, this.leftPos + 8 + column * 18, this.topPos + 20, 0xFFB06EDE);
			drawSlot(graphics, this.leftPos + 8 + column * 18, this.topPos + 50, 0xFFE0786A);
		}
		for (int row = 0; row < 2; row++) {
			for (int column = 0; column < 9; column++) {
				drawSlot(graphics, this.leftPos + 8 + column * 18, this.topPos + 85 + row * 18, 0xFF77A96E);
			}
		}
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				drawSlot(graphics, this.leftPos + 8 + column * 18, this.topPos + 141 + row * 18, 0xFF68727D);
			}
		}
		for (int column = 0; column < 9; column++) {
			drawSlot(graphics, this.leftPos + 8 + column * 18, this.topPos + 199, 0xFF68727D);
		}
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		graphics.drawString(this.font, Component.translatable("gui.blackbox.spawner_blackbox.title"), 31, 6, 0xFFF1EAF7, false);
		graphics.drawString(this.font, Component.translatable("gui.blackbox.spawner_blackbox.spawners"), 8, 39, 0xFFD7B8F3, false);
		graphics.drawString(this.font, Component.translatable("gui.blackbox.spawner_blackbox.upgrades"), 8, 72, 0xFFFFAAA0, false);
		graphics.drawString(this.font, Component.translatable("gui.blackbox.spawner_blackbox.outputs"), 8, 76, 0xFF8DDB9B, false);
		graphics.drawString(this.font, Component.translatable("container.inventory"), 48, 130, 0xFFAAB4C0, false);
		Component state = status();
		graphics.drawString(this.font, this.font.plainSubstrByWidth(state.getString(), 66), 104, 55,
				state.getStyle().getColor() == null ? 0xFFAAB4C0 : state.getStyle().getColor().getValue(), false);
	}

	private Component status() {
		if (!SpawnerIntegration.isAvailable()) {
			return Component.translatable("gui.blackbox.spawner_blackbox.missing_mods").withStyle(style -> style.withColor(0xFFFF7373));
		}
		for (int slot = SpawnerBlackboxBlockEntity.SPAWNER_START; slot < SpawnerBlackboxBlockEntity.SPAWNER_END; slot++) {
			if (SpawnerIntegration.isApothicSpawner(this.menu.getSlot(slot).getItem())) {
				return Component.translatable("gui.blackbox.spawner_blackbox.running").withStyle(style -> style.withColor(0xFF86E7A0));
			}
		}
		return Component.translatable("gui.blackbox.spawner_blackbox.missing_spawner").withStyle(style -> style.withColor(0xFFFFB85C));
	}

	private static void drawSlot(GuiGraphics graphics, int x, int y, int color) {
		graphics.fill(x - 1, y - 1, x + 17, y + 17, color);
		graphics.fill(x, y, x + 16, y + 16, 0xFF0C0F13);
	}
}
