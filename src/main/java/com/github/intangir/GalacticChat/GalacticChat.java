package com.github.intangir.GalacticChat;

import net.md_5.bungee.api.plugin.Plugin;

public class GalacticChat extends Plugin {

    @Override
    public void onEnable() {
        getProxy().getPluginManager().registerListener(this, new GalacticChatListener());
    }

}