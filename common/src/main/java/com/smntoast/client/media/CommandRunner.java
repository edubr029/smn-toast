package com.smntoast.client.media;

import com.smntoast.SmnToast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CommandRunner {
    private static final long TIMEOUT_MS = 5000L;

    public static List<String> runCommand(String... command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            if (!process.waitFor(TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                SmnToast.LOGGER.debug("Command timed out after {}ms: {}", TIMEOUT_MS, String.join(" ", command));
                return new ArrayList<>();
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                SmnToast.LOGGER.debug("Command returned exit code {}", exitCode);
                return new ArrayList<>();
            }

            return lines;
        } catch (Exception e) {
            SmnToast.LOGGER.debug("Command exception: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}
