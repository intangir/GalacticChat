package com.github.intangir.GalacticChat.Commands;

import java.util.Arrays;

import com.github.intangir.GalacticChat.Chatter;
import com.github.intangir.GalacticChat.MainConfig;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import static com.github.intangir.GalacticChat.Utility.join;

// this is pretty much only valuable for the Proxy Console
public class SayCommand extends Command {
	
	public SayCommand(MainConfig config) {
		super(config.getCommand("say"), null, config.getAliases("say"));
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		Chatter chatter = Chatter.find(sender.getName());
		if(chatter != null) {
			chatter.parseChat(join(Arrays.asList(args), " "));
		}
	}
}
