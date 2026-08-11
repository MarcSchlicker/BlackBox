package net.mcreator.blackbox.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import net.mcreator.blackbox.world.inventory.AdminBookMenu;
import net.mcreator.blackbox.network.AdminFarmRegistryMessage;
import net.neoforged.neoforge.network.PacketDistributor;

public class AdminBookScreen extends AbstractContainerScreen<AdminBookMenu> {
	public AdminBookScreen(AdminBookMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		this.imageWidth = 176;
		this.imageHeight = 170;
	}

	@Override
	protected void init() {
		super.init();
		this.addRenderableWidget(Button.builder(Component.translatable("gui.blackbox.admin.farms_short"), button ->
				PacketDistributor.sendToServer(AdminFarmRegistryMessage.request())).bounds(this.leftPos + 112, this.topPos + 5, 56, 18).build());
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		this.renderTooltip(graphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFF202329);
		graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + 1, 0xFF8E9AA8);
		for (int row = 0; row < 2; row++) {
			for (int column = 0; column < 9; column++) {
				drawSlot(graphics, this.leftPos + 8 + column * 18, this.topPos + 33 + row * 18, 0xFFB77979);
			}
		}
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				drawSlot(graphics, this.leftPos + 8 + column * 18, this.topPos + 88 + row * 18, 0xFF697582);
			}
		}
		for (int column = 0; column < 9; column++) {
			drawSlot(graphics, this.leftPos + 8 + column * 18, this.topPos + 146, 0xFF697582);
		}
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		graphics.drawString(this.font, Component.translatable("gui.blackbox.admin.title"), 8, 7, 0xFFF1F3F5, false);
		graphics.drawString(this.font, Component.translatable("gui.blackbox.admin.hint"), 8, 20, 0xFFC9CFD6, false);
		graphics.drawString(this.font, Component.translatable("container.inventory"), 8, 77, 0xFFC9CFD6, false);
	}

	private void drawSlot(GuiGraphics graphics, int x, int y, int borderColor) {
		graphics.fill(x - 1, y - 1, x + 17, y + 17, borderColor);
		graphics.fill(x, y, x + 16, y + 16, 0xFF0C0F13);
	}
}
