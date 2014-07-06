package com.github.intangir.GalacticChat.Commands;

import com.github.intangir.GalacticChat.Chatter;
import com.github.intangir.GalacticChat.MainConfig;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

@SuppressWarnings("deprecation")
public class IgnoreCommand extends Command {

	private String command;

	public IgnoreCommand(MainConfig config) {
		super(config.getCommand("ignore"), null, config.getAliases("ignore"));
		command = config.getCommand("ignore");
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		Chatter chatter = Chatter.find(sender.getName());
		if(chatter != null) {
			if(args.length == 1) {
					chatter.ignore(args[0], null);
			} else {
				sender.sendMessage(ChatColor.RED + "Usage: /" + command +" <player>");
			}
		}
	}
}
