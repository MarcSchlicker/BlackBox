package net.mcreator.blackbox.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import net.mcreator.blackbox.init.BlackboxModScreens;
import net.mcreator.blackbox.world.inventory.OutputBlockGUIMenu;

public class OutputBlockGUIScreen extends AbstractContainerScreen<OutputBlockGUIMenu> implements BlackboxModScreens.ScreenAccessor {
	public OutputBlockGUIScreen(OutputBlockGUIMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		this.imageWidth = 176;
		this.imageHeight = 166;
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
		graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFF172019);
		graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + 2, 0xFF55A56A);
		for (int row = 0; row < 2; row++) {
			for (int column = 0; column < 5; column++) {
				drawSlot(graphics, this.leftPos + 44 + column * 18, this.topPos + 27 + row * 18, 0xFF6FC784);
			}
		}
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				drawSlot(graphics, this.leftPos + 8 + column * 18, this.topPos + 84 + row * 18, 0xFF68727D);
			}
		}
		for (int column = 0; column < 9; column++) {
			drawSlot(graphics, this.leftPos + 8 + column * 18, this.topPos + 142, 0xFF68727D);
		}
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		graphics.drawString(this.font, Component.translatable("gui.blackbox.output.title"), 8, 7, 0xFFE8EDF2, false);
		graphics.drawCenteredString(this.font, Component.translatable("gui.blackbox.output.manual"), this.imageWidth / 2, 66, 0xFF91C99C);
		graphics.drawString(this.font, Component.translatable("container.inventory"), 8, 74, 0xFFAAB4C0, false);
	}

	private void drawSlot(GuiGraphics graphics, int x, int y, int borderColor) {
		graphics.fill(x - 1, y - 1, x + 17, y + 17, borderColor);
		graphics.fill(x, y, x + 16, y + 16, 0xFF0C0F13);
	}
}
