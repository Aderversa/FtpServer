package com.aderversa;

import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class FtpServerConfig {
    @Getter
    @Setter
    Charset ftpCharset;
    Path path;
    Map<String, String> userMap;
    FtpServerConfig() {
        ftpCharset = StandardCharsets.UTF_8;
        userMap = new HashMap<>();
        path = Paths.get("./");
        setPath(path.toAbsolutePath().getParent());
    }

    public void addUserMap(String username, String password) {
        userMap.put(username, password);
    }

    public String getPassword(String username) {
        return userMap.get(username);
    }

    public Path getPath() {
        return path.toAbsolutePath();
    }

    public void setPath(Path path) {
        if (path != null) {
            this.path = path;
        }
    }

    public Path getFileByName(String fileName) {
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path)) {
            for (Path file : directoryStream) {
                if (file.getFileName().toString().equals(fileName)) {
                    return file;
                }
            }
            return null;
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
