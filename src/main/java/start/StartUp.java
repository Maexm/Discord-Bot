package start;

import java.io.Console;
import java.util.List;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.GuildEmoji;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import discord4j.voice.AudioProvider;
import exceptions.StartUpException;
import system.BotHeart;
import musicBot.AudioProviderLavaPlayer;
import services.Markdown;
import snowflakes.ChannelID;
import snowflakes.EmojiID;
import snowflakes.GuildID;

public class StartUp {

	public static void main(String[] args) {

		System.out.println("STARTING MEGUMIN BOT");
		// Retrieve token
		String TOKEN = "";
		if (args.length >= 1) {
			TOKEN = args[0];
		} else {
			System.out.println("Please enter a token (won't be displayed): ");
			Console console = System.console();
			TOKEN = new String(console.readPassword());
			if (TOKEN == null || TOKEN.equals("")) {
				throw new StartUpException("Token missing");
			}
		}
		// Retrieve debug info, if available
		if (args.length >= 2 && args[1].toUpperCase().equals("DEBUG")) {
			RuntimeVariables.IS_DEBUG = true;
			reactor.util.Loggers.useJdkLoggers();
		}

		// Music components
		final AudioPlayerManager playerManager = new DefaultAudioPlayerManager();

		playerManager.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);

		AudioSourceManagers.registerRemoteSources(playerManager);
		final AudioPlayer player = playerManager.createPlayer();
		AudioProvider provider = new AudioProviderLavaPlayer(player);

		final GatewayDiscordClient client = DiscordClientBuilder.create(TOKEN).build().login().block();

		final BotHeart messageReceivedHandler = new BotHeart(client, provider, player,
				playerManager);

		// ########## On client login ##########

		client.getEventDispatcher().on(ReadyEvent.class).subscribe(ready -> {
			System.out.println("LOGGED IN AS: '" + ready.getSelf().getUsername() + "'");
			System.out.println("Currently member of " + ready.getGuilds().size() + " guilds");

			client.updatePresence(Presence
					.online(Activity.playing(RuntimeVariables.IS_DEBUG ? "EXPERIMENTELL" : "Schreib 'MegHelp'!")))
					.block();

			if(RuntimeVariables.firstLogin){
				try {
					List<GuildEmoji> emojis = client.getGuildById(GuildID.UNSER_SERVER).block().getEmojis().buffer()
							.blockFirst();
					String emojiFormat = "";
					for (GuildEmoji emoji : emojis) {
						if (emoji.getId().equals(EmojiID.MEG_THUMBUP)) {
							emojiFormat = emoji.asFormat();
							break;
						}
					}
					MessageChannel channel = (MessageChannel) client.getGuildById(GuildID.UNSER_SERVER).block()
							.getChannelById(ChannelID.MEGUMIN).block();
	
					channel.createMessage("Megumin ist online und einsatzbereit! " + emojiFormat + " Schreib "
							+ Markdown.toBold("'MegHelp'") + " für mehr Informationen! ").block();
				} catch (Exception e) {
					e.printStackTrace();
				}
				RuntimeVariables.firstLogin = false;
			}
			else{
				System.out.println("Reconnected!");
				MessageChannel channel = (MessageChannel) client.getGuildById(GuildID.UNSER_SERVER).block()
							.getChannelById(ChannelID.MEGUMIN).block();
	
					channel.createMessage(":warning: Ein Verbindungsfehler ist aufgetreten... Jetzt bin ich aber wieder verbunden!").block();
			}
		});

		// ########## On received message ##########

		client.getEventDispatcher().on(MessageCreateEvent.class).log().subscribe(event -> {
			if (RuntimeVariables.IS_DEBUG) {
				System.out.println("Event received!");
			}
			messageReceivedHandler.onMessageReceived(event);
		});
		
		// ########## On logout ##########
		client.onDisconnect().block();
	}

}
