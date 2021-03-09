package config;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;

public class FileManager {

    /**
     * Reads and returns the content of a file. Catches all exceptions
     * @param file
     * @return File content as a string. Null if an exception was thrown
     */
    public static String read(File file) {
        try {
            System.out.println("Reading from file: "+file.getAbsolutePath());
            return FileUtils.readFileToString(file, Charset.forName("UTF-8"));
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Writes string content to a file, creates a new file, if file does not exist
     * @param file
     * @param content
     * @return true if write was successfull, false if an excpetion was thrown
     */
    public static boolean write(File file, String content){
        try {
            System.out.println("Saved content to file: "+file.getAbsolutePath());
            FileUtils.writeStringToFile(file, content, Charset.forName("UTF-8"));
            System.out.println("Content saved successfully!");
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
