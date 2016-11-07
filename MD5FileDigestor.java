package org.nikos.vplanbot.org.nikos.vplanbot.checksum;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;

public class MD5FileDigestor {
    private static final MessageDigest md;

    static {
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Could not find message digest algorithm", e);
        }
    }

    public static List<byte[]> digestFiles(final List<Path> filePaths) {
        return filePaths.stream()
                .map(MD5FileDigestor::readFileContent)
                .map(md::digest)
                .collect(Collectors.toList());
    }

    private static byte[] readFileContent(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException("Could not read file at path " + path, e);
        }
    }

}
