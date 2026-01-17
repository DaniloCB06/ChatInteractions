package com.example.plugin;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

public final class ClearChatCommand extends AbstractCommand {

    // Give this node only to the admin group in your permission system
    private static final String PERM_NODE = "localglobalchat.admin.clearchat";

    private static final int CLEAR_LINES = 120;

    public ClearChatCommand() {
        super("clearchat", "Clears the chat for all online players");

        // Requires permission (admin)
        LGChatCompat.requirePermissionNode(this, PERM_NODE);

        // (Optional) alias /cc if the build supports it
        trySetAliases(this, "cc");

        // (Optional) also tries to require a high permission level (if your build uses levels)
        trySetHighPermissionLevel(this, 4);
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @Override
    @Nullable
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
        // Console can usually run this (equivalent to admin)
        if (context.sender().getUuid() != null) {
            if (!LGChatCompat.hasPermissionCompat(context.sender(), PERM_NODE)) {
                context.sender().sendMessage(LGChatCompat.pinkMessage("No permission: " + PERM_NODE));
                return CompletableFuture.completedFuture(null);
            }
        }

        Message blank = Message.raw(" ");
        Message notice = buildNotice();

        int affected = 0;
        for (PlayerRef p : getOnlinePlayersCompat()) {
            if (p == null) continue;

            for (int i = 0; i < CLEAR_LINES; i++) {
                p.sendMessage(blank);
            }
            p.sendMessage(notice);
            affected++;
        }

        context.sender().sendMessage(Message.raw("Chat cleared for " + affected + " player(s)."));
        return CompletableFuture.completedFuture(null);
    }

    private static Message buildNotice() {
        String text = "Chat was cleared by staff.";

        // Try TinyMsg (if available), otherwise fallback to raw
        String tiny = "<color:gray>" + LGChatCompat.tinySafe(text) + "</color>";
        Message parsed = LGChatCompat.tryTinyMsgParse(tiny);
        return (parsed != null) ? parsed : Message.raw(text);
    }

    // ---------- Online players (compat / reflection) ----------
    private static Iterable<PlayerRef> getOnlinePlayersCompat() {
        try {
            Class<?> uniCl = Class.forName("com.hypixel.hytale.server.core.universe.Universe");
            Object uni = uniCl.getMethod("get").invoke(null);
            if (uni == null) return java.util.List.of();

            for (String mn : new String[]{"getPlayers", "players", "getOnlinePlayers", "onlinePlayers"}) {
                try {
                    Method m = uni.getClass().getMethod(mn);
                    Object r = m.invoke(uni);
                    if (r instanceof Iterable<?> it) {
                        java.util.ArrayList<PlayerRef> out = new java.util.ArrayList<>();
                        for (Object o : it) if (o instanceof PlayerRef pr) out.add(pr);
                        return out;
                    }
                } catch (Throwable ignored) { }
            }
        } catch (Throwable ignored) { }
        return java.util.List.of();
    }

    // ---------- Aliases (optional) ----------
    private static void trySetAliases(Object cmd, String... aliases) {
        if (cmd == null || aliases == null || aliases.length == 0) return;

        for (String mn : new String[]{"setAliases", "addAlias", "addAliases", "alias", "aliases"}) {
            try {
                Method m = cmd.getClass().getMethod(mn, String[].class);
                m.invoke(cmd, (Object) aliases);
                return;
            } catch (Throwable ignored) { }

            try {
                Method m = cmd.getClass().getMethod(mn, String.class);
                for (String a : aliases) m.invoke(cmd, a);
                return;
            } catch (Throwable ignored) { }
        }
    }

    // ---------- High permission level (optional) ----------
    private static void trySetHighPermissionLevel(Object cmd, int level) {
        String[] names = {
                "setRequiredPermissionLevel", "setMinPermissionLevel",
                "setPermissionLevel", "setRequiredLevel", "setMinLevel"
        };

        for (String n : names) {
            try {
                Method m = cmd.getClass().getMethod(n, int.class);
                m.invoke(cmd, level);
                return;
            } catch (Throwable ignored) { }
            try {
                Method m = cmd.getClass().getMethod(n, short.class);
                m.invoke(cmd, (short) level);
                return;
            } catch (Throwable ignored) { }
        }
    }
}
