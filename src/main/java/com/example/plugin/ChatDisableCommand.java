package com.example.plugin;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;

public class ChatDisableCommand extends AbstractCommandCollection {

    public ChatDisableCommand(LocalGlobalChatPlugin plugin) {
        super("chatdisable", "Disables/enables global/local chat or private messages (admin/op only).");

        this.addSubCommand(new ChatDisableGlobalCommand(plugin));
        this.addSubCommand(new ChatDisableLocalCommand(plugin));
        this.addSubCommand(new ChatDisableMsgCommand(plugin));

        // Alias /cdb
        addAliasCompat("cdb");
    }

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
