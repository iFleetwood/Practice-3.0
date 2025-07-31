package cc.kasumi.practice.nametag;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NametagContent {

    private final String prefix;
    private final String suffix;

    public NametagContent(String prefix) {
        this(prefix, "");
    }

    /**
     * Create empty nametag content
     */
    public static NametagContent empty() {
        return new NametagContent("", "");
    }

    /**
     * Create nametag with only prefix
     */
    public static NametagContent withPrefix(String prefix) {
        return new NametagContent(prefix, "");
    }

    /**
     * Create nametag with only suffix
     */
    public static NametagContent withSuffix(String suffix) {
        return new NametagContent("", suffix);
    }

    /**
     * Truncate prefix/suffix to fit nametag limits
     */
    public NametagContent truncate() {
        String truncatedPrefix = prefix.length() > 16 ? prefix.substring(0, 16) : prefix;
        String truncatedSuffix = suffix.length() > 16 ? suffix.substring(0, 16) : suffix;
        return new NametagContent(truncatedPrefix, truncatedSuffix);
    }
}