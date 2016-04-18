package me.iasc.vultureegg.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;


public class SettingActivity extends Activity {
    private static final String TAG = SettingActivity.class.getSimpleName();
    private Tracker mTracker;

    public static String SET_M_COTTON = "mCottonServer";
    public static String SET_M_MCOTTON_USER = "mCottonUser";
    public static String SET_M_MCOTTON_PASSWORD = "mCottonPassword";

    private EditText editServer, editUser, editPassword;

    private RadioGroup radioGroup;
    private RadioButton radioCn, radioCc, radioLan;

    static SharedPreferences settings;
    static SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        VultureEggApplication application = (VultureEggApplication) getApplication();
        mTracker = application.getDefaultTracker();

        getActionBar().setDisplayHomeAsUpEnabled(true);

        settings = PreferenceManager.getDefaultSharedPreferences(this);

        editServer = (EditText) findViewById(R.id.editServer);

        radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        radioCn = (RadioButton) radioGroup.findViewById(R.id.server_cn);
        radioCc = (RadioButton) radioGroup.findViewById(R.id.server_cc);
        radioLan = (RadioButton) radioGroup.findViewById(R.id.server_lan);

        radioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // find which radio button is selected
                if (checkedId == radioCn.getId()) {
                    editServer.setText(radioCn.getText());
                } else if (checkedId == radioCc.getId()) {
                    editServer.setText(radioCc.getText());
                } else {
                    editServer.setText(radioLan.getText());
                }
            }
        });

        editUser = (EditText) findViewById(R.id.editUser);
        editPassword = (EditText) findViewById(R.id.editPassword);

        editServer.setText(settings.getString(SET_M_COTTON, "mcotton.microduino.cn"));
        editUser.setText(settings.getString(SET_M_MCOTTON_USER, "Email"));
        editPassword.setText(settings.getString(SET_M_MCOTTON_PASSWORD, "Password"));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        editor = settings.edit();
        String server = editServer.getText().toString();

        editor.putString(SET_M_COTTON, server);
        editor.putString(SET_M_MCOTTON_USER, editUser.getText().toString());
        editor.putString(SET_M_MCOTTON_PASSWORD, editPassword.getText().toString());
        editor.commit();

        mTracker.send(new HitBuilders.EventBuilder().setCategory("Action")
                .setAction("Setting").setLabel(server).build());

        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mTracker.setScreenName("SettingActivity");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }
}
