package org.ollide.xposed.incognitotab;

import android.app.Activity;

import de.robv.android.xposed.XposedHelpers;

public final class ChromeUtils {

    public static boolean isDocumentMode(ClassLoader classLoader, String packageName, Activity chromeActivity) {
        Class<?> featureUtilitiesClass;
        if (PackageName.CHROME.equals(packageName)) {
            featureUtilitiesClass = XposedHelpers.findClass("com.google.android.apps.chrome.utilities.FeatureUtilitiesInternal", classLoader);
        } else {
            featureUtilitiesClass = XposedHelpers.findClass("org.chromium.chrome.browser.util.FeatureUtilities", classLoader);
        }
        return (boolean) XposedHelpers.callStaticMethod(featureUtilitiesClass, "isDocumentMode", chromeActivity);
    }
}
