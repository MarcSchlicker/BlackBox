package io.github.marcschlicker.blackbox.util;

public enum BlackboxSideMode {
	ITEM_INPUT("item_input"),
	ITEM_OUTPUT("item_output"),
	FLUID_INPUT("fluid_input"),
	FLUID_OUTPUT("fluid_output"),
	ENERGY_INPUT("energy_input"),
	ENERGY_OUTPUT("energy_output"),
	DISABLED("disabled"),
	ITEM_BOTH("item_both"),
	FLUID_BOTH("fluid_both"),
	ENERGY_BOTH("energy_both");

	private final String id;

	BlackboxSideMode(String id) {
		this.id = id;
	}

	public String id() {
		return this.id;
	}

	public BlackboxSideMode next() {
		return switch (this) {
			case DISABLED -> ITEM_INPUT;
			case ITEM_INPUT -> ITEM_OUTPUT;
			case ITEM_OUTPUT -> ITEM_BOTH;
			case ITEM_BOTH -> FLUID_INPUT;
			case FLUID_INPUT -> FLUID_OUTPUT;
			case FLUID_OUTPUT -> FLUID_BOTH;
			case FLUID_BOTH -> ENERGY_INPUT;
			case ENERGY_INPUT -> ENERGY_OUTPUT;
			case ENERGY_OUTPUT -> ENERGY_BOTH;
			case ENERGY_BOTH -> DISABLED;
		};
	}
}
