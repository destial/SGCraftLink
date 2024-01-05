package xyz.destiall.sgcraftlink.proxy.litebans;

public class Punishment {
    protected PunishmentType type;
    private final String victim;
    private final String executor;
    private final String duration;
    private final String reason;
    private final long time;
    private final long until;
    private final boolean active;
    private final String server;

    public Punishment(PunishmentType type, String victim, String executor, long time, long until, String reason, boolean active, String server) {
        this.type = type;
        this.victim = victim;
        this.reason = reason;
        this.executor = executor;
        this.duration = toDate(until - time);
        this.active = active;
        this.time = time;
        this.until = until;
        this.server = server.toLowerCase();
    }

    public boolean isActive() {
        return active && getType() != PunishmentType.KICK;
    }

    public String getServer() {
        return server;
    }

    public long getUntil() {
        return until;
    }

    public long getTime() {
        return this.time;
    }

    public String getExecutor() {
        return executor;
    }

    public String getVictim() {
        return victim;
    }

    public PunishmentType getType() {
        return type;
    }

    public String getDuration() {
        return duration;
    }

    public String getReason() {
        return reason;
    }

    public static String toDate(long milliseconds) {
        if (milliseconds <= 0)
            return "Forever";

        String str = "";
        long d = 0, h = 0, m = 0, s;

        if (milliseconds >= 86400000L) {
            d = milliseconds / 86400000L;
            str = d + (d > 1 ? " days" : " day");
            milliseconds = milliseconds - (d * 86400000L);
        }

        if (milliseconds >= 3600000L) {
            h = milliseconds / 3600000;
            if (d != 0) {
                str += ", " + h + (h > 1 ? " hours" : " hour");
            } else {
                str += h + (h > 1 ? " hours" : " hour");
            }
            milliseconds = milliseconds - (h * 3600000L);
        }

        if (milliseconds > 60000L) {
            m = milliseconds / 60000L;
            if (h != 0 || d != 0) {
                str += ", " + m + (m > 1 ? " minutes" : " minute");
            } else {
                str += m + (m > 1 ? " minutes" : " minute");
            }
            milliseconds = milliseconds - (m * 60000L);
        }

        if (milliseconds > 1000L) {
            s = milliseconds / 1000L;
            if (h != 0 || d != 0 || m != 0) {
                str += ", " + s + (s > 1 ? " seconds" : " second");
            } else {
                str += s + (s > 1 ? " seconds" : " second");
            }
        }

        if (str.trim().isEmpty()) {
            str = milliseconds + " ms";
        }

        return str;
    }
}
