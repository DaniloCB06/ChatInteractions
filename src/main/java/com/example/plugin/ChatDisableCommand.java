package com.example.plugin;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;

public class ChatDisableCommand extends CommandBase {

    private final LocalGlobalChatPlugin plugin;

    public ChatDisableCommand(LocalGlobalChatPlugin plugin) {
        super("chatdisable", "localglobalchat.commands.chardisable.desc");
        this.plugin = plugin;

        // Permission (admins)
        LGChatCompat.requirePermissionNode(this, LocalGlobalChatPlugin.PERM_CHAT_DISABLE);

        // Alias /cdb (reflection-compatible)
        addAliasCompat("cdb");
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        // Double-check (in addition to the command's requiredPermission)
        if (!senderHasPermission(context, LocalGlobalChatPlugin.PERM_CHAT_DISABLE)) {
            context.sendMessage(LocalGlobalChatPlugin.systemColor("red",
                    "You don't have permission to use this command."));
            return;
        }

        boolean nowDisabled = plugin.toggleChatDisabled();

        if (nowDisabled) {
            plugin.broadcastSystemMessage(LocalGlobalChatPlugin.systemColor("red",
                    "Chat has been disabled by the staff."));
        } else {
            plugin.broadcastSystemMessage(LocalGlobalChatPlugin.systemColor("green",
                    "Chat has been re-enabled by the staff."));
        }
    }

    private static boolean senderHasPermission(CommandContext context, String node) {
        try {
            // context.sender() -> hasPermission("node")
            Method senderM = context.getClass().getMethod("sender");
            Object sender = senderM.invoke(context);
            return LGChatCompat.hasPermissionCompat(sender, node);
        } catch (Throwable ignored) {
            // fallback: if this build doesn't expose sender(), let the server validate via the command permission node
            return true;
        }
    }

    private void addAliasCompat(String alias) {
        String[] methods = {"addAlias", "addAliases", "setAlias", "setAliases"};

        for (String mn : methods) {
            // (String)
            try {
                Method m = this.getClass().getMethod(mn, String.class);
                m.invoke(this, alias);
                return;
            } catch (Throwable ignored) { }

            // (String[])
            try {
                Method m = this.getClass().getMethod(mn, String[].class);
                m.invoke(this, (Object) new String[]{alias});
                return;
            } catch (Throwable ignored) { }

            // (Collection)
            try {
                Method m = this.getClass().getMethod(mn, Collection.class);
                m.invoke(this, Arrays.asList(alias));
                return;
            } catch (Throwable ignored) { }
        }
    }
}
