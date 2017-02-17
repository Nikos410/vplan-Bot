package nikos.vplanbot.download;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;

public class FileDownloader {
    
    public static Path download(URL fileUrl, Path savePath) throws IOException {
        savePath = savePath.toAbsolutePath();
        URL url = fileUrl;

        ReadableByteChannel rbc = Channels.newChannel(url.openStream());
        FileOutputStream fos = new FileOutputStream(savePath.toString());
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        fos.close();
        return savePath;
    }
}
