package util;

import start.RuntimeVariables;

public class Help {

	public final static String HELPTEXT = RuntimeVariables.getInstance().getHelpText()
			+ "**VERSION "+RuntimeVariables.getInstance().getVersion()+" "+(RuntimeVariables.isDebug() ? "(EXPERIMENTELL)":"")+"**"
			+ "\n"
			+ "Fragen, Anregungen und Vorschläge können gerne persönlich, mit `MegFeedback Dein Feedback` oder auf GitHub gestellt werden:\n"
			+ RuntimeVariables.getInstance().getGitUrl();
	
	public final static String MUSICHELPTEXT = RuntimeVariables.getInstance().getMusicHelpText();
	
	public final static String ADMHELP = RuntimeVariables.getInstance().getAdmHelpText();
	
	public final static String SURVEYHELPTEXT = RuntimeVariables.getInstance().getSurveyHelpText();
}
