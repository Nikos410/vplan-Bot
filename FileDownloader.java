package org.nikos.vplanbot.org.nikos.vplanbot.download;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;

public class FileDownloader {
    private final URL fileUrl;
    private final Path savePath;

    public FileDownloader(final URL fileUrl, final Path savePath) {
        this.fileUrl = fileUrl;
        this.savePath = savePath.toAbsolutePath();
    }

    public Path download() throws IOException {
        URL url = this.fileUrl;
        ReadableByteChannel rbc = Channels.newChannel(url.openStream());
        FileOutputStream fos = new FileOutputStream(this.savePath.toString());
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        fos.close();
        return this.savePath;
    }
}
