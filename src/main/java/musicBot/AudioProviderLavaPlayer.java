package musicBot;

import java.nio.ByteBuffer;

import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame;

import discord4j.voice.AudioProvider;

public class AudioProviderLavaPlayer extends AudioProvider{
	
	private final AudioPlayer player;
	private final MutableAudioFrame frame = new MutableAudioFrame();
	
	public AudioProviderLavaPlayer(final AudioPlayer player) {
		super(ByteBuffer.allocate(StandardAudioDataFormats.DISCORD_OPUS.maximumChunkSize()));
		//System.out.println("creating music player");
		frame.setBuffer(getBuffer());
		this.player = player;
		//System.out.println("created");
	}

	@Override
	public boolean provide() {
		//System.out.println("providing!");
		final boolean didProvide = player.provide(frame);
		
		if(didProvide) {
			//System.out.println("did provide....");
			getBuffer().flip();
		}
		return didProvide;
	}

}
