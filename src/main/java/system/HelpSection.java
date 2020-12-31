package system;

import java.util.ArrayList;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.voice.AudioProvider;
import musicBot.AudioEventHandler;
import security.SecurityLevel;
import util.Help;
import survey.Survey;

public class HelpSection extends Middleware {

    public HelpSection(Snowflake guildId, GatewayDiscordClient client, AudioProvider audioProvider,
            ArrayList<Survey> surveys, AudioEventHandler audioEventHandler) {
        super(guildId, client, audioProvider, surveys, audioEventHandler);
    }

    @Override
    protected boolean handle() {
        switch(this.msgContent.toLowerCase()){
            // Help section (and ONLY the help section) should be fool proofed
            case "megh":
            case "meghelp":
            case "meghelp!":
            case "'meghelp!'":
            case "'meghelp'!":
            case "'meghelp'":
            case "'meghelp":
            case "'meghelp!":
            case "meghelp'":
            case "meghelp!'":
            case "meghelp'!":
            case "\"meghelp\"!":
            case "\"meghelp!\"":
            case "\"meghelp\"":
            case "\"meghelp":
            case "\"meghelp!":
            case "meghelp\"":
            case "meghelp!\"":
            case "meghelp\"!":

            case "meg help":
            case "meg help!":
            case "'meg help!'":
            case "'meg help'!":
            case "'meg help'":
            case "'meg help":
            case "'meg help!":
            case "meg help'":
            case "meg help!'":
            case "meg help'!":
            case "\"meg help\"!":
            case "\"meg help!\"":
            case "\"meg help\"":
            case "\"meg help":
            case "\"meg help!":
            case "meg help\"":
            case "meg help!\"":
            case "meg help\"!":
                this.help();
                break;
            default:
                    // Nothing
        }
        return true;
    }

    private void help(){
        this.sendPrivateAnswer(Help.HELPTEXT);
		if (this.hasPermission(SecurityLevel.ADM)) {
			this.sendPrivateAnswer(Help.ADMHELP);
		}
    }
    
}
