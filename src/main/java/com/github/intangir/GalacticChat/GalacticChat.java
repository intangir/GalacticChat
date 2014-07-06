package com.github.intangir.GalacticChat;

import com.github.intangir.GalacticChat.Commands.CensorCommand;
import com.github.intangir.GalacticChat.Commands.ChannelCommand;
import com.github.intangir.GalacticChat.Commands.IgnoreCommand;
import com.github.intangir.GalacticChat.Commands.JoinCommand;
import com.github.intangir.GalacticChat.Commands.LeaveCommand;
import com.github.intangir.GalacticChat.Commands.ReplyCommand;
import com.github.intangir.GalacticChat.Commands.RetellCommand;
import com.github.intangir.GalacticChat.Commands.SayCommand;
import com.github.intangir.GalacticChat.Commands.TellCommand;
import com.github.intangir.GalacticChat.Commands.UnignoreCommand;

import net.cubespace.Yamler.Config.InvalidConfigurationException;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import lombok.Getter;

import net.md_5.bungee.api.plugin.Plugin;

public class GalacticChat extends Plugin implements Listener
{
	@Getter
	public MainConfig config;
	
	@Override
    public void onEnable() {
        config = new MainConfig(this);
        try {
			config.init();
		} catch (InvalidConfigurationException e) {
			getLogger().severe("Couldn't Load config.yml");
		}
        getProxy().getPluginManager().registerListener(this, this);
        getProxy().getPluginManager().registerCommand(this, new JoinCommand(config));
        getProxy().getPluginManager().registerCommand(this, new LeaveCommand(config));
        getProxy().getPluginManager().registerCommand(this, new InviteCommand(config));
        getProxy().getPluginManager().registerCommand(this, new CensorCommand(config));
        getProxy().getPluginManager().registerCommand(this, new ChannelCommand(config));
        getProxy().getPluginManager().registerCommand(this, new IgnoreCommand(config));
        getProxy().getPluginManager().registerCommand(this, new UnignoreCommand(config));
        getProxy().getPluginManager().registerCommand(this, new TellCommand(config));
        getProxy().getPluginManager().registerCommand(this, new RetellCommand(config));
        getProxy().getPluginManager().registerCommand(this, new ReplyCommand(config));
        getProxy().getPluginManager().registerCommand(this, new SayCommand(config));
        getProxy().getPluginManager().registerCommand(this, new BanCommand(config));
        Chatter.logOn(this, getProxy().getConsole());
    }
	
    @EventHandler
    public void onLoggedIn(final PostLoginEvent e) {
    	Chatter.logOn(this, e.getPlayer());
    }

    @EventHandler
    public void onLoggedOut(final PlayerDisconnectEvent e) {
    	Chatter.logOut(e.getPlayer());
    }
    
    @EventHandler
    public void onChatEvent(final ChatEvent e) {
    	getLogger().info("caught event");
    	if(e.isCancelled()) { return; }
    	if(e.getSender() instanceof CommandSender) {
	    	Chatter chatter = Chatter.find((CommandSender)e.getSender());
			if(chatter != null && chatter.parseChat(e.getMessage())) {
				e.setCancelled(true);
			}
    	}
    }
}