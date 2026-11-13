package xyz.destiall.sgcraftlink.modules.skyblock;

import io.th0rgal.oraxen.pack.upload.hosts.HostingProvider;
import org.bukkit.configuration.ConfigurationSection;
import org.json.JSONObject;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.UUID;

public class OraxenPackProvider implements HostingProvider {

    private final String apiKey;
    private final String uploadEndpoint;
    private String packURL;
    private byte[] sha1Bytes;
    private String sha1String;
    private UUID packUUID;

    public OraxenPackProvider(ConfigurationSection config) {
        this.apiKey = "1123SGCRAFTLINK_SPECIAL_UPLOAD_TOKEN3321";
        this.uploadEndpoint = "https://www.destial.xyz/";
    }

    @Override
    public boolean uploadPack(File resourcePack) {
        try {
            // Calculate SHA1
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] packBytes = Files.readAllBytes(resourcePack.toPath());
            this.sha1Bytes = sha1.digest(packBytes);
            this.sha1String = bytesToHex(sha1Bytes);
            this.packUUID = UUID.randomUUID();
            String url = uploadEndpoint + "pack?filename=" + resourcePack.getName();
            SGCraftLinkSpigot.getPlugin(SGCraftLinkSpigot.class).getLogger().info("Beginning upload pack to " + url);
            // Upload to your service
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/zip")
                    .POST(HttpRequest.BodyPublishers.ofFile(resourcePack.toPath()))
                    .build();
            ;
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 201) {
                SGCraftLinkSpigot.getPlugin(SGCraftLinkSpigot.class).getLogger().info("Uploaded Oraxen Pack");
                this.packURL = parseUploadResponse(response.body());
                return true;
            } else {
                SGCraftLinkSpigot.getPlugin(SGCraftLinkSpigot.class).getLogger().warning(response.body());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        SGCraftLinkSpigot.getPlugin(SGCraftLinkSpigot.class).getLogger().warning("Unable to upload pack");
        return false;
    }

    @Override
    public String getPackURL() {
        return packURL;
    }

    @Override
    public byte[] getSHA1() {
        return sha1Bytes;
    }

    @Override
    public String getOriginalSHA1() {
        return sha1String;
    }

    @Override
    public UUID getPackUUID() {
        return packUUID;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    private String parseUploadResponse(String response) {
        JSONObject json = new JSONObject(response);
        return uploadEndpoint + "public/zips/" + json.getString("filename");
    }
}
