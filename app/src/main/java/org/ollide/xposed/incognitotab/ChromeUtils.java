package org.ollide.xposed.incognitotab;

import android.app.Activity;
import android.content.Context;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import de.robv.android.xposed.XposedHelpers;

public final class ChromeUtils {

    public static boolean isDocumentMode(ClassLoader classLoader, String packageName, Activity chromeActivity) {
        Class<?> featureUtilitiesClass;
        if (PackageName.CHROME.equals(packageName)) {
            featureUtilitiesClass = XposedHelpers.findClass("com.google.android.apps.chrome.utilities.FeatureUtilitiesInternal", classLoader);
        } else {
            featureUtilitiesClass = XposedHelpers.findClass("org.chromium.chrome.browser.util.FeatureUtilities", classLoader);
        }

        boolean documentMode = false;
        try {
            Method isDocumentMode = featureUtilitiesClass.getMethod("isDocumentMode", Context.class);
            documentMode = (boolean) isDocumentMode.invoke(null, chromeActivity);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            // fail silently
        }
        return documentMode;
    }
}
