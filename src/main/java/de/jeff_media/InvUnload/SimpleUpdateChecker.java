package de.jeff_media.InvUnload;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleUpdateChecker {

    private static final Pattern VERSION_PART_PATTERN = Pattern.compile("\\d+");

    private final JavaPlugin plugin;
    private final String versionUrl;
    private final HttpClient httpClient;

    private boolean suppressUpToDateMessage;
    private String donationLink;
    private String downloadLink;
    private String changelogLink;
    private BukkitTask repeatingTask;

    public SimpleUpdateChecker(JavaPlugin plugin, String versionUrl) {
        this.plugin = plugin;
        this.versionUrl = versionUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public SimpleUpdateChecker suppressUpToDateMessage(boolean suppressUpToDateMessage) {
        this.suppressUpToDateMessage = suppressUpToDateMessage;
        return this;
    }

    public SimpleUpdateChecker setDonationLink(String donationLink) {
        this.donationLink = donationLink;
        return this;
    }

    public SimpleUpdateChecker setDownloadLink(int spigotResourceId) {
        return setDownloadLink("https://www.spigotmc.org/resources/" + spigotResourceId);
    }

    public SimpleUpdateChecker setDownloadLink(String downloadLink) {
        this.downloadLink = downloadLink;
        return this;
    }

    public SimpleUpdateChecker setChangelogLink(int spigotResourceId) {
        return setChangelogLink("https://www.spigotmc.org/resources/" + spigotResourceId + "/updates");
    }

    public SimpleUpdateChecker setChangelogLink(String changelogLink) {
        this.changelogLink = changelogLink;
        return this;
    }

    public SimpleUpdateChecker checkNow() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::checkForUpdates);
        return this;
    }

    public SimpleUpdateChecker checkEveryXHours(double hours) {
        stop();
        long periodTicks = Math.max(1L, Math.round(hours * 60 * 60 * 20));
        repeatingTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::checkForUpdates, periodTicks, periodTicks);
        return this;
    }

    public void stop() {
        if(repeatingTask != null) {
            repeatingTask.cancel();
            repeatingTask = null;
        }
    }

    private void checkForUpdates() {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(versionUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", plugin.getName() + "/" + plugin.getDescription().getVersion())
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if(response.statusCode() < 200 || response.statusCode() >= 300) {
                return;
            }

            String latestVersion = response.body().trim();
            if(latestVersion.isEmpty()) {
                return;
            }

            String currentVersion = plugin.getDescription().getVersion();
            if(isNewerVersion(latestVersion, currentVersion)) {
                plugin.getLogger().warning("A newer version of " + plugin.getName() + " is available: " + latestVersion + " (current: " + currentVersion + ")");
                if(downloadLink != null) {
                    plugin.getLogger().warning("Download: " + downloadLink);
                }
                if(changelogLink != null) {
                    plugin.getLogger().warning("Changelog: " + changelogLink);
                }
                if(donationLink != null) {
                    plugin.getLogger().warning("Donate: " + donationLink);
                }
            } else if(!suppressUpToDateMessage) {
                plugin.getLogger().info(plugin.getName() + " is up to date (" + currentVersion + ").");
            }
        } catch (IOException | InterruptedException ignored) {
            if(ignored instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private boolean isNewerVersion(String latestVersion, String currentVersion) {
        List<Integer> latestParts = getVersionParts(latestVersion);
        List<Integer> currentParts = getVersionParts(currentVersion);
        int partCount = Math.max(latestParts.size(), currentParts.size());
        for(int i = 0; i < partCount; i++) {
            int latest = i < latestParts.size() ? latestParts.get(i) : 0;
            int current = i < currentParts.size() ? currentParts.get(i) : 0;
            if(latest > current) return true;
            if(latest < current) return false;
        }
        return false;
    }

    private List<Integer> getVersionParts(String version) {
        Matcher matcher = VERSION_PART_PATTERN.matcher(version);
        List<Integer> parts = new ArrayList<>();
        while(matcher.find()) {
            parts.add(Integer.parseInt(matcher.group()));
        }
        if(parts.isEmpty()) {
            parts.add(0);
        }
        return parts;
    }
}
