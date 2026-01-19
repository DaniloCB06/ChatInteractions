package com.example.plugin;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.UUID;

public class ChatAdminListCommand extends CommandBase {

    private final LocalGlobalChatPlugin plugin;

    public ChatAdminListCommand(LocalGlobalChatPlugin plugin) {
        super("list", "localglobalchat.commands.chatadmin.list.desc");
        this.plugin = plugin;

        LGChatCompat.relaxCommandPermissions(this);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        if (!plugin.canUseChatAdmin(context)) {
            context.sendMessage(LocalGlobalChatPlugin.systemColor("red", "You don't have permission."));
            return;
        }

        Set<UUID> list = plugin.getChatAdminsSnapshot();
        if (list.isEmpty()) {
            context.sendMessage(Message.raw("ChatAdmins: (empty)"));
            return;
        }

        StringBuilder sb = new StringBuilder("ChatAdmins (" + list.size() + "):\n");
        for (UUID u : list) {
            String name = plugin.resolveOnlineName(u);
            sb.append("- ").append(u);
            if (name != null) sb.append(" (").append(name).append(")");
            sb.append("\n");
        }
        context.sendMessage(Message.raw(sb.toString().trim()));
    }
}
