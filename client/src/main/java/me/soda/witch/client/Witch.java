package me.soda.witch.client;

import me.soda.witch.client.connection.Client;
import me.soda.witch.client.connection.MessageHandler;
import me.soda.witch.client.events.events.MessageReceiveEvent;
import me.soda.witch.client.features.ChatCommandLogging;
import me.soda.witch.shared.Cfg;
import net.minecraft.client.MinecraftClient;

public class Witch {
    public static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final boolean print = Boolean.getBoolean("fabric.development");
    //config
    private static final String server = Cfg.server();
    //variables
    public static Client client;

    public static void init() {
        ChatCommandLogging.init();
        MessageReceiveEvent.INSTANCE.registerEvent((id, message) -> MessageHandler.handleMessage(message));
        try {
            client = new Client(server);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void printStackTrace(Exception e) {
        if (Witch.print) e.printStackTrace();
    }

    public static void println(Object o) {
        if (Witch.print) System.out.println("[WITCH] " + o);
    }
}
