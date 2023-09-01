package xyz.destiall.sgcraftlink;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SGCraftLinkCommon {
    public static final String CHANNEL = "sgc:link";

    public static final String GET_RANKS = "SGCGetRanks";
    public static final String SET_RANKS = "SGCSetRanks";
    public static final String GET_FIRSTJOIN = "SGCGetJoined";
    public static final String SET_FIRSTJOIN = "SGCSetJoined";
    public static final String SET_JAIL = "SGCSetJail";
    public static final String GET_JAIL = "SGCGetJail";
    public static final String BIRTHDAY = "SGCBirthday";

    public static final String DATA_SERVER_REGISTER = "SGC_DATA_SERVER_REGISTER";
    public static final String DATA_SERVER_UNREGISTER = "SGC_DATA_SERVER_UNREGISTER";
    public static final String DATA_CHATS = "SGC_DATA_CHATS";
    public static final String DATA_EXECUTE_COMMAND = "SGC_DATA_EXECUTE_COMMAND";
    public static final String DATA_RANKS = "SGC_DATA_RANKS";

    public static Map<String, Object> createCommandsData(String... commands) {
        Map<String, Object> data = new HashMap<>();
        data.put("commands", Arrays.asList(commands));
        return data;
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
