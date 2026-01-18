package com.example.plugin;

import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.permissions.HytalePermissions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class ChatWarningCommand extends AbstractCommand {

    private static final String PERM_CHATWARNING = HytalePermissions.fromCommand("chatwarning");

    private final LocalGlobalChatPlugin plugin;
    private final RequiredArg<Integer> minutesArg;

    public ChatWarningCommand(LocalGlobalChatPlugin plugin) {
        super("chatwarning", "Configures the periodic chat warning (/cw <minutes>, 0 = disable)");
        this.plugin = plugin;

        // Allow execution even without a permission provider; we enforce admin-only manually below
        LGChatCompat.relaxCommandPermissions(this);

        requirePermission(PERM_CHATWARNING);

        addAliases("cw");

        minutesArg = withRequiredArg(
                "minutes",
                "Interval in minutes (0 = disable)",
                ArgTypes.INTEGER
        );
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        if (sender == null) return false;

        UUID senderUuid = sender.getUuid();
        if (senderUuid == null) return true; // console

        if (plugin.isChatAdmin(senderUuid)) return true;

        if (sender.hasPermission(PERM_CHATWARNING)) return true;

        return plugin.isAdminOrOp(sender);
    }

    @Override
    @Nullable
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
        UUID senderUuid = context.sender().getUuid();
        if (senderUuid != null && !plugin.isChatAdmin(senderUuid)) {
            context.sender().sendMessage(LocalGlobalChatPlugin.systemColor("red",
                    "Only chatadmins can use this command. Ask a staff member to add you with /chatadmin add <player|uuid>."));
            return CompletableFuture.completedFuture(null);
        }

        int mins;
        try {
            mins = context.get(minutesArg);
        } catch (Throwable t) {
            mins = 0;
        }

        plugin.setChatWarningMinutes(mins);
        context.sender().sendMessage(plugin.buildChatWarningConfigFeedback(mins));

        return CompletableFuture.completedFuture(null);
    }
}
