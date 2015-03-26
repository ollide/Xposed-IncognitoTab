package org.ollide.xposed.incognitotab;

import android.app.Activity;
import android.os.Bundle;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MethodHooks implements IXposedHookLoadPackage {

    public static final String EXTRA_INCOGNITO_URL = "EXTRA_INCOGNITO_URL";

    private static final String PACKAGE_CHROME = "com.android.chrome";
    private static final String PACKAGE_CHROME_BETA = "com.chrome.beta";

    private static String url = null;
    private static boolean didOpen = false;

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpp) throws Throwable {
        if (lpp.packageName.equals(PACKAGE_CHROME) || lpp.packageName.equals(PACKAGE_CHROME_BETA)) {
            hookChromeMethods(lpp.classLoader);
        }
    }

    private void hookChromeMethods(final ClassLoader classLoader) {
        XposedHelpers.findAndHookMethod("com.google.android.apps.chrome.document.ChromeLauncherActivity", classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);

                Activity chromeActivity = (Activity) param.thisObject;
                String incognitoUrl = chromeActivity.getIntent().getStringExtra(EXTRA_INCOGNITO_URL);
                if (incognitoUrl != null) {
                    didOpen = false;
                    url = incognitoUrl;
                }
            }
        });

        final Class<?> chromeTabClass = XposedHelpers.findClass("com.google.android.apps.chrome.tab.ChromeTab", classLoader);
        final Class<?> chromeActivityClass = XposedHelpers.findClass("com.google.android.apps.chrome.ChromeActivity", classLoader);

        // class to create new tabs
        final Class<?> chromeTabCreatorClass = XposedHelpers.findClass("com.google.android.apps.chrome.tabmodel.ChromeTabCreator", classLoader);

        // parameter of ChromeTabCreator#createNewTab
        final Class<?> loadUrlParamsClass = XposedHelpers.findClass("org.chromium.content_public.browser.LoadUrlParams", classLoader);
        final Class<?> tabLaunchTypeClass = XposedHelpers.findClass("org.chromium.chrome.browser.tabmodel.TabModel.TabLaunchType", classLoader);
        final Class<?> tabClass = XposedHelpers.findClass("org.chromium.chrome.browser.Tab", classLoader);

        XposedHelpers.findAndHookMethod("com.google.android.apps.chrome.tab.ChromeTab", classLoader, "didStartPageLoad", String.class, boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);

                if (!didOpen && url != null) {
                    didOpen = true;

                    // this = empty regular tab (chrome-native://newtab/)
                    Object chromeTab = param.thisObject;

                    // access ChromeTab's activity
                    Field chromeActivityField = chromeTabClass.getDeclaredField("mActivity");
                    chromeActivityField.setAccessible(true);
                    Object chromeActivity = chromeActivityField.get(chromeTab);

                    // access Activity's incognito TabCreator
                    Method getTabCreator = chromeActivityClass.getMethod("getTabCreator", boolean.class);
                    Object tabCreator = getTabCreator.invoke(chromeActivity, true);

                    // retrieve TabCreator's method createNewTab
                    Method createNewTab = chromeTabCreatorClass.getMethod("createNewTab", loadUrlParamsClass, tabLaunchTypeClass, tabClass);

                    // create a LoadUrlParams object with the requested incognito URL
                    Constructor<?> constructor = loadUrlParamsClass.getConstructor(String.class);
                    Object loadUrlParams = constructor.newInstance(url);

                    // specify required enum (TabModel.TabLaunchType.FROM_LINK)
                    Enum tabLaunchType = Enum.valueOf((Class<? extends Enum>) tabLaunchTypeClass, "FROM_MENU_OR_OVERVIEW");

                    // invoke createNewTab to open the url in an incognito tab :)
                    createNewTab.invoke(tabCreator, loadUrlParams, tabLaunchType, chromeTab);
                }
            }
        });
    }

}
