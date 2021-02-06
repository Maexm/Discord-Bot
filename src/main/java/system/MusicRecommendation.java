package system;

import discord4j.common.util.Snowflake;
import discord4j.core.object.presence.Status;
import util.Markdown;
import snowflakes.ChannelID;

public class MusicRecommendation extends Middleware {

    public MusicRecommendation(MiddlewareConfig config) {
        super(config, true);
    }

    @Override
    public boolean handle() {

        final Snowflake ownerId = this.getOwner().getId();
        if (this.getMessage().getContent().contains(this.getOwner().getMention())
                && !this.isPrivate()
                && this.getMessage().getMessageObject().getChannelId().equals(ChannelID.MUSIK) && this.getMessage().getUser() != null
                && !this.getMessage().getUser().getId().equals(ownerId)
                && this.getMemberPresence(ownerId, this.config.guildId).getStatus()
                        .compareTo(Status.ONLINE) != 0) {
            this.sendAnswer(
                    "Danke für deine Musikempfehlung! "+this.getOwner().asMember(this.config.guildId).block().getDisplayName()
                    +"ist aktuell beschäftigt, ich habe ihm aber die Nachricht weitergeschickt, damit er alles auf einem Blick hat!");

            String msg = Markdown.toBold(this.getMessage().getAuthorName()) + " hat dir eine Musikempfehlung hinterlassen!\n"
                    + Markdown.toSafeMultilineBlockQuotes(this.getMessage().getContent());

            this.sendMessageToOwner(msg);
        }
        return true;
    }

}
