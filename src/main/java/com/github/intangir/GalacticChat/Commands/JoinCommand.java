package com.github.intangir.GalacticChat.Commands;

import com.github.intangir.GalacticChat.Chatter;
import com.github.intangir.GalacticChat.MainConfig;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

@SuppressWarnings("deprecation")
public class JoinCommand extends Command {

	public String command;
	
	public JoinCommand(MainConfig config) {
		super(config.getCommand("join"), "galacticchat.joinleave", config.getAliases("join"));
		command = config.getCommand("join");
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		Chatter chatter = Chatter.find(sender.getName());
		if(chatter != null) {
			if(args.length == 0) {
				chatter.parseJoin(null, null);
			} else if(args.length == 1) {
				chatter.parseJoin(args[0], null);
			} else if(args.length == 2) {
				chatter.parseJoin(args[0], args[1]);
			} else {
				sender.sendMessage(ChatColor.RED + "Usage: /" + command + " [channel] [alias]");
			}
		}
	}
}
