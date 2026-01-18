package com.example.plugin;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

public class ChatDisableCommand extends CommandBase {

    private final LocalGlobalChatPlugin plugin;

    public ChatDisableCommand(LocalGlobalChatPlugin plugin) {
        super("chatdisable", "Disables/enables the chat (admin/op only).");
        this.plugin = plugin;

        // Alias /cdb
        addAliasCompat("cdb");
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        Object sender = extractSender(context);

        // Console -> sempre pode
        UUID senderUuid = extractUuid(sender);
        if (sender == null || senderUuid == null) {
            toggleAndBroadcast();
            return;
        }

        // 1) Allowlist do plugin (chatAdmins) -> pode
        // (use /chatadmin add <nick/uuid> pra adicionar)
        if (plugin.isChatAdmin(senderUuid)) {
            toggleAndBroadcast();
            return;
        }

        // 2) OP/Admin do servidor -> pode
        if (isOperatorOrAdmin(sender, senderUuid)) {
            toggleAndBroadcast();
            return;
        }

        // Caso contrário -> bloqueia
        context.sendMessage(LocalGlobalChatPlugin.systemColor("red",
                "Only admins/operators can use this command."));
    }

    private void toggleAndBroadcast() {
        boolean nowDisabled = plugin.toggleChatDisabled();

        if (nowDisabled) {
            plugin.broadcastSystemMessage(LocalGlobalChatPlugin.systemColor("red",
                    "Chat has been disabled by the staff."));
        } else {
            plugin.broadcastSystemMessage(LocalGlobalChatPlugin.systemColor("green",
                    "Chat has been re-enabled by the staff."));
        }
    }

    // ---------------- Sender / UUID ----------------

    private static Object extractSender(CommandContext context) {
        if (context == null) return null;
        try {
            Method m = context.getClass().getMethod("sender");
            return m.invoke(context);
        } catch (Throwable ignored) { }
        try {
            Method m = context.getClass().getMethod("getSender");
            return m.invoke(context);
        } catch (Throwable ignored) { }
        return null;
    }

    private static UUID extractUuid(Object sender) {
        if (sender == null) return null;

        // Sender pode ser PlayerRef direto
        if (sender instanceof PlayerRef pr) {
            try { return pr.getUuid(); } catch (Throwable ignored) { }
        }

        // Wrapper pode ter getUuid()/uuid()
        for (String mn : new String[]{"getUuid", "uuid", "getUniqueId", "uniqueId"}) {
            try {
                Method m = sender.getClass().getMethod(mn);
                Object r = m.invoke(sender);
                if (r instanceof UUID u) return u;
            } catch (Throwable ignored) { }
        }

        // Wrapper pode expor getPlayer()/player()
        for (String mn : new String[]{"getPlayer", "player", "asPlayer"}) {
            try {
                Method m = sender.getClass().getMethod(mn);
                Object r = m.invoke(sender);
                if (r instanceof PlayerRef pr) {
                    try { return pr.getUuid(); } catch (Throwable ignored) { }
                }
            } catch (Throwable ignored) { }
        }

        return null;
    }

    // ---------------- OP/Admin detection (robusto) ----------------

    private static boolean isOperatorOrAdmin(Object sender, UUID uuid) {
        // 1) tenta boolean methods no sender wrapper
        if (sender != null && hasTrueBooleanMethod(sender,
                "isAdmin", "isOperator", "isOp", "isUniverseOperator", "isUniverseOp")) {
            return true;
        }

        // 2) se tiver PlayerRef dentro, tenta nele também
        PlayerRef pr = extractPlayerRef(sender);
        if (pr != null && hasTrueBooleanMethod(pr,
                "isAdmin", "isOperator", "isOp", "isUniverseOperator", "isUniverseOp")) {
            return true;
        }

        // 3) fallback: Universe.get().isOperator(uuid) (nomes variam por build)
        if (uuid != null && universeSaysOperator(uuid)) {
            return true;
        }

        return false;
    }

    private static PlayerRef extractPlayerRef(Object sender) {
        if (sender instanceof PlayerRef pr) return pr;
        if (sender == null) return null;

        for (String mn : new String[]{"getPlayer", "player", "asPlayer"}) {
            try {
                Method m = sender.getClass().getMethod(mn);
                Object r = m.invoke(sender);
                if (r instanceof PlayerRef pr) return pr;
            } catch (Throwable ignored) { }
        }
        return null;
    }

    private static boolean hasTrueBooleanMethod(Object obj, String... names) {
        for (String mn : names) {
            try {
                Method m = obj.getClass().getMethod(mn);
                Object r = m.invoke(obj);
                if (r instanceof Boolean b && b) return true;
            } catch (Throwable ignored) { }
        }
        return false;
    }

    private static boolean universeSaysOperator(UUID uuid) {
        try {
            Class<?> uniCl = Class.forName("com.hypixel.hytale.server.core.universe.Universe");
            Object uni = uniCl.getMethod("get").invoke(null);
            if (uni == null) return false;

            for (String mn : new String[]{"isOperator", "isOp", "isPlayerOperator", "isUniverseOperator"}) {
                try {
                    Method m = uni.getClass().getMethod(mn, UUID.class);
                    Object r = m.invoke(uni, uuid);
                    if (r instanceof Boolean b && b) return true;
                } catch (Throwable ignored) { }
            }
        } catch (Throwable ignored) { }
        return false;
    }

    // ---------------- Alias compat ----------------

    private void addAliasCompat(String alias) {
        String[] methods = {"addAlias", "addAliases", "setAlias", "setAliases"};

        for (String mn : methods) {
            try {
                Method m = this.getClass().getMethod(mn, String.class);
                m.invoke(this, alias);
                return;
            } catch (Throwable ignored) { }

            try {
                Method m = this.getClass().getMethod(mn, String[].class);
                m.invoke(this, (Object) new String[]{alias});
                return;
            } catch (Throwable ignored) { }

            try {
                Method m = this.getClass().getMethod(mn, Collection.class);
                m.invoke(this, Arrays.asList(alias));
                return;
            } catch (Throwable ignored) { }
        }
    }
}
