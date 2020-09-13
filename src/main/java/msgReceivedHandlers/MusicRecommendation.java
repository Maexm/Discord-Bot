package msgReceivedHandlers;

import java.util.ArrayList;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.presence.Status;
import discord4j.voice.AudioProvider;
import musicBot.AudioEventHandler;
import services.Markdown;
import snowflakes.ChannelID;
import snowflakes.UserID;
import survey.Survey;

public class MusicRecommendation extends Middleware {

    public MusicRecommendation(GatewayDiscordClient client, AudioProvider audioProvider, ArrayList<Survey> surveys,
            AudioEventHandler audioEventHandler) {
        super(client, audioProvider, surveys, audioEventHandler);
    }

    @Override
    public boolean handle() {

        if (this.msgContent.contains(UserID.MAXIM.toString()) && this.msgObject.getChannelId().equals(ChannelID.MUSIK)
                && this.msgAuthorObject != null && !this.msgAuthorObject.getId().equals(UserID.MAXIM)
                && this.getMemberPresence(UserID.MAXIM, this.getMessageGuild().getId()).getStatus()
                        .compareTo(Status.ONLINE) != 0) {
            this.sendAnswer(
                    "Danke für deine Musikempfehlung! Maxim ist aktuell beschäftigt, ich habe ihm aber die Nachricht weitergeschickt, damit er alles auf einem Blick hat!");

                    String msg = Markdown.toBold(this.getMessageAuthorName())+" hat dir eine Musikempfehlung hinterlassen!\n"
                    + Markdown.toBlockQuotes(this.msgContent);

                    this.getMember(UserID.MAXIM, this.getMessageGuild().getId()).getPrivateChannel().block().createMessage(msg).block();
        }
        return true;
    }

}
