package org.homicideware.stealthrabbit.utils.contract;

import android.os.Build;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

import org.homicideware.stealthrabbit.BuildConfig;

public class Web {

    public Web() {

    }

    public String getContent(String link) throws IOException {

        String result;

        URL url = new URL(link);
        URLConnection connection = url.openConnection();
        connection.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android " + Build.VERSION.RELEASE + "; " + Build.DEVICE + ")" +
                        " AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.131 Mobile Safari/537.36"
                        + " StealthRabbit/" + BuildConfig.VERSION_CODE);
        connection.connect();

        BufferedReader r = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) {
            sb.append(line).append("\n");
        }

        result = sb.toString();
        result = result.substring(0, result.length() - 1);
        return result;
    }
}
