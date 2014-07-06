package com.github.intangir.GalacticChat.Commands;

import com.github.intangir.GalacticChat.Chatter;
import com.github.intangir.GalacticChat.MainConfig;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

@SuppressWarnings("deprecation")
public class LeaveCommand extends Command {

	private String command;
	
	public LeaveCommand(MainConfig config) {
		super(config.getCommand("leave"), "galacticchat.joinleave", config.getAliases("leave"));
		command = config.getCommand("leave");
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		Chatter chatter = Chatter.find(sender.getName());
		if(chatter != null) {
			if(args.length == 0) {
				chatter.leave(null);
			} else if(args.length == 1) {
				chatter.leave(args[0]);
			} else {
				sender.sendMessage(ChatColor.RED + "Usage: /" + command + " [channel/alias]");
			}
		}
	}
}
