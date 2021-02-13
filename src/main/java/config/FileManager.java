package config;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;

public class FileManager {

    public static String read(File file) {
        try {
            return FileUtils.readFileToString(file, Charset.defaultCharset());
        } catch (IOException e) {
            return null;
        }
    }

    public static boolean write(File file, String content){
        try {
            FileUtils.writeStringToFile(file, content, Charset.defaultCharset());
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
