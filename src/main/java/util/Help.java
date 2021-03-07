package util;

import start.RuntimeVariables;

public class Help {

	public final static String HELPTEXT() {
			return RuntimeVariables.getInstance().getHelpText()
			+ "**VERSION "+RuntimeVariables.getInstance().getVersion()+" "+(RuntimeVariables.isDebug() ? "(EXPERIMENTELL)":"")+"**"
			+ "\n"
			+ "Fragen, Anregungen und Vorschläge können gerne persönlich, mit `MegFeedback Dein Feedback` oder auf GitHub gestellt werden:\n"
			+ RuntimeVariables.getInstance().getGitUrl();
	}
	
	public final static String MUSICHELPTEXT() {
		return RuntimeVariables.getInstance().getMusicHelpText();
	}
	
	public final static String ADMHELP(){
		return RuntimeVariables.getInstance().getAdmHelpText();
	}
	
	public final static String SURVEYHELPTEXT() {
		return RuntimeVariables.getInstance().getSurveyHelpText();
	}
}
