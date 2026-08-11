package net.mcreator.blackbox.client.gui;

import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import net.mcreator.blackbox.init.BlackboxModScreens;
import net.mcreator.blackbox.network.EmeraldBedrockGUIButtonMessage;
import net.mcreator.blackbox.world.inventory.EmeraldBedrockGUIMenu;

public class EmeraldBedrockGUIScreen extends AbstractContainerScreen<EmeraldBedrockGUIMenu> implements BlackboxModScreens.ScreenAccessor {
	private Button leaveButton;

	public EmeraldBedrockGUIScreen(EmeraldBedrockGUIMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		this.imageWidth = 196;
		this.imageHeight = 108;
	}

	@Override
	public void updateMenuState(int elementType, String name, Object elementState) {
	}

	@Override
	protected void init() {
		super.init();
		this.leaveButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.blackbox.farm_exit.confirm"), button -> {
			PacketDistributor.sendToServer(new EmeraldBedrockGUIButtonMessage(0, this.menu.x, this.menu.y, this.menu.z));
			this.minecraft.player.closeContainer();
		}).bounds(this.leftPos + 24, this.topPos + 70, 148, 22).build());
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		this.renderTooltip(graphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFF171B21);
		graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + 2, 0xFF5DBA79);
		graphics.fill(this.leftPos, this.topPos, this.leftPos + 1, this.topPos + this.imageHeight, 0xFF596572);
		graphics.fill(this.leftPos + this.imageWidth - 1, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFF080A0D);
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		graphics.drawCenteredString(this.font, Component.translatable("gui.blackbox.farm_exit.title"), this.imageWidth / 2, 13, 0xFFE8EDF2);
		graphics.drawWordWrap(this.font, Component.translatable("gui.blackbox.farm_exit.description"), 16, 34, this.imageWidth - 32, 0xFFBBC5CE);
	}
}
