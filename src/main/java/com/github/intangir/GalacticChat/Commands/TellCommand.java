package com.github.intangir.GalacticChat.Commands;

import java.util.Arrays;
import java.util.List;

import com.github.intangir.GalacticChat.Chatter;
import com.github.intangir.GalacticChat.MainConfig;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import static com.github.intangir.GalacticChat.Utility.join;


@SuppressWarnings("deprecation")
public class TellCommand extends Command {

	private String command;
	
	public TellCommand(MainConfig config) {
		super(config.getCommand("tell"), "galacticchat.privatemessage", config.getAliases("tell"));
		command = config.getCommand("tell");
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		Chatter chatter = Chatter.find(sender.getName());
		if(chatter != null) {
			if(args.length >= 2) {
				List<String> list = Arrays.asList(args);
				chatter.tell(args[0], join(list.subList(1, list.size()), " "));
			} else {
				sender.sendMessage(ChatColor.RED + "Usage: /" + command + " <player> <message>");
			}
		}
	}
}
