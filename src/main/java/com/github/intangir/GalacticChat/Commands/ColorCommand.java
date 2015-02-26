package com.github.intangir.GalacticChat.Commands;

import com.github.intangir.GalacticChat.Chatter;
import com.github.intangir.GalacticChat.MainConfig;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

@SuppressWarnings("deprecation")
public class ColorCommand extends Command {

	private String command;

	public ColorCommand(MainConfig config) {
		super(config.getCommand("color"), null, config.getAliases("color"));
		command = config.getCommand("color");
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		Chatter chatter = Chatter.find(sender.getName());
		if(chatter != null) {
			if(args.length == 1) {
					chatter.color(args[0]);
			} else {
				sender.sendMessage(ChatColor.RED + "Usage: /" + command +" <color>");
			}
		}
	}
}
