package org.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ShaderUtils {

    public static String loadShaderSource(String filePath) {
        try {
            return new String(Files.readAllBytes(Paths.get(filePath)));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
