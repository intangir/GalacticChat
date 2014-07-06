package com.github.intangir.GalacticChat.Commands;

import com.github.intangir.GalacticChat.Chatter;
import com.github.intangir.GalacticChat.MainConfig;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

@SuppressWarnings("deprecation")
public class BanCommand extends Command {

	private String command;

	public BanCommand(MainConfig config) {
		super(config.getCommand("ban"), "galacticchat.ban", config.getAliases("ban"));
		command = config.getCommand("ban");
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		Chatter chatter = Chatter.find(sender.getName());
		if(chatter != null) {
			if(args.length == 1) {
				chatter.ban(args[0], null);
			} else if(args.length == 2) {
				chatter.ban(args[0], args[1]);
			} else {
				sender.sendMessage(ChatColor.RED + "Usage: /" + command + " <player> [channel]");
			}
		}
	}
}
