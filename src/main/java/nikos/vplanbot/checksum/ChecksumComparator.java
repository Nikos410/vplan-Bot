package nikos.vplanbot.checksum;

import java.util.Arrays;
import java.util.List;

public class ChecksumComparator {
    public static boolean compareChecksums(final List<byte[]> checksums) {
        final int length = checksums.size();
        for (int i = 0; i < length - 1; ++i) {
            if(!Arrays.equals(checksums.get(i), checksums.get(i+1))) {
                return false;
            }
        }
        return true;
    }
}
