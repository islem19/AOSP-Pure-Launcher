/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dz.aosp.purelauncher;

import static dz.aosp.purelauncher.states.RotationHelper.ALLOW_ROTATION_PREFERENCE_KEY;
import static dz.aosp.purelauncher.states.RotationHelper.getAllowRotationDefaultValue;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.ListPreference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.Adapter;
import android.widget.ListView;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.util.Log;

import dz.aosp.purelauncher.graphics.IconShapeOverride;
import dz.aosp.purelauncher.notification.NotificationListener;
import dz.aosp.purelauncher.util.ListViewHighlighter;
import dz.aosp.purelauncher.util.SettingsObserver;
import dz.aosp.purelauncher.views.ButtonPreference;
import dz.aosp.purelauncher.util.LooperExecutor;

import java.util.Objects;

/**
 * Settings activity for Launcher. Currently implements the following setting: Allow rotation
 */
public class SettingsActivity extends Activity {

    private static final String ICON_BADGING_PREFERENCE_KEY = "pref_icon_badging";
    /** Hidden field Settings.Secure.NOTIFICATION_BADGING */
    public static final String NOTIFICATION_BADGING = "notification_badging";
    /** Hidden field Settings.Secure.ENABLED_NOTIFICATION_LISTENERS */
    private static final String NOTIFICATION_ENABLED_LISTENERS = "enabled_notification_listeners";

    private static final String EXTRA_FRAGMENT_ARG_KEY = ":settings:fragment_args_key";
    private static final String EXTRA_SHOW_FRAGMENT_ARGS = ":settings:show_fragment_args";
    private static final int DELAY_HIGHLIGHT_DURATION_MILLIS = 600;
    private static final String SAVE_HIGHLIGHTED_KEY = "android:preference_highlighted";
    public static final String PREF_THEME_STYLE_KEY = "pref_theme_style";
    private static boolean mRestartNeeded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            // Display the fragment as the main content.
            getFragmentManager().beginTransaction()
                    .replace(android.R.id.content, getNewFragment())
                    .commit();
        }
    }

    protected PreferenceFragment getNewFragment() {
        return new LauncherSettingsFragment();
    }

    /**
     * This fragment shows the launcher preferences.
     */
    public static class LauncherSettingsFragment extends PreferenceFragment {

        private IconBadgingObserver mIconBadgingObserver;

        private String mPreferenceKey;
        private boolean mPreferenceHighlighted = false;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (savedInstanceState != null) {
                mPreferenceHighlighted = savedInstanceState.getBoolean(SAVE_HIGHLIGHTED_KEY);
            }

            getPreferenceManager().setSharedPreferencesName(LauncherFiles.SHARED_PREFERENCES_KEY);
            addPreferencesFromResource(R.xml.launcher_preferences);

            HomeKeyWatcher mHomeKeyListener = new HomeKeyWatcher(getActivity());
            mHomeKeyListener.setOnHomePressedListener(() -> {
                if (mRestartNeeded) {
                    Utilities.restart(getActivity());
                }
            });
            mHomeKeyListener.startWatch();

            ContentResolver resolver = getActivity().getContentResolver();

            ButtonPreference iconBadgingPref =
                    (ButtonPreference) findPreference(ICON_BADGING_PREFERENCE_KEY);
            if (!Utilities.ATLEAST_OREO) {
                getPreferenceScreen().removePreference(
                        findPreference(SessionCommitReceiver.ADD_ICON_PREFERENCE_KEY));
                getPreferenceScreen().removePreference(iconBadgingPref);
            } else if (!getResources().getBoolean(R.bool.notification_badging_enabled)) {
                getPreferenceScreen().removePreference(iconBadgingPref);
            } else {
                // Listen to system notification badge settings while this UI is active.
                mIconBadgingObserver = new IconBadgingObserver(
                        iconBadgingPref, resolver, getFragmentManager());
                mIconBadgingObserver.register(NOTIFICATION_BADGING, NOTIFICATION_ENABLED_LISTENERS);
            }

            Preference iconShapeOverride = findPreference(IconShapeOverride.KEY_PREFERENCE);
            if (iconShapeOverride != null) {
                if (IconShapeOverride.isSupported(getActivity())) {
                    IconShapeOverride.handlePreferenceUi((ListPreference) iconShapeOverride);
                } else {
                    getPreferenceScreen().removePreference(iconShapeOverride);
                }
            }

            // Setup allow rotation preference
            Preference rotationPref = findPreference(ALLOW_ROTATION_PREFERENCE_KEY);
            if (getResources().getBoolean(R.bool.allow_rotation)) {
                // Launcher supports rotation by default. No need to show this setting.
                getPreferenceScreen().removePreference(rotationPref);
            } else {
                // Initialize the UI once
                rotationPref.setDefaultValue(getAllowRotationDefaultValue());
            }

            final ListPreference iconSizes = (ListPreference) findPreference(Utilities.ICON_SIZE);
            iconSizes.setSummary(iconSizes.getEntry());
            iconSizes.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    int index = iconSizes.findIndexOfValue((String) newValue);
                    iconSizes.setSummary(iconSizes.getEntries()[index]);
                    mRestartNeeded = true;
                    return true;
                }
            });

            final ListPreference gridColumns = (ListPreference) findPreference(Utilities.GRID_COLUMNS);
            gridColumns.setSummary(gridColumns.getEntry());
            gridColumns.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    int index = gridColumns.findIndexOfValue((String) newValue);
                    gridColumns.setSummary(gridColumns.getEntries()[index]);
                    mRestartNeeded = true;
                    return true;
                }
            });

            final ListPreference gridRows = (ListPreference) findPreference(Utilities.GRID_ROWS);
            gridRows.setSummary(gridRows.getEntry());
            gridRows.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    int index = gridRows.findIndexOfValue((String) newValue);
                    gridRows.setSummary(gridRows.getEntries()[index]);
                    mRestartNeeded = true;
                    return true;
                }
            });

            final ListPreference mThemeStyle = (ListPreference) findPreference(PREF_THEME_STYLE_KEY);
            mThemeStyle.setSummary(mThemeStyle.getEntry());
            mThemeStyle.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    String newValue = (String) o;
                    int valueIndex = mThemeStyle.findIndexOfValue(newValue);
                    mThemeStyle.setSummary(mThemeStyle.getEntries()[valueIndex]);
                    mRestartNeeded = true;
                    return true;
                }
            });

        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putBoolean(SAVE_HIGHLIGHTED_KEY, mPreferenceHighlighted);
        }

        @Override
        public void onResume() {
            super.onResume();

            Intent intent = getActivity().getIntent();
            mPreferenceKey = intent.getStringExtra(EXTRA_FRAGMENT_ARG_KEY);
            if (isAdded() && !mPreferenceHighlighted && !TextUtils.isEmpty(mPreferenceKey)) {
                getView().postDelayed(this::highlightPreference, DELAY_HIGHLIGHT_DURATION_MILLIS);
            }
        }

        private void highlightPreference() {
            Preference pref = findPreference(mPreferenceKey);
            if (pref == null || getPreferenceScreen() == null) {
                return;
            }
            PreferenceScreen screen = getPreferenceScreen();
            if (Utilities.ATLEAST_OREO) {
                screen = selectPreferenceRecursive(pref, screen);
            }
            if (screen == null) {
                return;
            }

            View root = screen.getDialog() != null
                    ? screen.getDialog().getWindow().getDecorView() : getView();
            ListView list = root.findViewById(android.R.id.list);
            if (list == null || list.getAdapter() == null) {
                return;
            }
            Adapter adapter = list.getAdapter();

            // Find the position
            int position = -1;
            for (int i = adapter.getCount() - 1; i >= 0; i--) {
                if (pref == adapter.getItem(i)) {
                    position = i;
                    break;
                }
            }
            new ListViewHighlighter(list, position);
            mPreferenceHighlighted = true;
        }

        @Override
        public void onDestroy() {
            if (mIconBadgingObserver != null) {
                mIconBadgingObserver.unregister();
                mIconBadgingObserver = null;
            }
            super.onDestroy();
            if (mRestartNeeded) {
                Utilities.restart(getActivity());
            }
        }

        @TargetApi(Build.VERSION_CODES.O)
        private PreferenceScreen selectPreferenceRecursive(
                Preference pref, PreferenceScreen topParent) {
            if (!(pref.getParent() instanceof PreferenceScreen)) {
                return null;
            }

            PreferenceScreen parent = (PreferenceScreen) pref.getParent();
            if (Objects.equals(parent.getKey(), topParent.getKey())) {
                return parent;
            } else if (selectPreferenceRecursive(parent, topParent) != null) {
                ((PreferenceScreen) parent.getParent())
                        .onItemClick(null, null, parent.getOrder(), 0);
                return parent;
            } else {
                return null;
            }
        }
    }

    /**
     * Content observer which listens for system badging setting changes,
     * and updates the launcher badging setting subtext accordingly.
     */
    private static class IconBadgingObserver extends SettingsObserver.Secure
            implements Preference.OnPreferenceClickListener {

        private final ButtonPreference mBadgingPref;
        private final ContentResolver mResolver;
        private final FragmentManager mFragmentManager;

        public IconBadgingObserver(ButtonPreference badgingPref, ContentResolver resolver,
                FragmentManager fragmentManager) {
            super(resolver);
            mBadgingPref = badgingPref;
            mResolver = resolver;
            mFragmentManager = fragmentManager;
        }

        @Override
        public void onSettingChanged(boolean enabled) {
            int summary = enabled ? R.string.icon_badging_desc_on : R.string.icon_badging_desc_off;

            boolean serviceEnabled = true;
            if (enabled) {
                // Check if the listener is enabled or not.
                String enabledListeners =
                        Settings.Secure.getString(mResolver, NOTIFICATION_ENABLED_LISTENERS);
                ComponentName myListener =
                        new ComponentName(mBadgingPref.getContext(), NotificationListener.class);
                serviceEnabled = enabledListeners != null &&
                        (enabledListeners.contains(myListener.flattenToString()) ||
                                enabledListeners.contains(myListener.flattenToShortString()));
                if (!serviceEnabled) {
                    summary = R.string.title_missing_notification_access;
                }
            }
            mBadgingPref.setWidgetFrameVisible(!serviceEnabled);
            mBadgingPref.setOnPreferenceClickListener(serviceEnabled ? null : this);
            mBadgingPref.setSummary(summary);

        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            new NotificationAccessConfirmation().show(mFragmentManager, "notification_access");
            return true;
        }
    }

    public static class NotificationAccessConfirmation
            extends DialogFragment implements DialogInterface.OnClickListener {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();
            String msg = context.getString(R.string.msg_missing_notification_access,
                    context.getString(R.string.derived_app_name));
            return new AlertDialog.Builder(context)
                    .setTitle(R.string.title_missing_notification_access)
                    .setMessage(msg)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.title_change_settings, this)
                    .create();
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            ComponentName cn = new ComponentName(getActivity(), NotificationListener.class);
            Bundle showFragmentArgs = new Bundle();
            showFragmentArgs.putString(EXTRA_FRAGMENT_ARG_KEY, cn.flattenToString());

            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra(EXTRA_FRAGMENT_ARG_KEY, cn.flattenToString())
                    .putExtra(EXTRA_SHOW_FRAGMENT_ARGS, showFragmentArgs);
            getActivity().startActivity(intent);
        }
    }


}
