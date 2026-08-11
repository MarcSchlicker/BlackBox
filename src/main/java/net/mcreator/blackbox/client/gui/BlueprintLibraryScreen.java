package net.mcreator.blackbox.client.gui;

import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;

import net.mcreator.blackbox.client.BlueprintLibraryClient;
import net.mcreator.blackbox.item.BlueprintItem.StorageScope;
import net.mcreator.blackbox.network.BlueprintLibraryMessage;
import net.mcreator.blackbox.util.BlueprintLibrary.BlueprintSummary;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BlueprintLibraryScreen extends Screen {
	private static final int PANEL_WIDTH = 280;
	private static final int PANEL_HEIGHT = 260;
	private static final int ROWS_PER_PAGE = 4;
	private final List<BlueprintSummary> serverEntries;
	private final List<BlueprintSummary> localEntries;
	private final String selectedId;
	private final StorageScope selectedScope;
	private final InteractionHand hand;
	private final List<Button> entryButtons = new ArrayList<>();
	private EditBox search;
	private Button localTab;
	private Button serverTab;
	private Button actionButton;
	private Button transferButton;
	private Button renameButton;
	private Button deleteButton;
	private Button previousButton;
	private Button nextButton;
	private StorageScope visibleScope;
	private int page;
	private boolean confirmDelete;

	public BlueprintLibraryScreen(List<BlueprintSummary> serverEntries, List<BlueprintSummary> localEntries, String selectedId,
			StorageScope selectedScope, StorageScope preferredStorage, InteractionHand hand) {
		super(Component.translatable("gui.blackbox.blueprint_library.title"));
		this.serverEntries = List.copyOf(serverEntries);
		this.localEntries = List.copyOf(localEntries);
		this.selectedId = selectedId;
		this.selectedScope = selectedScope;
		this.visibleScope = selectedId.isBlank() ? preferredStorage : selectedScope;
		this.hand = hand;
	}

	@Override
	protected void init() {
		int left = (this.width - PANEL_WIDTH) / 2;
		int top = (this.height - PANEL_HEIGHT) / 2;
		this.localTab = this.addRenderableWidget(Button.builder(Component.translatable("gui.blackbox.blueprint_library.local"), button -> switchScope(StorageScope.LOCAL))
				.bounds(left + 20, top + 27, 119, 19).build());
		this.serverTab = this.addRenderableWidget(Button.builder(Component.translatable("gui.blackbox.blueprint_library.server"), button -> switchScope(StorageScope.SERVER))
				.bounds(left + 141, top + 27, 119, 19).build());
		this.search = new EditBox(this.font, left + 20, top + 50, 240, 19, Component.translatable("gui.blackbox.blueprint_library.search"));
		this.search.setHint(Component.translatable("gui.blackbox.blueprint_library.search"));
		this.search.setMaxLength(48);
		this.search.setResponder(value -> {
			this.page = 0;
			rebuildEntries();
		});
		this.addRenderableWidget(this.search);
		this.previousButton = this.addRenderableWidget(Button.builder(Component.literal("<"), button -> changePage(-1)).bounds(left + 20, top + 234, 30, 18).build());
		this.nextButton = this.addRenderableWidget(Button.builder(Component.literal(">"), button -> changePage(1)).bounds(left + 230, top + 234, 30, 18).build());
		this.addRenderableWidget(Button.builder(Component.literal("X"), button -> onClose()).bounds(left + 255, top + 5, 18, 18).build());
		rebuildEntries();
	}

	private void switchScope(StorageScope scope) {
		this.visibleScope = scope;
		this.page = 0;
		rebuildEntries();
	}

	private void changePage(int offset) {
		this.page = Math.max(0, Math.min(maxPage(), this.page + offset));
		rebuildEntries();
	}

	private void rebuildEntries() {
		for (Button button : this.entryButtons) {
			this.removeWidget(button);
		}
		this.entryButtons.clear();
		if (this.actionButton != null) {
			this.removeWidget(this.actionButton);
			this.actionButton = null;
		}
		if (this.transferButton != null) {
			this.removeWidget(this.transferButton);
			this.transferButton = null;
		}
		if (this.renameButton != null) {
			this.removeWidget(this.renameButton);
			this.renameButton = null;
		}
		if (this.deleteButton != null) {
			this.removeWidget(this.deleteButton);
			this.deleteButton = null;
		}
		List<BlueprintSummary> filtered = filteredEntries();
		this.page = Math.min(this.page, Math.max(0, (filtered.size() - 1) / ROWS_PER_PAGE));
		int left = (this.width - PANEL_WIDTH) / 2;
		int top = (this.height - PANEL_HEIGHT) / 2;
		int first = this.page * ROWS_PER_PAGE;
		for (int row = 0; row < ROWS_PER_PAGE && first + row < filtered.size(); row++) {
			BlueprintSummary entry = filtered.get(first + row);
			String marker = entry.id().equals(this.selectedId) && this.visibleScope == this.selectedScope ? "\u2713 " : "";
			Component label = Component.literal(marker + entry.name() + "  (v" + entry.revision() + ", " + entry.author() + ", " + entry.blockCount() + ")");
			Button button = Button.builder(label, ignored -> select(entry)).bounds(left + 20, top + 73 + row * 21, 240, 19).build();
			this.entryButtons.add(this.addRenderableWidget(button));
		}
		this.actionButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.blackbox.blueprint_library.create." + this.visibleScope.id()), ignored -> createNew())
				.bounds(left + 30, top + 160, 220, 18).build());
		if (!this.selectedId.isBlank() && this.visibleScope == this.selectedScope) {
			BlueprintSummary selected = filtered.stream().filter(entry -> entry.id().equals(this.selectedId)).findFirst().orElse(null);
			if (selected != null) {
				this.renameButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.blackbox.blueprint_library.rename"), ignored -> renameSelected(selected))
						.bounds(left + 30, top + 181, 107, 18).build());
				this.deleteButton = this.addRenderableWidget(Button.builder(Component.translatable(confirmDelete ? "gui.blackbox.blueprint_library.confirm_delete" : "gui.blackbox.blueprint_library.delete"),
						ignored -> deleteSelected(selected)).bounds(left + 143, top + 181, 107, 18).build());
			}
			String key = this.visibleScope == StorageScope.LOCAL ? "gui.blackbox.blueprint_library.publish" : "gui.blackbox.blueprint_library.download";
			this.transferButton = this.addRenderableWidget(Button.builder(Component.translatable(key), ignored -> transferSelected()).bounds(left + 30, top + 202, 220, 18).build());
		}
		this.localTab.active = this.visibleScope != StorageScope.LOCAL;
		this.serverTab.active = this.visibleScope != StorageScope.SERVER;
		this.previousButton.active = this.page > 0;
		this.nextButton.active = this.page < maxPage();
	}

	private void select(BlueprintSummary entry) {
		BlueprintLibraryClient.select(entry.id(), entry.name(), this.visibleScope, this.hand);
		onClose();
	}

	private void createNew() {
		BlueprintLibraryClient.createNew(this.visibleScope, this.hand);
	}

	private void transferSelected() {
		if (this.visibleScope == StorageScope.LOCAL) {
			BlueprintLibraryClient.publishLocal(this.selectedId, this.hand);
		} else {
			PacketDistributor.sendToServer(BlueprintLibraryMessage.download(this.hand, this.selectedId));
		}
		onClose();
	}

	private void renameSelected(BlueprintSummary selected) {
		if (this.minecraft != null) {
			this.minecraft.setScreen(new BlueprintRenameScreen(selected.id(), selected.name(), this.visibleScope, this.hand));
		}
	}

	private void deleteSelected(BlueprintSummary selected) {
		if (!this.confirmDelete) {
			this.confirmDelete = true;
			rebuildEntries();
			return;
		}
		BlueprintLibraryClient.delete(selected.id(), selected.name(), this.visibleScope, this.hand);
		onClose();
	}

	private List<BlueprintSummary> filteredEntries() {
		String query = this.search == null ? "" : this.search.getValue().trim().toLowerCase(Locale.ROOT);
		List<BlueprintSummary> source = this.visibleScope == StorageScope.LOCAL ? this.localEntries : this.serverEntries;
		if (query.isEmpty()) {
			return source;
		}
		return source.stream().filter(entry -> entry.name().toLowerCase(Locale.ROOT).contains(query)
				|| entry.author().toLowerCase(Locale.ROOT).contains(query)).toList();
	}

	private int maxPage() {
		return Math.max(0, (filteredEntries().size() - 1) / ROWS_PER_PAGE);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		int left = (this.width - PANEL_WIDTH) / 2;
		int top = (this.height - PANEL_HEIGHT) / 2;
		graphics.fill(0, 0, this.width, this.height, 0xCC0A0D10);
		graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xFF20262B);
		graphics.fill(left, top, left + PANEL_WIDTH, top + 2, 0xFF4A9A6B);
		graphics.drawCenteredString(this.font, this.title, this.width / 2, top + 8, 0xFFF0F3F1);
		if (filteredEntries().isEmpty()) {
			graphics.drawCenteredString(this.font, Component.translatable("gui.blackbox.blueprint_library.empty." + this.visibleScope.id()), this.width / 2, top + 122, 0xFFB5BEC3);
		}
		graphics.drawCenteredString(this.font, Component.literal((this.page + 1) + " / " + (maxPage() + 1)), this.width / 2, top + 239, 0xFFB5BEC3);
		super.render(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
