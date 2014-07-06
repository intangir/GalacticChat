package com.github.intangir.GalacticChat.Commands;

import java.util.Arrays;

import com.github.intangir.GalacticChat.Chatter;
import com.github.intangir.GalacticChat.MainConfig;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import static com.github.intangir.GalacticChat.Utility.join;


@SuppressWarnings("deprecation")
public class ReplyCommand extends Command {

	private String command;
	
	public ReplyCommand(MainConfig config) {
		super(config.getCommand("reply"), "galacticchat.privatemessage", config.getAliases("reply"));
		command = config.getCommand("reply");
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		Chatter chatter = Chatter.find(sender.getName());
		if(chatter != null) {
			if(args.length >= 1) {
				chatter.reply(join(Arrays.asList(args), " "));
			} else {
				sender.sendMessage(ChatColor.RED + "Usage: /" + command + " <message>");
			}
		}
	}
}
