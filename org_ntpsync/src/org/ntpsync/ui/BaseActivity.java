/*
 * Copyright (C) 2012 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * This file is part of NTPSync.
 * 
 * NTPSync is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NTPSync is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NTPSync.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.ntpsync.ui;

import java.util.Date;

import org.donations.DonationsActivity;
import org.ntpsync.R;
import org.ntpsync.service.NtpSyncService;
import org.ntpsync.util.Constants;
import org.ntpsync.util.Log;
import org.ntpsync.util.PreferencesHelper;
import org.ntpsync.util.Utils;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.view.Window;
import android.widget.Toast;

public class BaseActivity extends PreferenceActivity {

    Activity mActivity;

    private Preference mSync;

    private Preference mHelp;
    private Preference mAbout;
    private Preference mDonations;

    private boolean progressEnabled;

    private void setIndeterminateProgress(Boolean enabled) {
        progressEnabled = enabled;
        setProgressBarIndeterminateVisibility(enabled);
    }

    /**
     * Retain activity on rotate and set back progress indicator
     * 
     * @param newConfig
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        setIndeterminateProgress(progressEnabled);
    }

    /** Called when the activity is first created. */
    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        // enable progress indicator for later use
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        super.onCreate(savedInstanceState);

        setIndeterminateProgress(false);

        mActivity = this;

        getPreferenceManager().setSharedPreferencesName(Constants.PREFS_NAME);
        addPreferencesFromResource(R.xml.preferences);

        mSync = (Preference) findPreference(getString(R.string.pref_sync_key));
        mHelp = (Preference) findPreference(getString(R.string.pref_help_key));
        mAbout = (Preference) findPreference(getString(R.string.pref_about_key));
        mDonations = (Preference) findPreference(getString(R.string.pref_donations_key));

        mSync.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                // start progress indicator
                setIndeterminateProgress(true);

                // start service with ntp server from preferences
                Intent intent = new Intent(mActivity, NtpSyncService.class);

                intent.putExtra(NtpSyncService.EXTRA_ACTION, NtpSyncService.ACTION_GET_TIME);

                // Message is received after saving is done in service
                Handler resultHandler = new Handler() {
                    public void handleMessage(Message message) {
                        // stop progress indicator
                        setIndeterminateProgress(false);

                        Toast toast = null;
                        switch (message.arg1) {
                        case NtpSyncService.MESSAGE_ERROR:
                            toast = Toast.makeText(mActivity, "error", Toast.LENGTH_LONG);
                            toast.show();

                            break;

                        case NtpSyncService.MESSAGE_OKAY:
                            Bundle returnData = message.getData();
                            Date newTime = (Date) returnData
                                    .getSerializable(NtpSyncService.MESSAGE_DATA_TIME);

                            toast = Toast.makeText(mActivity, "Time was set to " + newTime,
                                    Toast.LENGTH_LONG);
                            toast.show();

                            break;

                        case NtpSyncService.MESSAGE_SERVER_TIMEOUT:
                            toast = Toast.makeText(mActivity, "server timeout!", Toast.LENGTH_LONG);
                            toast.show();

                            break;

                        case NtpSyncService.MESSAGE_NO_ROOT:
                            Utils.showRootDialog(mActivity);

                            break;

                        case NtpSyncService.MESSAGE_UTIL_NOT_FOUND:
                            toast = Toast.makeText(mActivity, "date Util not found!",
                                    Toast.LENGTH_LONG);
                            toast.show();

                            break;

                        default:
                            break;
                        }

                    };
                };

                // Create a new Messenger for the communication back
                Messenger messenger = new Messenger(resultHandler);
                intent.putExtra(NtpSyncService.EXTRA_MESSENGER, messenger);

                Bundle data = new Bundle();
                data.putString(NtpSyncService.DATA_NTP_SERVER,
                        PreferencesHelper.getNtpServer(mActivity));

                intent.putExtra(NtpSyncService.EXTRA_DATA, data);
                mActivity.startService(intent);

                return false;
            }

        });

        mHelp.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(mActivity, HelpActivity.class));

                return false;
            }

        });

        mAbout.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(mActivity, AboutActivity.class));

                return false;
            }

        });

        mDonations.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(mActivity, DonationsActivity.class));

                return false;
            }

        });

        // TODO: TEST
        // set one hour later
        // Utils.setTime(new Date(new Date().getTime() + (1000 * 60 * 60)));

    }
}