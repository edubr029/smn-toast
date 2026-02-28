package com.smntoast.client.media;

import com.smntoast.client.SmnToastClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class CommandRunner {
    public static List<String> runCommand(String... command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                SmnToastClient.LOGGER.debug("Command returned exit code {}", exitCode);
                return new ArrayList<>();
            }

            return lines;
        } catch (Exception e) {
            SmnToastClient.LOGGER.debug("Command exception: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}
