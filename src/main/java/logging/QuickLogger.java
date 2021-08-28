package logging;

import start.RuntimeVariables;

public final class QuickLogger extends BasicLogger {

    private final static QuickLogger instance = new QuickLogger();

    private QuickLogger() {
        super();
    }

    public final static void logWarn(final String text){
        QuickLogger.instance.log(text, LogType.WARN);
    }

    public final static void logErr(final String text){
        QuickLogger.instance.log(text, LogType.ERR);
    }

    public final static void logMinErr(final String text){
        QuickLogger.instance.log(text, LogType.MINOR_ERR);
    }

    public final static void logFatalErr(final String text){
        QuickLogger.instance.log(text, LogType.FATAL_ERR);
    }

    public final static void logDebug(final String text){
        if(RuntimeVariables.isDebug()){
            QuickLogger.instance.log(text, LogType.DEBUG);
        }
    }

    public final static void logfeedback(final String text){
        QuickLogger.instance.log(text, LogType.FEEDBACK);
    }

    public final static void logInfo(final String text){
        QuickLogger.instance.log(text, LogType.INFO);
    }
}
