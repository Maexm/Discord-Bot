package start;

import java.util.ArrayList;
import java.util.HashMap;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import exceptions.IllegalMagicException;
import survey.Survey;
import survey.VoteEndReason;
import system.GuildHandler;

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
    }

}
