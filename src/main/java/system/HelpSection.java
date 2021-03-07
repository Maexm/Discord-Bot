package system;

import security.SecurityLevel;
import start.RuntimeVariables;
import util.Help;

public class HelpSection extends Middleware {

    public HelpSection(MiddlewareConfig config) {
        super(config, false);
    }

    @Override
    protected boolean handle() {
        final String prefix = RuntimeVariables.getInstance().getCommandPrefix().toLowerCase();
        switch(this.getMessage().getContent().toLowerCase().replace("hilfe", "help").replace(prefix, "")){
            // Help section (and ONLY the help section) should be fool proofed
            case "h":
            case "help":
            case "help!":
            case "'help!'":
            case "'help'!":
            case "'help'":
            case "'help":
            case "'help!":
            case "help'":
            case "help!'":
            case "help'!":
            case "\"help\"!":
            case "\"help!\"":
            case "\"help\"":
            case "\"help":
            case "\"help!":
            case "help\"":
            case "help!\"":
            case "help\"!":

            case " help":
            case " help!":
            case "' help!'":
            case "' help'!":
            case "' help'":
            case "' help":
            case "' help!":
            case " help'":
            case " help!'":
            case " help'!":
            case "\" help\"!":
            case "\" help!\"":
            case "\" help\"":
            case "\" help":
            case "\" help!":
            case " help\"":
            case " help!\"":
            case " help\"!":

            case " hilfe":
            case " hilfe!":
            case "' hilfe!'":
            case "' hilfe'!":
            case "' hilfe'":
            case "' hilfe":
            case "' hilfe!":
            case " hilfe'":
            case " hilfe!'":
            case " hilfe'!":
            case "\" hilfe\"!":
            case "\" hilfe!\"":
            case "\" hilfe\"":
            case "\" hilfe":
            case "\" hilfe!":
            case " hilfe\"":
            case " hilfe!\"":
            case " hilfe\"!":
                this.help();
                break;
            default:
                    // Nothing
        }
        return true;
    }

    private void help(){
        this.sendPrivateAnswer(Help.HELPTEXT());
		if (this.hasPermission(SecurityLevel.ADM)) {
			this.sendPrivateAnswer(Help.ADMHELP());
		}
    }
    
}
