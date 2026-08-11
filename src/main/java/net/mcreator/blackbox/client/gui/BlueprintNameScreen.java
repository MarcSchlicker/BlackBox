package net.mcreator.blackbox.client.gui;

import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;

import net.mcreator.blackbox.item.BlueprintItem.StorageScope;
import net.mcreator.blackbox.network.BlueprintSaveRequestMessage;

public class BlueprintNameScreen extends Screen {
	private static final int PANEL_WIDTH = 260;
	private static final int PANEL_HEIGHT = 116;
	private final InteractionHand hand;
	private final StorageScope storageScope;
	private EditBox nameField;
	private Button saveButton;

	public BlueprintNameScreen(InteractionHand hand, StorageScope storageScope) {
		super(Component.translatable("gui.blackbox.blueprint_name.title"));
		this.hand = hand;
		this.storageScope = storageScope;
	}

	@Override
	protected void init() {
		int left = (this.width - PANEL_WIDTH) / 2;
		int top = (this.height - PANEL_HEIGHT) / 2;
		this.nameField = new EditBox(this.font, left + 20, top + 42, 220, 20, Component.translatable("gui.blackbox.blueprint_name.field"));
		this.nameField.setHint(Component.translatable("gui.blackbox.blueprint_name.field"));
		this.nameField.setMaxLength(48);
		this.nameField.setResponder(value -> this.saveButton.active = !value.trim().isEmpty());
		this.addRenderableWidget(this.nameField);
		this.saveButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.blackbox.blueprint_name.save"), ignored -> save())
				.bounds(left + 20, top + 76, 106, 20).build());
		this.saveButton.active = false;
		this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), ignored -> onClose()).bounds(left + 134, top + 76, 106, 20).build());
		this.setInitialFocus(this.nameField);
	}

	private void save() {
		String name = this.nameField.getValue().trim();
		if (name.isEmpty()) {
			return;
		}
		PacketDistributor.sendToServer(new BlueprintSaveRequestMessage(this.hand, name));
		onClose();
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if ((keyCode == 257 || keyCode == 335) && this.saveButton.active) {
			save();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		int left = (this.width - PANEL_WIDTH) / 2;
		int top = (this.height - PANEL_HEIGHT) / 2;
		graphics.fill(0, 0, this.width, this.height, 0xCC0A0D10);
		graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xFF20262B);
		graphics.fill(left, top, left + PANEL_WIDTH, top + 2, 0xFF4A9A6B);
		graphics.drawCenteredString(this.font, this.title, this.width / 2, top + 10, 0xFFF0F3F1);
		graphics.drawCenteredString(this.font, Component.translatable("gui.blackbox.blueprint_name.storage." + this.storageScope.id()), this.width / 2, top + 25, 0xFFAFC6B8);
		super.render(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
