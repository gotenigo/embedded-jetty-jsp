package org.eclipse.jetty.demo;

import org.eclipse.jetty.demo.utils.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.ArrayList;

import static org.eclipse.jetty.demo.WebServer.DEFAULT_PORT;

public class ConsoleManager {

    public static final String DEFAULT_URL="https://localhost:"+DEFAULT_PORT+"/";



    public static void main(String[] args) throws Exception{

        String url=DEFAULT_URL;
        if(args!=null && args.length>=1){
            url=args[0];
        }
        openBrowser(url);

    }




    /**
     * Open a new browser tab or window with the given URL.
     *
     * @param url the URL to open
     * @throws Exception on failure
     */
    public static void openBrowser(String url) throws Exception {
        try {
            String osName = StringUtils.toLowerEnglish(getProperty("os.name", "linux"));
            Runtime rt = Runtime.getRuntime();
            String browser = getProperty("DEFAULT_BROWSER", null);
            if (browser == null) {
                // under Linux, this will point to the default system browser
                try {
                    browser = System.getenv("BROWSER");
                } catch (SecurityException se) {
                    // ignore
                }
            }
            if (browser != null) {
                if (browser.startsWith("call:")) {
                    browser = browser.substring("call:".length());
                    callStaticMethod(browser, url);
                } else if (browser.contains("%url")) {
                    String[] args = arraySplit(browser, ',', false);
                    for (int i = 0; i < args.length; i++) {
                        args[i] = StringUtils.replaceAll(args[i], "%url", url);
                    }
                    rt.exec(args);
                } else if (osName.contains("windows")) {
                    rt.exec(new String[] { "cmd.exe", "/C",  browser, url });
                } else {
                    rt.exec(new String[] { browser, url });
                }
                return;
            }
            try {
                Class<?> desktopClass = Class.forName("java.awt.Desktop");
                // Desktop.isDesktopSupported()
                Boolean supported = (Boolean) desktopClass.
                        getMethod("isDesktopSupported").
                        invoke(null, new Object[0]);
                URI uri = new URI(url);
                if (supported) {
                    // Desktop.getDesktop();
                    Object desktop = desktopClass.getMethod("getDesktop").
                            invoke(null);
                    // desktop.browse(uri);
                    desktopClass.getMethod("browse", URI.class).
                            invoke(desktop, uri);
                    return;
                }
            } catch (Exception e) {
                // ignore
            }
            if (osName.contains("windows")) {
                rt.exec(new String[] { "rundll32", "url.dll,FileProtocolHandler", url });
            } else if (osName.contains("mac") || osName.contains("darwin")) {
                // Mac OS: to open a page with Safari, use "open -a Safari"
                Runtime.getRuntime().exec(new String[] { "open", url });
            } else {
                String[] browsers = { "xdg-open", "chromium", "google-chrome",
                        "firefox", "mozilla-firefox", "mozilla", "konqueror",
                        "netscape", "opera", "midori" };
                boolean ok = false;
                for (String b : browsers) {
                    try {
                        rt.exec(new String[] { b, url });
                        ok = true;
                        break;
                    } catch (Exception e) {
                        // ignore and try the next
                    }
                }
                if (!ok) {
                    // No success in detection.
                    throw new Exception(
                            "Browser detection failed, and java property 'h2.browser' " +
                                    "and environment variable BROWSER are not set to a browser executable.");
                }
            }
        } catch (Exception e) {
            throw new Exception(
                    "Failed to start a browser to open the URL " +
                            url + ": " + e.getMessage());
        }
    }




    /**
     * Get the system property. If the system property is not set, or if a
     * security exception occurs, the default value is returned.
     *
     * @param key the key
     * @param defaultValue the default value
     * @return the value
     */
    public static String getProperty(String key, String defaultValue) {
        try {
            return System.getProperty(key, defaultValue);
        } catch (SecurityException se) {
            return defaultValue;
        }
    }




    /**
     * Calls a static method via reflection. This will try to use the method
     * where the most parameter classes match exactly (this algorithm is simpler
     * than the one in the Java specification, but works well for most cases).
     *
     * @param classAndMethod a string with the entire class and method name, eg.
     *            "java.lang.System.gc"
     * @param params the method parameters
     * @return the return value from this call
     * @throws Exception on failure
     */
    public static Object callStaticMethod(String classAndMethod,
                                          Object... params) throws Exception {
        int lastDot = classAndMethod.lastIndexOf('.');
        String className = classAndMethod.substring(0, lastDot);
        String methodName = classAndMethod.substring(lastDot + 1);
        return callMethod(null, Class.forName(className), methodName, params);
    }


    private static Object callMethod(
            Object instance, Class<?> clazz,
            String methodName,
            Object... params) throws Exception {
        Method best = null;
        int bestMatch = 0;
        boolean isStatic = instance == null;
        for (Method m : clazz.getMethods()) {
            if (Modifier.isStatic(m.getModifiers()) == isStatic &&
                    m.getName().equals(methodName)) {
                int p = match(m.getParameterTypes(), params);
                if (p > bestMatch) {
                    bestMatch = p;
                    best = m;
                }
            }
        }
        if (best == null) {
            throw new NoSuchMethodException(methodName);
        }
        return best.invoke(instance, params);
    }



    private static int match(Class<?>[] params, Object[] values) {
        int len = params.length;
        if (len == values.length) {
            int points = 1;
            for (int i = 0; i < len; i++) {
                Class<?> pc = getNonPrimitiveClass(params[i]);
                Object v = values[i];
                Class<?> vc = v == null ? null : v.getClass();
                if (pc == vc) {
                    points++;
                } else if (vc == null) {
                    // can't verify
                } else if (!pc.isAssignableFrom(vc)) {
                    return 0;
                }
            }
            return points;
        }
        return 0;
    }



    /**
     * Convert primitive class names to java.lang.* class names.
     *
     * @param clazz the class (for example: int)
     * @return the non-primitive class (for example: java.lang.Integer)
     */
    public static Class<?> getNonPrimitiveClass(Class<?> clazz) {
        if (!clazz.isPrimitive()) {
            return clazz;
        } else if (clazz == boolean.class) {
            return Boolean.class;
        } else if (clazz == byte.class) {
            return Byte.class;
        } else if (clazz == char.class) {
            return Character.class;
        } else if (clazz == double.class) {
            return Double.class;
        } else if (clazz == float.class) {
            return Float.class;
        } else if (clazz == int.class) {
            return Integer.class;
        } else if (clazz == long.class) {
            return Long.class;
        } else if (clazz == short.class) {
            return Short.class;
        } else if (clazz == void.class) {
            return Void.class;
        }
        return clazz;
    }




    /**
     * Create a new ArrayList with an initial capacity of 4.
     *
     * @param <T> the type
     * @return the object
     */
    public static <T> ArrayList<T> newSmallArrayList() {
        return new ArrayList<>(4);
    }


    /**
     * Split a string into an array of strings using the given separator. A null
     * string will result in a null array, and an empty string in a zero element
     * array.
     *
     * @param s the string to split
     * @param separatorChar the separator character
     * @param trim whether each element should be trimmed
     * @return the array list
     */
    public static String[] arraySplit(String s, char separatorChar, boolean trim) {
        if (s == null) {
            return null;
        }
        int length = s.length();
        if (length == 0) {
            return new String[0];
        }
        ArrayList<String> list = newSmallArrayList();
        StringBuilder buff = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            char c = s.charAt(i);
            if (c == separatorChar) {
                String e = buff.toString();
                list.add(trim ? e.trim() : e);
                buff.setLength(0);
            } else if (c == '\\' && i < length - 1) {
                buff.append(s.charAt(++i));
            } else {
                buff.append(c);
            }
        }
        String e = buff.toString();
        list.add(trim ? e.trim() : e);
        return list.toArray(new String[0]);
    }




}
