package bin.xposed.Unblock163MusicClient;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.text.TextUtils;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

import static android.os.Looper.getMainLooper;
import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

@SuppressWarnings({"unused", "WeakerAccess"})
public class Utils {
    private static Map<String, InetAddress[]> dnsCache = new HashMap<>();
    private static WeakReference<Resources> moduleResources = new WeakReference<>(null);

    static String getFirstPartOfString(String str, String separator) {
        return str.substring(0, str.indexOf(separator));
    }

    static String getLastPartOfString(String str, String separator) {
        return str.substring(str.lastIndexOf(separator) + 1);
    }

    static String getFileName(String url) {
        return getFirstPartOfString(getLastPartOfString(url, "/"), ".");
    }

    public static String encode(String s) {
        try {
            return s == null ? "" : java.net.URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    static String serialData(Map<String, String> map) {
        return Stream.of(map.entrySet())
                .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                .collect(Collectors.joining("&"));
    }

    static String serialData(JSONObject json) {
        return Stream.of(json.keys())
                .map(k -> encode(k) + "=" + encode(getValByJsonKey(json, k).toString()))
                .collect(Collectors.joining("&"));
    }

    static Object getValByJsonKey(JSONObject json, String key) {
        try {
            return json.get(key);
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("deprecation")
    static String serialCookies(List cookieList, Map<String, String> cookieMethods, String filterDomain) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (Object cookie : cookieList) {

            String domain = (String) callMethod(cookie, cookieMethods.get("domain"));
            if (filterDomain == null || filterDomain.equals(domain)) {
                if (first) {
                    first = false;
                } else {
                    result.append("; ");
                }

                String name = (String) callMethod(cookie, cookieMethods.get("name"));
                String value = (String) callMethod(cookie, cookieMethods.get("value"));

                result.append(URLEncoder.encode(name, "UTF-8"));
                result.append("=");
                result.append(URLEncoder.encode(value, "UTF-8"));
            }
        }
        return result.toString();
    }


    public static InetAddress[] getIpByHostViaHttpDns(String domain) throws IOException, InvocationTargetException, IllegalAccessException, JSONException, PackageManager.NameNotFoundException {
        if (dnsCache.containsKey(domain)) {
            return dnsCache.get(domain);
        } else {
            String raw = Http.get(String.format("http://119.29.29.29/d?dn=%s&ip=119.29.29.29", domain), false)
                    .getResponseText();
            String[] ss = raw.replaceAll("[ \r\n]", "").split(";");


            InetAddress[] ips = new InetAddress[ss.length];
            for (int i = 0; i < ss.length; i++) {
                ips[i] = InetAddress.getByAddress(domain, InetAddress.getByName(ss[i]).getAddress());
            }
            dnsCache.put(domain, ips);
            return ips;
        }
    }

    static String optString(JSONObject json, String key) {
        // http://code.google.com/p/android/issues/detail?id=13830
        if (json.isNull(key)) {
            return null;
        } else {
            return json.optString(key, null);
        }
    }

    static Map<String, String> stringToMap(String data) {
        Map<String, String> map = new HashMap<>();
        map.put("A", null);

        if (!TextUtils.isEmpty(data)) {
            for (String s : data.split("&")) {
                String[] ss = s.split("=");
                map.put(ss[0], ss.length > 1 ? ss[1] : null);
            }
        }
        return map;
    }

    static Map<String, String> combineRequestData(String path, Map<String, String> map) {
        HashMap<String, String> hashMap = new HashMap<>(map);

        String query = Uri.parse(path).getQuery();
        if (!TextUtils.isEmpty(query)) {
            for (String s : query.split("&")) {
                String[] data = s.split("=");
                hashMap.put(data[0], data.length > 1 ? data[1] : null);
            }
        }
        return hashMap;
    }

    static Context getSystemContext() {
        Object activityThread = callStaticMethod(findClass("android.app.ActivityThread", null), "currentActivityThread");
        return (Context) callMethod(activityThread, "getSystemContext");
    }

    static Resources getModuleResources() throws PackageManager.NameNotFoundException {
        Resources resources = moduleResources.get();
        if (resources == null) {
            resources = getSystemContext().createPackageContext(BuildConfig.APPLICATION_ID, Context.CONTEXT_IGNORE_SECURITY).getResources();
            moduleResources = new WeakReference<>(resources);
        }

        return resources;

        // æˆ–è€…ç”¨ CloudMusicPackage.NeteaseMusicApplication.getApplication()
    }


    static String readFile(File file) throws IOException {
        if (file.exists() && file.isFile() && file.canRead()) {
            StringBuilder sb = new StringBuilder();
            BufferedReader input = null;
            try {
                input = new BufferedReader(new FileReader(file));
                String line;
                boolean isFirstLine = true;
                while ((line = input.readLine()) != null) {
                    sb.append(line);
                    if (isFirstLine) {
                        isFirstLine = false;
                    } else {
                        sb.append(System.getProperty("line.separator"));
                    }
                }
            } finally {
                try {
                    if (input != null) {
                        input.close();
                    }
                } catch (IOException e) {
                    log(e);
                }
            }
            return sb.toString();
        } else {
            throw new RuntimeException("file not exists or file can't read");
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    static void writeFile(File file, String string) throws IOException {
        file.getParentFile().mkdirs();

        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write(string);
        fileWriter.flush();
        fileWriter.close();
    }

    static File findFirstFile(File dir, final String start, final String end) {
        File[] fs = findFiles(dir, start, end, 1);
        return fs != null && fs.length > 0 ? fs[0] : null;
    }

    static File[] findFiles(File dir, final String start, final String end, final Integer limit) {
        if (dir != null && dir.exists() && dir.isDirectory()) {
            return dir.listFiles(new FilenameFilter() {
                int find = 0;

                @Override
                public boolean accept(File file, String s) {
                    if ((limit == null || find < limit)
                            && (TextUtils.isEmpty(start) || s.startsWith(start))
                            && (TextUtils.isEmpty(end) || s.endsWith(end))) {
                        find++;
                        return true;
                    } else {
                        return false;
                    }
                }
            });
        } else {
            return null;
        }
    }

    static void deleteFile(File file) {
        try {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        } catch (Throwable ignored) {
        }
    }

    static void deleteFiles(File[] files) {
        for (File file : files) {
            deleteFile(file);
        }
    }

    static boolean containsField(Class source, String exact, String start, String end) {
        return findFirstField(source, exact, start, end) != null;
    }

    static Field findFirstField(Class source, String exact, String start, String end) {
        Field[] fs = findFields(source, exact, start, end, 1);
        return fs.length > 0 ? fs[0] : null;
    }

    static Field[] findFields(Class source, String exact, String start, String end, int limit) {
        Field[] fs = source.getDeclaredFields();
        List<Field> returnFs = new ArrayList<>();

        for (Field f : fs) {
            if (returnFs.size() == limit) {
                break;
            }

            String s = f.getType().getName();
            if ((TextUtils.isEmpty(exact) || s.equals(exact))
                    && (TextUtils.isEmpty(start) || s.startsWith(start))
                    && (TextUtils.isEmpty(end) || s.endsWith(end))) {
                returnFs.add(f);
            }

        }
        return returnFs.toArray(new Field[returnFs.size()]);
    }

    static boolean isJSONValid(String test) {
        try {
            new JSONObject(test);
        } catch (Exception e) {
            try {
                new JSONArray(test);
            } catch (Exception ez) {
                return false;
            }
        }
        return true;
    }

    static List<String> filterList(List<String> list, Pattern pattern) {
        List<String> filteredList = new ArrayList<>();
        for (String curStr : list) {
            if (pattern.matcher(curStr).find()) {
                filteredList.add(curStr);
            }
        }
        return filteredList;
    }

    static List<String> filterList(List<String> list, String start, String end) {
        List<String> filteredList = new ArrayList<>();
        for (String s : list) {
            if ((TextUtils.isEmpty(start) || s.startsWith(start))
                    && (TextUtils.isEmpty(end) || s.endsWith(end))) {
                filteredList.add(s);
            }
        }
        return filteredList;
    }

    public static boolean isAppInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getApplicationInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static boolean isCallFromMyself() {
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        boolean findAppStack = false;
        boolean findModStack = false;

        for (StackTraceElement element : elements) {
            if (!findAppStack && element.getClassName().startsWith(BuildConfig.APPLICATION_ID)) {
                findAppStack = true;
                continue;
            }
            if (findAppStack && element.getClassName().startsWith(CloudMusicPackage.PACKAGE_NAME)) {
                findModStack = true;
                continue;
            }
            if (findModStack && element.getClassName().startsWith(BuildConfig.APPLICATION_ID)) {
                return true;
            }
        }
        return false;
    }

    public static void postDelayed(Runnable runnable, long delay) {
        new android.os.Handler(getMainLooper()).postDelayed(runnable, delay);
    }

    public static int randInt(int min, int max) {
        Random rand = new Random();
        return rand.nextInt((max - min) + 1) + min;
    }

    static class AlphanumComparator implements Comparator<String> {
        // http://www.davekoelle.com/files/AlphanumComparator.java

        private boolean isDigit(char ch) {
            return ch >= 48 && ch <= 57;
        }

        private String getChunk(String s, int slength, int marker) {
            StringBuilder chunk = new StringBuilder();
            char c = s.charAt(marker);
            chunk.append(c);
            marker++;
            if (isDigit(c)) {
                while (marker < slength) {
                    c = s.charAt(marker);
                    if (!isDigit(c)) {
                        break;
                    }
                    chunk.append(c);
                    marker++;
                }
            } else {
                while (marker < slength) {
                    c = s.charAt(marker);
                    if (isDigit(c)) {
                        break;
                    }
                    chunk.append(c);
                    marker++;
                }
            }
            return chunk.toString();
        }

        @Override
        public int compare(String s1, String s2) {
            int thisMarker = 0;
            int thatMarker = 0;
            int s1Length = s1.length();
            int s2Length = s2.length();

            while (thisMarker < s1Length && thatMarker < s2Length) {
                String thisChunk = getChunk(s1, s1Length, thisMarker);
                thisMarker += thisChunk.length();

                String thatChunk = getChunk(s2, s2Length, thatMarker);
                thatMarker += thatChunk.length();

                int result;
                if (isDigit(thisChunk.charAt(0)) == isDigit(thatChunk.charAt(0))) {
                    int thisChunkLength = thisChunk.length();
                    result = thisChunkLength - thatChunk.length();
                    if (result == 0) {
                        for (int i = 0; i < thisChunkLength; i++) {
                            result = thisChunk.charAt(i) - thatChunk.charAt(i);
                            if (result != 0) {
                                return result;
                            }
                        }
                    }
                } else {
                    result = thisChunk.compareTo(thatChunk);
                }

                if (result != 0) {
                    return result;
                }
            }

            return s1Length - s2Length;
        }
    }
}