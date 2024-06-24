package com.aderversa;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PathTest {
    @Test
    public void test1() {
        String name = "Desktop";
        Path path = Paths.get("D:/");
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path)) {
            for (Path file : directoryStream) {
                String filename = file.getFileName().toString();
                if (filename.equals(name)) {
                    path = file;
                }
            }
            System.out.println(path.toAbsolutePath());
            path = path.getRoot();
            System.out.println(path.toAbsolutePath());
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
