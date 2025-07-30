package cc.kasumi.practice.util;

import java.util.regex.Pattern;

public class UUIDUtil {

    private final static Pattern UUID_REGEX_PATTERN =
            Pattern.compile("^[{]?[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}[}]?$");

    public static boolean isValidUUID(String string) {
        if (string == null) {
            return false;
        }

        return UUID_REGEX_PATTERN.matcher(string).matches();
    }
}
