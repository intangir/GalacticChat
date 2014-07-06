package com.github.intangir.GalacticChat.Commands;

import java.util.Arrays;

import com.github.intangir.GalacticChat.Chatter;
import com.github.intangir.GalacticChat.MainConfig;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import static com.github.intangir.GalacticChat.Utility.join;


@SuppressWarnings("deprecation")
public class RetellCommand extends Command {

	private String command;
	
	public RetellCommand(MainConfig config) {
		super(config.getCommand("retell"), "galacticchat.privatemessage", config.getAliases("retell"));
		command = config.getCommand("retell");
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		Chatter chatter = Chatter.find(sender.getName());
		if(chatter != null) {
			if(args.length >= 1) {
				chatter.retell(join(Arrays.asList(args), " "));
			} else {
				sender.sendMessage(ChatColor.RED + "Usage: /" + command + " <message>");
			}
		}
	}
}
