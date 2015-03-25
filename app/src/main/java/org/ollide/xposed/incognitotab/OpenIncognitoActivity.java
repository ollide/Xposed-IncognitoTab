package org.ollide.xposed.incognitotab;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

public class OpenIncognitoActivity extends Activity {

    private static final String CHROME_EMPTY_TAB = "googlechrome://navigate?url=chrome-native://newtab/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent in = getIntent();
        if (Intent.ACTION_VIEW.equals(in.getAction())) {

            Intent incognitoIntent = new Intent(Intent.ACTION_VIEW);
            incognitoIntent.addCategory(Intent.CATEGORY_BROWSABLE);
            incognitoIntent.setData(Uri.parse(CHROME_EMPTY_TAB));
            // custom extra which contains the incognito URL
            incognitoIntent.putExtra(MethodHooks.EXTRA_INCOGNITO_URL, in.getDataString());

            try {
                startActivity(incognitoIntent);
            } catch (ActivityNotFoundException exception) {
                Toast.makeText(this, R.string.chrome_not_found, Toast.LENGTH_SHORT).show();
            }

            finish();
        }
    }
}
