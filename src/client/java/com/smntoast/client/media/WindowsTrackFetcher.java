package com.smntoast.client.media;

import com.smntoast.client.SmnToastClient;

public class WindowsTrackFetcher implements TrackFetcher {
    @Override
    public TrackInfo fetchCurrentTrack() {
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

            return TrackInfoParser.parseTrackInfo(CommandRunner.runCommand(
                    "powershell.exe",
                    "-NoProfile",
                    "-NonInteractive",
                    "-ExecutionPolicy", "Bypass",
                    "-Command", script));
        } catch (Exception e) {
            SmnToastClient.LOGGER.debug("Error fetching SMTC metadata: {}", e.getMessage());
            return null;
        }
    }
}
