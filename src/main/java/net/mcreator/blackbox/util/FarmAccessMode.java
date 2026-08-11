package net.mcreator.blackbox.util;

public enum FarmAccessMode {
	PRIVATE("private"),
	TEAM("team"),
	PUBLIC("public");

	private final String id;

	FarmAccessMode(String id) {
		this.id = id;
	}

	public String id() {
		return this.id;
	}

	public FarmAccessMode next(boolean allowPublic) {
		return switch (this) {
			case PRIVATE -> TEAM;
			case TEAM -> allowPublic ? PUBLIC : PRIVATE;
			case PUBLIC -> PRIVATE;
		};
	}

	public static FarmAccessMode fromId(String id) {
		for (FarmAccessMode mode : values()) {
			if (mode.id.equals(id)) {
				return mode;
			}
		}
		return PRIVATE;
	}
}
