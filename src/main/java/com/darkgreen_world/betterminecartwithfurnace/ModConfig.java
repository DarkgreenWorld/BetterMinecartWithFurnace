package com.darkgreen_world.betterminecartwithfurnace;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import net.fabricmc.loader.api.FabricLoader;

public final class ModConfig {
	private static final double DEFAULT_THRUST = 0.6;
	public static double thrust = DEFAULT_THRUST;

	private ModConfig() {
	}

	/** Most the speed delivered by the engine (blocks/tick) may rise per tick. */
	public static double acceleration() {
		return thrust / 15.0;
	}

	//Reads the config file. It is left alone when every setting is present and valid.
	public static void load() {
		Path file = FabricLoader.getInstance().getConfigDir().resolve(BetterMinecartWithFurnace.MOD_ID + ".properties");

		try {
			if (Files.exists(file)) {
				Properties properties = new Properties();

				try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
					properties.load(reader);
				}

				boolean valid = false;

				try {
					double value = Double.parseDouble(properties.getProperty("thrust", "").trim());

					if (value > 0.0 && !Double.isInfinite(value)) {
						thrust = value;
						valid = true;
					}
				} catch (NumberFormatException ignored) {
				}

				Set<String> unknown = new TreeSet<>(properties.stringPropertyNames());
				unknown.remove("thrust");

				if (valid && unknown.isEmpty()) {
					return;
				}

				if (!valid) {
					BetterMinecartWithFurnace.LOGGER.warn("{}: thrust is missing or not a positive number, resetting it to {}", file, DEFAULT_THRUST);
				}

				if (!unknown.isEmpty()) {
					BetterMinecartWithFurnace.LOGGER.warn("{}: removing unknown settings {}", file, unknown);
				}
			}

			Files.createDirectories(file.getParent());
			Files.write(file, List.of(
					"# Thrust of a minecart with furnace: speed (blocks/tick) its engine adds every tick.",
					"# Top speed in blocks/second pulling N linked minecarts: thrust * 19.5 / (0.22 + 0.02 * N).",
					"# Acceleration is thrust / 15. Restart the server after changing this.",
					"thrust=" + thrust
			), StandardCharsets.UTF_8);
		} catch (IOException e) {
			BetterMinecartWithFurnace.LOGGER.warn("Could not read or write config file {}, using the default thrust {}", file, DEFAULT_THRUST, e);
		}
	}
}
