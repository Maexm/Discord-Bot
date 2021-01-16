package start;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.event.domain.channel.VoiceChannelDeleteEvent;
import discord4j.core.event.domain.guild.MemberLeaveEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.VoiceChannel;
import exceptions.IllegalMagicException;
import survey.Survey;
import survey.VoteEndReason;
import system.GuildHandler;
import util.Pair;

public class GlobalDiscordHandler {

    private final HashMap<Snowflake, GuildHandler> guildMap;
    private final GuildHandler privateHandler;
    private final GlobalDiscordProxy globalProxy;
    private final GatewayDiscordClient client;
    private final boolean isDummy;
    private final ArrayList<Survey> surveys;

    public GlobalDiscordHandler(ReadyEvent readyEvent) {

        if (readyEvent == null) {
            this.guildMap = null;
            this.globalProxy = null;
            this.client = null;
            this.privateHandler = null;
            this.isDummy = true;
            this.surveys = null;
        } else {
            this.globalProxy = new GlobalDiscordProxy(this);
            this.client = readyEvent.getClient();

            // Prepare guilds
            this.guildMap = new HashMap<>();

            readyEvent.getGuilds().forEach(guild -> {
                GuildHandler guildHandler = new GuildHandler(guild.getId(), this.globalProxy);
                this.guildMap.put(guild.getId(), guildHandler);
            });
            this.privateHandler = new GuildHandler(null, this.globalProxy);

            this.surveys = new ArrayList<>();

            this.isDummy = false;
        }
    }

    public HashMap<Snowflake, GuildHandler> getGuildMap(){
        return this.guildMap;
    }

    public boolean isDummy(){
       return this.isDummy;
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
            GuildHandler guildHandler = new GuildHandler(guild.getId(), this.globalProxy);
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
            return survey.publicMessage.getGuildId().get().equals(guildId) ? survey : null;
        }

        public Survey getSurveyForKeyUserScoped(String keyword, Snowflake userId){
            Survey survey = this.getSurveyForKeyVerbose(keyword);
            Member member;
            try{
                member = survey.publicMessage.getGuild().flatMap(guild -> guild.getMemberById(userId)).block();
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
    }

}
