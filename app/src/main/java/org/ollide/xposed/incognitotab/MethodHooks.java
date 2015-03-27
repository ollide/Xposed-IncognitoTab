package org.ollide.xposed.incognitotab;

import android.app.Activity;
import android.os.Bundle;

import java.lang.reflect.Constructor;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MethodHooks implements IXposedHookLoadPackage {

    public static final String EXTRA_INCOGNITO_URL = "EXTRA_INCOGNITO_URL";

    private static String url = null;
    private static boolean didOpen = false;

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpp) throws Throwable {
        if (lpp.packageName.equals(PackageName.CHROME) || lpp.packageName.equals(PackageName.CHROME_BETA)) {
            hookChromeMethods(lpp);
        }
    }

    private void hookChromeMethods(XC_LoadPackage.LoadPackageParam lpp) {
        final String packageName = lpp.packageName;
        final ClassLoader classLoader = lpp.classLoader;

        final Class<?> chromeLauncherActivity = XposedHelpers.findClass("com.google.android.apps.chrome.document.ChromeLauncherActivity", classLoader);

        XposedHelpers.findAndHookMethod("com.google.android.apps.chrome.document.ChromeLauncherActivity", classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);

                Activity chromeActivity = (Activity) param.thisObject;
                String incognitoUrl = chromeActivity.getIntent().getStringExtra(EXTRA_INCOGNITO_URL);

                if (incognitoUrl != null) {
                    if (ChromeUtils.isDocumentMode(classLoader, packageName, chromeActivity)) {
                        XposedHelpers.callStaticMethod(chromeLauncherActivity, "launchInstance", chromeActivity, true, 0, incognitoUrl, 201, 6, false, null);
                    } else {
                        didOpen = false;
                        url = incognitoUrl;
                    }
                }
            }
        });

        // parameter of ChromeTabCreator#createNewTab
        final Class<?> loadUrlParamsClass = XposedHelpers.findClass("org.chromium.content_public.browser.LoadUrlParams", classLoader);
        final Class<?> tabLaunchTypeClass = XposedHelpers.findClass("org.chromium.chrome.browser.tabmodel.TabModel.TabLaunchType", classLoader);

        XposedHelpers.findAndHookMethod("com.google.android.apps.chrome.tab.ChromeTab", classLoader, "didStartPageLoad", String.class, boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);

                if (!didOpen && url != null) {
                    didOpen = true;

                    // this = empty regular tab
                    Object chromeTab = param.thisObject;

                    // access ChromeTab's activity
                    Object chromeActivity = XposedHelpers.getObjectField(chromeTab, "mActivity");

                    // access Activity's incognito TabCreator (true -> incognito, false -> regular)
                    Object tabCreator = XposedHelpers.callMethod(chromeActivity, "getTabCreator", true);

                    // create a LoadUrlParams object with the requested incognito URL
                    Constructor<?> constructor = loadUrlParamsClass.getConstructor(String.class);
                    Object loadUrlParams = constructor.newInstance(url);

                    // specify required enum (TabModel.TabLaunchType.FROM_MENU_OR_OVERVIEW)
                    Enum tabLaunchType = Enum.valueOf((Class<? extends Enum>) tabLaunchTypeClass, "FROM_MENU_OR_OVERVIEW");

                    // invoke createNewTab to open the url in an incognito tab :)
                    XposedHelpers.callMethod(tabCreator, "createNewTab", loadUrlParams, tabLaunchType, chromeTab);
                }
            }
        });
    }

}
