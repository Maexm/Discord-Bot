package system;

import security.SecurityLevel;
import util.Help;

public class HelpSection extends Middleware {

    public HelpSection(MiddlewareConfig config) {
        super(config, false);
    }

    @Override
    protected boolean handle() {
        switch(this.getMessage().getContent().toLowerCase().replace("hilfe", "help")){
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
