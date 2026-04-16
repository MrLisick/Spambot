package org.Spambot.spambot.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.text.Text;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;
import java.time.LocalTime;

public class SpambotClient implements ClientModInitializer {

    private static class Config {
        boolean active = false;
        boolean nightMode = false;
        String spamMessage = "Spambot by _MrLisick_";
        int defaultInterval = 6000;
        int nightInterval = 144000;
        int nightStartHour = 0;
        int nightEndHour = 6;
    }

    private static Config config = new Config();
    private static int timer = 0;

    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("spambot.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public void onInitializeClient() {
        loadConfig();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || !config.active) return;

            if (timer <= 0) {
                client.player.networkHandler.sendChatMessage(config.spamMessage);

                int currentHour = LocalTime.now().getHour();
                boolean isNightTime;

                if (config.nightStartHour < config.nightEndHour) {
                    isNightTime = (currentHour >= config.nightStartHour && currentHour < config.nightEndHour);
                } else {
                    isNightTime = (currentHour >= config.nightStartHour || currentHour < config.nightEndHour);
                }

                if (config.nightMode && isNightTime) {
                    timer = config.nightInterval;
                } else {
                    timer = config.defaultInterval;
                }
            } else {
                timer--;
            }
        });

        registerCommands();
    }

    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("spam")
                    .then(ClientCommandManager.literal("on").executes(context -> {
                        config.active = true; timer = 0; saveConfig();
                        context.getSource().sendFeedback(Text.translatable("spambot.msg.on"));
                        return 1;
                    }))
                    .then(ClientCommandManager.literal("off").executes(context -> {
                        config.active = false; saveConfig();
                        context.getSource().sendFeedback(Text.translatable("spambot.msg.off"));
                        return 1;
                    }))
                    .then(ClientCommandManager.literal("night").executes(context -> {
                        config.nightMode = !config.nightMode; saveConfig();
                        String key = config.nightMode ? "spambot.msg.night_on" : "spambot.msg.night_off";
                        context.getSource().sendFeedback(Text.translatable(key));
                        return 1;
                    }))
                    .then(ClientCommandManager.literal("start")
                            .then(ClientCommandManager.argument("hour", IntegerArgumentType.integer(0, 23))
                                    .executes(context -> {
                                        config.nightStartHour = IntegerArgumentType.getInteger(context, "hour");
                                        saveConfig();
                                        context.getSource().sendFeedback(Text.translatable("spambot.msg.start_set", config.nightStartHour));
                                        return 1;
                                    })))
                    .then(ClientCommandManager.literal("end")
                            .then(ClientCommandManager.argument("hour", IntegerArgumentType.integer(0, 23))
                                    .executes(context -> {
                                        config.nightEndHour = IntegerArgumentType.getInteger(context, "hour");
                                        saveConfig();
                                        context.getSource().sendFeedback(Text.translatable("spambot.msg.end_set", config.nightEndHour));
                                        return 1;
                                    })))
                    .then(ClientCommandManager.literal("time")
                            .then(ClientCommandManager.argument("ticks", IntegerArgumentType.integer(1))
                                    .executes(context -> {
                                        config.nightInterval = IntegerArgumentType.getInteger(context, "ticks");
                                        saveConfig();
                                        context.getSource().sendFeedback(Text.translatable("spambot.msg.night_delay_set", config.nightInterval));
                                        return 1;
                                    })))
                    .then(ClientCommandManager.literal("delay")
                            .then(ClientCommandManager.argument("ticks", IntegerArgumentType.integer(1))
                                    .executes(context -> {
                                        config.defaultInterval = IntegerArgumentType.getInteger(context, "ticks");
                                        saveConfig();
                                        context.getSource().sendFeedback(Text.translatable("spambot.msg.delay_set", config.defaultInterval));
                                        return 1;
                                    })))
                    .then(ClientCommandManager.literal("text")
                            .then(ClientCommandManager.argument("message", StringArgumentType.greedyString())
                                    .executes(context -> {
                                        config.spamMessage = StringArgumentType.getString(context, "message");
                                        saveConfig();
                                        context.getSource().sendFeedback(Text.translatable("spambot.msg.text_set"));
                                        return 1;
                                    })))
                    .then(ClientCommandManager.literal("info").executes(context -> {
                        context.getSource().sendFeedback(Text.literal("§b--- Spambot by _MrLisick_ ---"));
                        context.getSource().sendFeedback(Text.literal("§fNight: §e" + config.nightStartHour + ":00 - " + config.nightEndHour + ":00"));
                        context.getSource().sendFeedback(Text.literal("§fDefault KD: §e" + config.defaultInterval));
                        context.getSource().sendFeedback(Text.literal("§dNight KD: §e" + config.nightInterval));
                        return 1;
                    }))
            );
        });
    }

    private void saveConfig() {
        try (FileWriter writer = new FileWriter(CONFIG_PATH.toFile())) {
            GSON.toJson(config, writer);
        } catch (Exception ignored) {}
    }

    private void loadConfig() {
        File file = CONFIG_PATH.toFile();
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                config = GSON.fromJson(reader, Config.class);
            } catch (Exception ignored) {}
        } else {
            saveConfig();
        }
    }
}