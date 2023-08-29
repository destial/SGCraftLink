package xyz.destiall.sgcraftlink.proxy.misc;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Year;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class Birthday {
    private final Month month;
    private final int day;
    private boolean celebrated;

    public Birthday(Month month, int day, boolean celebrated) {
        this.month = month;
        this.day = day;
        this.celebrated = celebrated;
    }

    public boolean hasAlreadyCelebrated() {
        return celebrated;
    }

    public int getMonth() {
        return this.month.getValue();
    }

    public int getDay() {
        return this.day;
    }

    public String getTimeLeft() {
        try {
            Date today = new Date();
            Calendar today_cal = Calendar.getInstance();
            today_cal.setTime(today);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            String date = getDay() + "/" + getMonth() + "/" + today_cal.get(Calendar.YEAR) + " 00:00";
            Date expiration = sdf.parse(date);
            Calendar exp_cal = Calendar.getInstance();
            exp_cal.setTime(expiration);
            if (today_cal.get(Calendar.DAY_OF_YEAR) >= exp_cal.get(Calendar.DAY_OF_YEAR)) {
                date = getDay() + "/" + getMonth() + "/" + (today_cal.get(Calendar.YEAR) + 1) + " 00:00";
                expiration = sdf.parse(date);
                exp_cal = Calendar.getInstance();
                exp_cal.setTime(expiration);
            }
            long second_dif = TimeUnit.MILLISECONDS.toSeconds(Duration.between(today_cal.toInstant(), exp_cal.toInstant()).toMillis());
            int days = (int)Duration.between(today_cal.toInstant(), exp_cal.toInstant()).toDays();
            int hours = (int)Math.abs(Duration.between(today_cal.toInstant(), exp_cal.toInstant()).toHours() - TimeUnit.DAYS.toHours(days));
            int minutes = (int)Math.abs(Duration.between(today_cal.toInstant(), exp_cal.toInstant()).toMinutes() - TimeUnit.DAYS.toMinutes(days) - TimeUnit.HOURS.toMinutes(hours));
            int seconds = (int)Math.abs(second_dif - TimeUnit.DAYS.toSeconds(days) - TimeUnit.HOURS.toSeconds(hours) - TimeUnit.MINUTES.toSeconds(minutes));
            String days_str = String.valueOf(days);
            String hours_str = String.valueOf(hours);
            String minutes_str = String.valueOf(minutes);
            String seconds_str = String.valueOf(seconds);
            return days_str + " days " + hours_str + " hours " + minutes_str + " minutes " + seconds_str + " seconds";
        } catch (Throwable ex) {
            return "-1 00:00:00";
        }
    }

    public String getDayName() {
        try {
            Date today = new Date();
            Calendar today_cal = Calendar.getInstance();
            today_cal.setTime(today);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            String date = getDay() + "/" + getMonth() + "/" + today_cal.get(Calendar.YEAR) + " " + today_cal.get(Calendar.HOUR_OF_DAY) + ":" + today_cal.get(Calendar.MINUTE);
            Date expiration = sdf.parse(date);
            Calendar exp_cal = Calendar.getInstance();
            exp_cal.setTime(expiration);
            return exp_cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.ENGLISH);
        } catch (Throwable ex) {
            return "null";
        }
    }

    public String getMonthName() {
        return month.getName();
    }

    public boolean isToday() {
        LocalDate today = LocalDate.now();
        LocalDate birthday = LocalDate.of(Year.now().getValue(), day, month.value - 1);

        return today.equals(birthday);
    }

    public static class Days {
        public static Map<Month, Days> ALL_DAYS = new HashMap<>();
        static {
            for (Month month : Month.values()) {
                Days d = new Days(month);
                ALL_DAYS.put(month, d);
            }
        }

        private final Month month;
        Days(Month month) {
            this.month = month;
        }

        public boolean isMonth(Month month) {
            return this.month.equals(month);
        }

        public int getMax() {
            switch (month) {
                case JANUARY:
                case MARCH:
                case MAY:
                case JULY:
                case AUGUST:
                case OCTOBER:
                case DECEMBER:
                    return 31;
                case FEBRUARY:
                    return Year.now().isLeap() ? 29 : 28;
                case APRIL:
                case JUNE:
                case SEPTEMBER:
                case NOVEMBER:
                default:
                    return 30;
            }
        }
    }

    public enum Month {
        JANUARY(1),
        FEBRUARY(2),
        MARCH(3),
        APRIL(4),
        MAY(5),
        JUNE(6),
        JULY(7),
        AUGUST(8),
        SEPTEMBER(9),
        OCTOBER(10),
        NOVEMBER(11),
        DECEMBER(12);

        private final int value;

        Month(int value) {
            this.value = value;
        }

        public final int getValue() {
            return this.value;
        }

        public final String getName() {
            return name().charAt(0) + name().substring(1).toLowerCase();
        }
    }
}
