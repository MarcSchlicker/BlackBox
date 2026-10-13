package io.github.marcschlicker.blackbox.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.network.PacketDistributor;

import io.github.marcschlicker.blackbox.init.BlackboxModItems;
import io.github.marcschlicker.blackbox.network.CoreSplitterMessage;
import io.github.marcschlicker.blackbox.util.FarmCoreData;
import io.github.marcschlicker.blackbox.world.inventory.CoreSplitterMenu;

public class CoreSplitterScreen extends AbstractContainerScreen<CoreSplitterMenu> {
	private Button splitButton;

	public CoreSplitterScreen(CoreSplitterMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		this.imageWidth = 176;
		this.imageHeight = 192;
	}

	@Override
	protected void init() {
		super.init();
		this.splitButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> PacketDistributor.sendToServer(
				new CoreSplitterMessage(this.menu.isAwaitingConfirmation() ? CoreSplitterMessage.CONFIRM_SPLIT : CoreSplitterMessage.ARM_SPLIT)))
				.bounds(this.leftPos + 54, this.topPos + 74, 114, 20).build());
		this.addRenderableWidget(Button.builder(Component.translatable("gui.blackbox.core_splitter.cancel"), button -> this.onClose())
				.bounds(this.leftPos + 8, this.topPos + 74, 42, 20).build());
	}

	@Override
	public void containerTick() {
		super.containerTick();
		boolean canSplit = outputCount() > 1 && this.menu.core().is(BlackboxModItems.DIMENSION_CORE.get());
		this.splitButton.active = canSplit;
		this.splitButton.setMessage(Component.translatable(this.menu.isAwaitingConfirmation()
				? "gui.blackbox.core_splitter.confirm" : "gui.blackbox.core_splitter.prepare"));
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		this.renderTooltip(graphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFF171B21);
		graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + 1, this.menu.isAwaitingConfirmation() ? 0xFFBC4545 : 0xFF6B9DB7);
		drawSlot(graphics, this.leftPos + 80, this.topPos + 27, 0xFFB56E6E);
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				drawSlot(graphics, this.leftPos + 8 + column * 18, this.topPos + 110 + row * 18, 0xFF68727D);
			}
		}
		for (int column = 0; column < 9; column++) {
			drawSlot(graphics, this.leftPos + 8 + column * 18, this.topPos + 168, 0xFF68727D);
		}
		if (this.menu.isAwaitingConfirmation()) {
			graphics.fill(this.leftPos + 8, this.topPos + 51, this.leftPos + 168, this.topPos + 69, 0xAA742727);
		}
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		graphics.drawString(this.font, Component.translatable("gui.blackbox.core_splitter.title"), 8, 7, 0xFFE8EDF2, false);
		graphics.drawString(this.font, Component.translatable("gui.blackbox.core_splitter.core"), 8, 32, 0xFFAAB4C0, false);
		if (this.menu.isAwaitingConfirmation()) {
			graphics.drawString(this.font, Component.translatable("gui.blackbox.core_splitter.warning"), 12, 57, 0xFFFFD0D0, false);
		} else if (outputCount() > 1) {
			graphics.drawString(this.font, Component.translatable("gui.blackbox.core_splitter.outputs", outputCount()), 8, 55,
					0xFF8DDB9B, false);
		} else {
			graphics.drawString(this.font, Component.translatable("gui.blackbox.core_splitter.requirement"), 8, 55, 0xFFFFB85C, false);
		}
		graphics.drawString(this.font, Component.translatable("container.inventory"), 8, 100, 0xFFAAB4C0, false);
	}

	private int outputCount() {
		ItemStack core = this.menu.core();
		if (!core.is(BlackboxModItems.DIMENSION_CORE.get()) || Minecraft.getInstance().level == null) {
			return 0;
		}
		return FarmCoreData.read(core, Minecraft.getInstance().level.registryAccess()).outputTemplates().size();
	}

	private void drawSlot(GuiGraphics graphics, int x, int y, int borderColor) {
		graphics.fill(x - 1, y - 1, x + 17, y + 17, borderColor);
		graphics.fill(x, y, x + 16, y + 16, 0xFF0C0F13);
	}
}
