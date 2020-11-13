package system;

import java.util.ArrayList;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.presence.Status;
import discord4j.voice.AudioProvider;
import musicBot.AudioEventHandler;
import services.Markdown;
import snowflakes.ChannelID;
import survey.Survey;

public class MusicRecommendation extends Middleware {

    public MusicRecommendation(GatewayDiscordClient client, AudioProvider audioProvider, ArrayList<Survey> surveys,
            AudioEventHandler audioEventHandler) {
        super(client, audioProvider, surveys, audioEventHandler);
    }

    @Override
    public boolean handle() {

        final Snowflake ownerId = this.getOwner().getId();
        if (this.msgContent.contains(this.getOwner().getMention())
                && !this.isPrivate()
                && this.msgObject.getChannelId().equals(ChannelID.MUSIK) && this.msgAuthorObject != null
                && !this.msgAuthorObject.getId().equals(ownerId)
                && this.getMemberPresence(ownerId, this.getMessageGuild().getId()).getStatus()
                        .compareTo(Status.ONLINE) != 0) {
            this.sendAnswer(
                    "Danke für deine Musikempfehlung! "+this.getOwner().asMember(this.getMessageGuild().getId()).block().getDisplayName()
                    +"ist aktuell beschäftigt, ich habe ihm aber die Nachricht weitergeschickt, damit er alles auf einem Blick hat!");

            String msg = Markdown.toBold(this.getMessageAuthorName()) + " hat dir eine Musikempfehlung hinterlassen!\n"
                    + Markdown.toMultilineBlockQuotes(this.msgContent);

            this.getMember(ownerId, this.getMessageGuild().getId()).getPrivateChannel().block().createMessage(msg)
                    .block();
        }
        return true;
    }

}
