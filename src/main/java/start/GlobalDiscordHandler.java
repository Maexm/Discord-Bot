package start;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.event.domain.channel.VoiceChannelDeleteEvent;
import discord4j.core.event.domain.guild.MemberLeaveEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import exceptions.IllegalMagicException;
import musicBot.MusicWrapper;
import survey.Survey;
import survey.VoteEndReason;
import system.GuildHandler;
import util.Pair;

public class GlobalDiscordHandler {

    private final HashMap<Snowflake, GuildHandler> guildMap;
    private final GuildHandler privateHandler;
    private final GlobalDiscordProxy globalProxy;
    private final GatewayDiscordClient client;
    private final ArrayList<Survey> surveys;
    private final AudioPlayerManager playerManager;

    public GlobalDiscordHandler(ReadyEvent readyEvent) {
        this.globalProxy = new GlobalDiscordProxy(this);
        this.client = readyEvent.getClient();

        this.playerManager = new DefaultAudioPlayerManager();
		playerManager.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
        AudioSourceManagers.registerRemoteSources(playerManager);

        // Prepare guilds
        this.guildMap = new HashMap<>();

        readyEvent.getGuilds().forEach(guild -> {
            MusicWrapper musicWrapper = new MusicWrapper(this.playerManager);
            GuildHandler guildHandler = new GuildHandler(guild.getId(), this.globalProxy, musicWrapper);
            this.guildMap.put(guild.getId(), guildHandler);
        });
        this.privateHandler = new GuildHandler(null, this.globalProxy, null);

        this.surveys = new ArrayList<>();

        this.client.updatePresence(Presence
					.online(Activity.playing(RuntimeVariables.getStatus())))
					.block();
    }

    public HashMap<Snowflake, GuildHandler> getGuildMap(){
        return this.guildMap;
    }

    public void acceptEvent(MessageCreateEvent event) {
        if(event.getGuildId().isPresent()){
            this.guildMap.get(event.getGuildId().get()).onMessageReceived(event);
        }
        else{
            this.privateHandler.onMessageReceived(event);
        }
    }

    void addGuild(Guild guild){
        if(!this.guildMap.containsKey(guild.getId())){
            System.out.println("Bot was added to guild "+guild.getName());
            MusicWrapper musicWrapper = new MusicWrapper(this.playerManager);
            GuildHandler guildHandler = new GuildHandler(guild.getId(), this.globalProxy, musicWrapper);
            this.guildMap.put(guild.getId(), guildHandler);
        }
    }

    void removeGuild(Guild guild){
        if(this.guildMap.containsKey(guild.getId())){
            System.out.println("Removing guild "+guild.getName());
            this.guildMap.remove(guild.getId()).onPurge();
        }
    }

    void onVoiceStateEvent(VoiceStateUpdateEvent event){
        this.guildMap.get(event.getCurrent().getGuildId()).onVoiceStateEvent(event);
    }

    private void purgeAllGuilds() {
        this.guildMap.forEach((guildId, handler) ->{
            handler.onPurge();
        });
    }

    private void logout(){
        System.out.println("LOGGING OUT");
		this.getClient().logout().block();
    }

    private GatewayDiscordClient getClient() {
        return this.client;
    }

    public void onVoiceChannelDeleted(VoiceChannelDeleteEvent event){
        if(this.guildMap.containsKey(event.getChannel().getGuildId())){
            this.guildMap.get(event.getChannel().getGuildId()).onVoiceChannelDeleted(event);
        }
    }

    public void onMemberLeavesGuild(MemberLeaveEvent event){
        if(this.guildMap.containsKey(event.getGuildId())){
            this.guildMap.get(event.getGuildId()).onUserRemoved(event);
        }
    }

    public class GlobalDiscordProxy {

        private final GlobalDiscordHandler parent;

        private GlobalDiscordProxy(GlobalDiscordHandler parent) {
            this.parent = parent;
        }

        public void purgeAllGuilds() {
            parent.purgeAllGuilds();
        }

        public void logout(){
            parent.logout();
        }

        public GatewayDiscordClient getClient() {
            return this.parent.getClient();
        }

        public Survey getSurveyForKeyVerbose(String keyword){
            for (Survey survey : parent.surveys) {
                if (survey.key.equals(keyword)) {
                    return survey;
                }
            }
            return null;
        }

        public Survey getSurveyForKeyGuildScoped(String keyword, Snowflake guildId){
            Survey survey = this.getSurveyForKeyVerbose(keyword);
            return survey.guildId.equals(guildId) ? survey : null;
        }

        public Survey getSurveyForKeyUserScoped(String keyword, Snowflake userId){
            Survey survey = this.getSurveyForKeyVerbose(keyword);
            Member member;
            try{
                member = parent.getClient().getGuildById(survey.guildId).flatMap(guild -> guild.getMemberById(userId)).block();
            }
            catch(Exception e){
                member = null;
            }
            return member != null ? survey : null;
        }

        public void createSurvey(Survey survey){
            if(this.getSurveyForKeyVerbose(survey.key) != null){
                survey.stop(VoteEndReason.BROKEN);
                throw new IllegalMagicException("Survey key collision detected for key: "+survey.key);
            }
            parent.surveys.add(survey);
        }

        public ArrayList<Survey> getSurveysVerbose(){
            return parent.surveys;
        }

        public HashSet<Pair<Guild, VoiceChannel>> getSubscribedGuildChannelPairs(Snowflake userId){
            HashSet<Pair<Guild, VoiceChannel>> ret = new HashSet<>();

            // Search all guilds
            parent.getGuildMap().forEach((guildId, guildHandler) -> {
                // User is in guild
                if(guildHandler.hasUser(userId)){
                    // Search each subscribed voice channel
                    guildHandler.getVoiceSubscriptions().forEach((channelId, userSet) ->{
                        if(userSet.contains(userId)){
                            ret.add(new Pair<>(guildHandler.getGuild(), (VoiceChannel) guildHandler.getGuild().getChannelById(channelId).block()));
                        }
                    });
                }
            });

            return ret;
        }

        public ArrayList<MessageChannel> getGlobalSystemChannels(){
            ArrayList<MessageChannel> ret = new ArrayList<>();
            parent.getGuildMap().forEach((guildId, guildHandler) ->{
                ret.add(guildHandler.getResponseType().getSystemChannel());
            });

            return ret;
        }
    }

}
