package net.mcreator.blackbox.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import net.mcreator.blackbox.init.BlackboxModItems;
import net.mcreator.blackbox.init.BlackboxModScreens;
import net.mcreator.blackbox.util.FarmCoreData;
import net.mcreator.blackbox.world.inventory.BlackBoxGuiMenu;

public class BlackBoxGuiScreen extends AbstractContainerScreen<BlackBoxGuiMenu> implements BlackboxModScreens.ScreenAccessor {
	public BlackBoxGuiScreen(BlackBoxGuiMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		this.imageWidth = 176;
		this.imageHeight = 223;
	}

	@Override
	public void updateMenuState(int elementType, String name, Object elementState) {
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		this.renderTooltip(graphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		drawPanel(graphics);
		drawSlot(graphics, this.leftPos + 8, this.topPos + 20, 0xFF77CC88);
		drawSlot(graphics, this.leftPos + 152, this.topPos + 20, 0xFFD5B85A);
		for (int column = 0; column < 9; column++) {
			drawSlot(graphics, this.leftPos + 8 + column * 18, this.topPos + 49, 0xFF5E8FB8);
		}
		for (int row = 0; row < 2; row++) {
			for (int column = 0; column < 9; column++) {
				drawSlot(graphics, this.leftPos + 8 + column * 18, this.topPos + 78 + row * 18, 0xFF77A96E);
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
		graphics.drawString(this.font, Component.translatable("gui.blackbox.machine.title"), 8, 6, 0xFFE8EDF2, false);
		graphics.drawString(this.font, Component.translatable("gui.blackbox.common.core"), 29, 24, 0xFFAAB4C0, false);
		graphics.drawString(this.font, Component.translatable("gui.blackbox.machine.upgrade_short"), 119, 24, 0xFFD8C37A, false);
		graphics.drawString(this.font, Component.translatable("gui.blackbox.machine.inputs"), 8, 39, 0xFF83BCE6, false);
		graphics.drawString(this.font, Component.translatable("gui.blackbox.machine.outputs"), 8, 68, 0xFF8DDB9B, false);
		graphics.drawString(this.font, Component.translatable("container.inventory"), 8, 130, 0xFFAAB4C0, false);

		ItemStack core = this.menu.getSlot(0).getItem();
		Component status;
		int color;
		if (!core.is(BlackboxModItems.DIMENSION_CORE.get())) {
			status = Component.translatable("gui.blackbox.machine.missing_core");
			color = 0xFFFF7373;
		} else if (!FarmCoreData.isProgrammed(core)) {
			status = Component.translatable("gui.blackbox.machine.empty_core");
			color = 0xFFFFB85C;
		} else if (this.menu.getSlot(28).getItem().is(BlackboxModItems.STABILITY_UPGRADE.get())) {
			status = Component.translatable("gui.blackbox.machine.stable");
			color = 0xFF73C7FF;
		} else {
			status = Component.translatable("gui.blackbox.machine.timeline");
			color = 0xFF6EDC8C;
		}
		graphics.drawString(this.font, status, 83 - this.font.width(status) / 2, 118, color, false);
	}

	private void drawPanel(GuiGraphics graphics) {
		graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFF171B21);
		graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + 1, 0xFF596572);
		graphics.fill(this.leftPos, this.topPos + this.imageHeight - 1, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFF080A0D);
		graphics.fill(this.leftPos, this.topPos, this.leftPos + 1, this.topPos + this.imageHeight, 0xFF596572);
		graphics.fill(this.leftPos + this.imageWidth - 1, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFF080A0D);
	}

	private void drawSlot(GuiGraphics graphics, int x, int y, int borderColor) {
		graphics.fill(x - 1, y - 1, x + 17, y + 17, borderColor);
		graphics.fill(x, y, x + 16, y + 16, 0xFF0C0F13);
	}
}
