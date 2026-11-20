package dev.snowtracks;

import net.minecraft.resources.Identifier;

public enum Shape {
	BOOT("boot"),
	PAW("paw"),
	HOOF("hoof"),
	BIRD("bird"),
	RABBIT("rabbit"),
	DRAG("drag");

	private final String name;

	Shape(String name) {
		this.name = name;
	}

	public Identifier sprite(Surface surface) {
		return Identifier.fromNamespaceAndPath(SnowTracks.MOD_ID, name + "_" + surface.style);
	}
}
