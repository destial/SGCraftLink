package xyz.destiall.sgcraftlink;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class SGCraftLinkCommon {
    public static final String CHANNEL = "sgc:link";
    public static final DateTimeFormatter format = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy h:mm a Z").withZone(ZoneId.systemDefault());

    public static final String GET_FIRSTJOIN = "SGCGetJoined";
    public static final String SET_FIRSTJOIN = "SGCSetJoined";

    public static final String DATA_SERVER_REGISTER = "SGC_DATA_SERVER_REGISTER";
    public static final String DATA_SERVER_UNREGISTER = "SGC_DATA_SERVER_UNREGISTER";
    public static final String DATA_CHATS = "SGC_DATA_CHATS";
    public static final String DATA_EXECUTE_COMMAND = "SGC_DATA_EXECUTE_COMMAND";
    public static final String DATA_GET_HEADHUNT = "SGC_DATA_GET_HEADHUNT";
    public static final String DATA_START_HEADHUNT = "SGC_DATA_START_HEADHUNT";
    public static final String DATA_END_HEADHUNT = "SGC_DATA_END_HEADHUNT";
    public static final String DATA_CLEAR_SCORE_HEADHUNT = "SGC_DATA_CLEAR_HEADHUNT";
    public static final String DATA_REMOVE_HEAD_HEADHUNT = "SGC_DATA_REMOVE_HEADHUNT";
    public static final String DATA_BIRTHDAY = "SGC_DATA_BIRTHDAY";
    public static final String DATA_LUCKPERMS_SERVICE = "SGC_LUCKPERMS_SERVICE";
    public static final String DATA_PLAYER_MIGRATE = "SGC_MIGRATE_PLAYER";
    public static final String DATA_SECURITY_CHECK = "SGC_SECURITY_CHECK";
    public static final String DATA_GET_RANKS = "SGC_GET_RANKS";
    public static final String DATA_JAILED = "SGC_JAILED_EVENT";
    public static final String DATA_LINK_DISCORDSRV = "SGC_LINK_DISCORDSRV";
    public static final String DATA_UNLINK_DISCORDSRV = "SGC_UNLINK_DISCORDSRV";

    public static final String[] BLACKLISTED_PERMISSIONS = {"*", "luckperms", "owner", "admin"};

    private static final Map<String, Object> emptyMap = Collections.emptyMap();

    public static Map<String, Object> createCommandsData(String... commands) {
        Map<String, Object> data = new HashMap<>();
        data.put("commands", Arrays.asList(commands));
        return data;
    }

    public static Map<String, Object> createDataMap(String first, Object second, Object... others) {
        Map<String, Object> data = new HashMap<>();
        data.put(first, second);
        if (others != null) {
            for (int i = 0; i < others.length; i += 2) {
                data.put(String.valueOf(others[i]), others[i + 1]);
            }
        }
        return data;
    }

    public static Map<String, Object> emptyDataMap() {
        return emptyMap;
    }

    public static String formatDate(Date date) {
        return format.format(date.toInstant());
    }

    public static String relativeDate(long end) {
        long start = System.currentTimeMillis();
        // Calculate time difference
        // in milliseconds
        long difference_In_Time = end - start;

        String format = "";


        long difference_In_Days
                = (difference_In_Time
                / (1000L * 60 * 60 * 24))
                % 365;

        if (difference_In_Days > 0) {
            format += difference_In_Days + " days ";
        }

        long difference_In_Hours
                = (difference_In_Time
                / (1000L * 60 * 60))
                % 24;

        if (difference_In_Hours > 0) {
            format += difference_In_Hours + " hours ";
        }

        long difference_In_Minutes
                = (difference_In_Time
                / (1000L * 60))
                % 60;

        if (difference_In_Minutes > 0) {
            format += difference_In_Minutes + " mins ";
        }

        long difference_In_Seconds
                = (difference_In_Time
                / 1000L)
                % 60;

        if (difference_In_Seconds > 0) {
            format += difference_In_Seconds + " s";
        }

        return format.trim();
    }

    public static String convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
}
