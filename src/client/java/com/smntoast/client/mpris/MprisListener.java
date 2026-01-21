package com.smntoast.client.mpris;

import com.smntoast.client.SmnToastClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Cross-platform media listener that monitors music playback.
 * - Linux: Uses MPRIS via playerctl (supports native and Flatpak)
 * - Windows: Uses SMTC (System Media Transport Controls) via PowerShell
 */
public class MprisListener {
    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();
    private static final boolean IS_WINDOWS = OS_NAME.contains("win");
    private static final boolean IS_LINUX = OS_NAME.contains("nux") || OS_NAME.contains("nix");
    
    private Thread listenerThread;
    private volatile boolean running = false;
    private final AtomicReference<TrackInfo> currentTrack = new AtomicReference<>(null);
    private final boolean isFlatpak;
    
    public MprisListener() {
        // Detect if running inside a Flatpak sandbox (Linux only)
        if (IS_LINUX) {
            this.isFlatpak = System.getenv("FLATPAK_ID") != null || 
                             new java.io.File("/.flatpak-info").exists();
            if (isFlatpak) {
                SmnToastClient.LOGGER.info("Flatpak environment detected, using flatpak-spawn for host access");
            }
        } else {
            this.isFlatpak = false;
        }
        
        if (IS_WINDOWS) {
            SmnToastClient.LOGGER.info("Windows detected, using SMTC for media info");
        } else if (IS_LINUX) {
            SmnToastClient.LOGGER.info("Linux detected, using MPRIS/playerctl for media info");
        } else {
            SmnToastClient.LOGGER.warn("Unsupported OS: {}. Media detection may not work.", OS_NAME);
        }
    }
    
    public void start() {
        if (running) {
            return;
        }
        
        running = true;
        listenerThread = new Thread(this::pollMedia, "Media-Listener");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }
    
    public void stop() {
        running = false;
        if (listenerThread != null) {
            listenerThread.interrupt();
        }
    }
    
    private void pollMedia() {
        while (running) {
            try {
                TrackInfo track = fetchCurrentTrack();
                if (track != null) {
                    currentTrack.set(track);
                }
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                SmnToastClient.LOGGER.debug("Media poll error: {}", e.getMessage());
            }
        }
    }
    
    private TrackInfo fetchCurrentTrack() {
        if (IS_WINDOWS) {
            return fetchCurrentTrackWindows();
        } else if (IS_LINUX) {
            return fetchCurrentTrackLinux();
        }
        return null;
    }
    
    private TrackInfo fetchCurrentTrackLinux() {
        try {
            String status = executeLinuxCommand("playerctl", "status");
            if (status == null || !status.trim().equalsIgnoreCase("Playing")) {
                return new TrackInfo("", "", "", "", false);
            }
            
            String title = executeLinuxCommand("playerctl", "metadata", "title");
            String artist = executeLinuxCommand("playerctl", "metadata", "artist");
            String album = executeLinuxCommand("playerctl", "metadata", "album");
            String trackId = executeLinuxCommand("playerctl", "metadata", "mpris:trackid");
            
            if (title == null || title.isEmpty()) {
                return null;
            }
            
            if (trackId == null || trackId.isEmpty()) {
                trackId = (title + "-" + artist).hashCode() + "";
            }
            
            return new TrackInfo(
                trackId.trim(),
                title != null ? title.trim() : "Unknown",
                artist != null ? artist.trim() : "Unknown Artist",
                album != null ? album.trim() : "",
                true
            );
        } catch (Exception e) {
            SmnToastClient.LOGGER.debug("Error fetching MPRIS metadata: {}", e.getMessage());
            return null;
        }
    }
    
    private TrackInfo fetchCurrentTrackWindows() {
        try {
            String script = 
                "Add-Type -AssemblyName System.Runtime.WindowsRuntime;" +
                "$asTaskGeneric = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' })[0];" +
                "Function Await($WinRtTask, $ResultType) { $asTask = $asTaskGeneric.MakeGenericMethod($ResultType); $netTask = $asTask.Invoke($null, @($WinRtTask)); $netTask.Wait(-1) | Out-Null; $netTask.Result };" +
                "$null = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media.Control, ContentType = WindowsRuntime];" +
                "$manager = Await ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]);" +
                "$session = $manager.GetCurrentSession();" +
                "if ($session) {" +
                "  $null = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties, Windows.Media.Control, ContentType = WindowsRuntime];" +
                "  $info = $session.GetPlaybackInfo();" +
                "  $status = $info.PlaybackStatus;" +
                "  if ($status -eq 'Playing') {" +
                "    $props = Await ($session.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties]);" +
                "    Write-Output 'STATUS:Playing';" +
                "    Write-Output ('ARTIST:' + $props.Artist);" +
                "    Write-Output ('TITLE:' + $props.Title);" +
                "    Write-Output ('ALBUM:' + $props.AlbumTitle);" +
                "  } else {" +
                "    Write-Output 'STATUS:Paused';" +
                "  }" +
                "} else {" +
                "  Write-Output 'STATUS:NoSession';" +
                "}";
            
            List<String> output = executeWindowsCommand(script);
            if (output == null || output.isEmpty()) {
                return null;
            }
            
            String status = null;
            String artist = "";
            String title = "";
            String album = "";
            
            for (String line : output) {
                if (line.startsWith("STATUS:")) {
                    status = line.substring(7);
                } else if (line.startsWith("ARTIST:")) {
                    artist = line.substring(7);
                } else if (line.startsWith("TITLE:")) {
                    title = line.substring(6);
                } else if (line.startsWith("ALBUM:")) {
                    album = line.substring(6);
                }
            }
            
            if (!"Playing".equals(status)) {
                return new TrackInfo("", "", "", "", false);
            }
            
            if (title == null || title.isEmpty()) {
                return null;
            }
            
            String trackId = (title + "-" + artist).hashCode() + "";
            
            return new TrackInfo(
                trackId,
                title.trim(),
                artist != null && !artist.isEmpty() ? artist.trim() : "Unknown Artist",
                album != null ? album.trim() : "",
                true
            );
        } catch (Exception e) {
            SmnToastClient.LOGGER.debug("Error fetching SMTC metadata: {}", e.getMessage());
            return null;
        }
    }
    
    private List<String> executeWindowsCommand(String script) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "powershell.exe",
                "-NoProfile",
                "-NonInteractive", 
                "-ExecutionPolicy", "Bypass",
                "-Command", script
            );
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
                SmnToastClient.LOGGER.debug("PowerShell returned exit code {}", exitCode);
                return null;
            }
            
            return lines;
        } catch (Exception e) {
            SmnToastClient.LOGGER.debug("PowerShell exception: {}", e.getMessage());
            return null;
        }
    }
    
    private String executeLinuxCommand(String... command) {
        try {
            String[] actualCommand;
            
            if (isFlatpak) {
                actualCommand = new String[command.length + 2];
                actualCommand[0] = "flatpak-spawn";
                actualCommand[1] = "--host";
                System.arraycopy(command, 0, actualCommand, 2, command.length);
            } else {
                actualCommand = command;
            }
            
            ProcessBuilder pb = new ProcessBuilder(actualCommand);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
            
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                SmnToastClient.LOGGER.debug("Command returned exit code {}: {}", 
                    exitCode, String.join(" ", actualCommand));
                return null;
            }
            
            return output.toString();
        } catch (Exception e) {
            SmnToastClient.LOGGER.debug("Command exception: {}", e.getMessage());
            return null;
        }
    }
    
    public TrackInfo getCurrentTrack() {
        return currentTrack.get();
    }
    
    public static class TrackInfo {
        private final String trackId;
        private final String title;
        private final String artist;
        private final String album;
        private final boolean playing;
        
        public TrackInfo(String trackId, String title, String artist, String album, boolean playing) {
            this.trackId = trackId;
            this.title = title;
            this.artist = artist;
            this.album = album;
            this.playing = playing;
        }
        
        public String getTrackId() {
            return trackId;
        }
        
        public String getTitle() {
            return title;
        }
        
        public String getArtist() {
            return artist;
        }
        
        public String getAlbum() {
            return album;
        }
        
        public boolean isPlaying() {
            return playing;
        }
    }
}
