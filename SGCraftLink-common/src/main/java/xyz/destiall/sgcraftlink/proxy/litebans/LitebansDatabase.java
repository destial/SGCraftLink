package xyz.destiall.sgcraftlink.proxy.litebans;

import litebans.api.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class LitebansDatabase {
    public static String getPlayerName(String uuid) {
        String query = "SELECT name FROM {history} WHERE uuid=? ORDER BY date DESC LIMIT 1";
        try (PreparedStatement st = Database.get().prepareStatement(query)) {
            st.setString(1, uuid);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("name");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static long getTotalBans() {
        String query = "SELECT COUNT(*) FROM {bans}";
        try (PreparedStatement st = Database.get().prepareStatement(query)) {
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static long getTotalWarns() {
        String query = "SELECT COUNT(*) FROM {warnings}";
        try (PreparedStatement st = Database.get().prepareStatement(query)) {
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static long getTotalKicks() {
        String query = "SELECT COUNT(*) FROM {kicks}";
        try (PreparedStatement st = Database.get().prepareStatement(query)) {
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static long getTotalMutes() {
        String query = "SELECT COUNT(*) FROM {mutes}";
        try (PreparedStatement st = Database.get().prepareStatement(query)) {
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static UUID getUUID(String name) {
        String query = "SELECT name,uuid FROM {history}";
        try (PreparedStatement st = Database.get().prepareStatement(query)) {
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    if (name.equalsIgnoreCase(rs.getString("name"))) {
                        String uuid = rs.getString("uuid");
                        if (uuid.equalsIgnoreCase("console"))
                            continue;

                        return UUID.fromString(uuid);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static long getFirstJoin(String name) {
        String query = "SELECT name,date FROM {history}";
        try (PreparedStatement st = Database.get().prepareStatement(query)) {
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    if (name.equalsIgnoreCase(rs.getString("name"))) {
                        Timestamp timestamp = rs.getTimestamp("date");
                        return timestamp.getTime();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Long.MAX_VALUE;
    }

    public static List<Punishment> getHistory(String uuid) {
        List<Punishment> history = new ArrayList<>();
        String query = "SELECT * FROM {bans}";
        try (PreparedStatement st = Database.get().prepareStatement(query)) {
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    if (rs.getString("uuid").equalsIgnoreCase(uuid)) {
                        Punishment p = new Punishment(rs.getBoolean("ipban") ? PunishmentType.IPBAN : PunishmentType.BAN, getPlayerName(uuid), rs.getString("banned_by_name"), rs.getLong("time"), rs.getLong("until"), rs.getString("reason"), rs.getBoolean("active"), rs.getString("server_origin"));
                        history.add(p);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        query = "SELECT * FROM {mutes}";
        try (PreparedStatement st = Database.get().prepareStatement(query)) {
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    if (rs.getString("uuid").equalsIgnoreCase(uuid)) {
                        Punishment p = new Punishment(PunishmentType.MUTE, getPlayerName(uuid), rs.getString("banned_by_name"), rs.getLong("time"), rs.getLong("until"), rs.getString("reason"), rs.getBoolean("active"), rs.getString("server_origin"));
                        history.add(p);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        query = "SELECT * FROM {warnings}";
        try (PreparedStatement st = Database.get().prepareStatement(query)) {
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    if (rs.getString("uuid").equalsIgnoreCase(uuid)) {
                        Punishment p = new Punishment(PunishmentType.WARN, getPlayerName(uuid), rs.getString("banned_by_name"), rs.getLong("time"), rs.getLong("until"), rs.getString("reason"), rs.getBoolean("active"), rs.getString("server_origin"));
                        history.add(p);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        query = "SELECT * FROM {kicks}";
        try (PreparedStatement st = Database.get().prepareStatement(query)) {
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    if (rs.getString("uuid").equalsIgnoreCase(uuid)) {
                        Punishment p = new Punishment(PunishmentType.KICK, getPlayerName(uuid), rs.getString("banned_by_name"), rs.getLong("time"), rs.getLong("until"), rs.getString("reason"), rs.getBoolean("active"), rs.getString("server_origin"));
                        history.add(p);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        history.sort(Comparator.comparingLong(Punishment::getTime).reversed());
        return history;
    }

    public static Set<String> getUUIDAccounts(String uuid) {
        String query = "SELECT ip FROM {history} WHERE uuid=?";
        Set<String> ips = new HashSet<>();
        try (PreparedStatement st = Database.get().prepareStatement(query)) {
            st.setString(1, uuid);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    ips.add(rs.getString("ip"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        Set<String> uuids = new HashSet<>();
        query = "SELECT uuid FROM {history} WHERE ip=?";
        for (String ip : ips) {
            try (PreparedStatement st = Database.get().prepareStatement(query)) {
                st.setString(1, ip);
                try (ResultSet rs = st.executeQuery()) {
                    while (rs.next()) {
                        uuids.add(rs.getString("uuid"));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return uuids;
    }

    public static Set<String> getAlts(String uuid) {
        String query = "SELECT ip FROM {history} WHERE uuid=?";
        Set<String> ips = new HashSet<>();
        try (PreparedStatement st = Database.get().prepareStatement(query)) {
            st.setString(1, uuid);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    ips.add(rs.getString("ip"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        Set<String> ualts = new HashSet<>();
        query = "SELECT name FROM {history} WHERE ip=?";
        for (String ip : ips) {
            try (PreparedStatement st = Database.get().prepareStatement(query)) {
                st.setString(1, ip);
                try (ResultSet rs = st.executeQuery()) {
                    while (rs.next()) {
                        ualts.add(rs.getString("name"));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return ualts;
    }

    public static String getUsername(String username) {
        UUID uuid = getUUID(username);
        if (uuid == null)
            return null;

        return getPlayerName(uuid.toString());
    }
}
