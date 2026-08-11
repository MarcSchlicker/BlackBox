package net.mcreator.blackbox.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;

import net.mcreator.blackbox.client.BlueprintLibraryClient;
import net.mcreator.blackbox.item.BlueprintItem.StorageScope;

public final class BlueprintRenameScreen extends Screen {
	private final String blueprintId;
	private final String currentName;
	private final StorageScope scope;
	private final InteractionHand hand;
	private EditBox nameField;

	public BlueprintRenameScreen(String blueprintId, String currentName, StorageScope scope, InteractionHand hand) {
		super(Component.translatable("gui.blackbox.blueprint_rename.title"));
		this.blueprintId = blueprintId;
		this.currentName = currentName;
		this.scope = scope;
		this.hand = hand;
	}

	@Override
	protected void init() {
		int left = this.width / 2 - 120;
		int top = this.height / 2 - 48;
		this.nameField = new EditBox(this.font, left, top + 28, 240, 20, Component.translatable("gui.blackbox.blueprint_name.field"));
		this.nameField.setMaxLength(48);
		this.nameField.setValue(this.currentName);
		this.nameField.setFocused(true);
		this.addRenderableWidget(this.nameField);
		this.addRenderableWidget(Button.builder(Component.translatable("gui.blackbox.blueprint_rename.save"), button -> rename())
				.bounds(left, top + 55, 116, 20).build());
		this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
				.bounds(left + 124, top + 55, 116, 20).build());
	}

	private void rename() {
		String name = this.nameField.getValue().trim();
		if (name.isEmpty()) {
			return;
		}
		BlueprintLibraryClient.rename(this.blueprintId, this.currentName, name, this.scope, this.hand);
		onClose();
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == 257 || keyCode == 335) {
			rename();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		graphics.fill(0, 0, this.width, this.height, 0xCC0A0D10);
		int left = this.width / 2 - 130;
		int top = this.height / 2 - 58;
		graphics.fill(left, top, left + 260, top + 96, 0xFF20262B);
		graphics.fill(left, top, left + 260, top + 2, 0xFF4A9A6B);
		graphics.drawCenteredString(this.font, this.title, this.width / 2, top + 10, 0xFFF0F3F1);
		super.render(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
