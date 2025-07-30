package cc.kasumi.practice.util;

public class StringUtil {

    public static String toLowerCaseFirstUpper(String string) {
        return string.substring(0, 1).toUpperCase() + string.substring(1).toLowerCase();
    }

    public static String toLowerCaseFirstUpper(String[] strings) {
        StringBuilder builder = new StringBuilder();

        int length = strings.length;
        int i = 1;
        for (String string : strings) {
            if (length > i++) {
                builder.append(toLowerCaseFirstUpper(string)).append(" ");

                continue;
            }

            builder.append(toLowerCaseFirstUpper(string));
        }

        return builder.toString();
    }
}
