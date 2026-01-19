package com.example.plugin;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

public class ChatAdminCommand extends AbstractCommandCollection {

    public ChatAdminCommand(LocalGlobalChatPlugin plugin) {
        super("chatadmin", "localglobalchat.commands.chatadmin.desc");

        // Subcomandos (ESTILO /op add)
        this.addSubCommand(new ChatAdminAddCommand(plugin));
        this.addSubCommand(new ChatAdminRemoveCommand(plugin));
        this.addSubCommand(new ChatAdminListCommand(plugin));

        // Permissions are enforced in execute to keep console/op working across builds
        LGChatCompat.relaxCommandPermissions(this);
    }
}
