package logging;

import java.util.Calendar;

import start.RuntimeVariables;

public abstract class BasicLogger {

    public final void log(String text, LogType type) {
        Class<?> caller = null;
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        int lineNumber = -1;
        // 0 = current thread
        for (int i = 1; i < stackTraceElements.length; i++) {
            Class<?> elementClass;
            try {
                elementClass = Class.forName(stackTraceElements[i].getClassName());
                if (!BasicLogger.class.isAssignableFrom(elementClass)) {
                    caller = Class.forName(stackTraceElements[i].getClassName());
                    lineNumber = stackTraceElements[i].getLineNumber();
                    break;
                }
            } catch (ClassNotFoundException e) {
                break;
            }
        }

        Calendar time = Calendar.getInstance(RuntimeVariables.getInstance().getTimezone());

        System.out.println("[" + time.getTime() + "][" + type + "][" + (caller == null ? "CLASSNAME ERR"
                : caller.getSimpleName()) + " l:"+lineNumber+"] - " + text);
    }
}
