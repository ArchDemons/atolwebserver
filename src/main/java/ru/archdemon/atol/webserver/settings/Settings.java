package ru.archdemon.atol.webserver.settings;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import ru.archdemon.atol.webserver.Utils;

public class Settings {

    public static String fileName() {
        String name = System.getProperty("settings.file");
        if (name == null) {
            name = Thread.currentThread().getContextClassLoader().getResource("settings.json").getFile();
        }
        try {
            return (new URI(name)).getPath();
        } catch (URISyntaxException e) {
            return "";
        }
    }

    public static JSONObject load() throws IOException, ParseException {
        JSONParser parser = new JSONParser();
        return (JSONObject) parser.parse(Utils.readFromReader(new BufferedReader(new FileReader(fileName()))));
    }

    public static void save(JSONObject settings) throws IOException {
        try (Writer w = new FileWriter(fileName())) {
            w.write(settings.toJSONString());
        }
    }
}
