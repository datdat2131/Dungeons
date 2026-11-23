package com.astrax.dungeon.integration;

import com.astrax.dungeon.AstraXDungeon;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/** Gui thong bao len Discord bang webhook (tuy chon: de trong discord.webhook-url la tat). */
public final class DiscordHook {
    private final AstraXDungeon plugin;
    private final HttpClient client = HttpClient.newHttpClient();

    public DiscordHook(AstraXDungeon plugin) { this.plugin = plugin; }

    private String url() { return plugin.getConfig().getString("discord.webhook-url", ""); }

    public boolean enabled() {
        String u = url();
        return u != null && !u.isBlank();
    }

    public void send(String text) {
        if (!enabled()) return;
        try {
            String clean = text.replaceAll("&[0-9a-fk-orA-FK-OR]", "");
            String json = "{\"content\":\"" + escape(clean) + "\"}";
            HttpRequest req = HttpRequest.newBuilder(URI.create(url()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            client.sendAsync(req, HttpResponse.BodyHandlers.discarding()).exceptionally(e -> {
                plugin.getLogger().warning("Discord webhook loi: " + e.getMessage());
                return null;
            });
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("discord.webhook-url khong hop le.");
        }
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}
