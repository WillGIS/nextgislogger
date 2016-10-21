/******************************************************************************
 * Project: NextGIS Logger
 * Purpose: Productive data logger for Android
 * Author:  Stanislav Petriakov, becomeglory@gmail.com
 ******************************************************************************
 * Copyright © 2014-2016 NextGIS
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *****************************************************************************/
package com.nextgis.logger;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.keenfin.sfcdialog.SimpleFileChooser;
import com.nextgis.logger.UI.IntEditTextPreference;
import com.nextgis.logger.engines.ArduinoEngine;
import com.nextgis.logger.util.AudioCalibratePreference;
import com.nextgis.logger.util.LoggerConstants;
import com.nextgis.logger.util.FileUtil;

import java.io.File;

public class PreferencesActivity extends PreferenceActivity {
	public static final int minPeriodSec = 1;
	public static final int maxPeriodSec = 3600;
	public static final int CHOOSE_FILE = 53;

	@Override
	protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        Intent intent = getIntent();

        if (intent != null) {
            String action = intent.getAction();

            if (action != null && action.equalsIgnoreCase(Intent.ACTION_GET_CONTENT)) {
                SimpleFileChooser sfcDialog = new SimpleFileChooser();

                sfcDialog.setOnChosenListener(new SimpleFileChooser.SimpleFileChooserListener() {
                    String info = getString(R.string.error_no_file);

                    @Override
                    public void onFileChosen(File file) {
                        Intent result = new Intent("com.example.RESULT_ACTION", Uri.parse("file://" + file.getPath()));
                        setResult(Activity.RESULT_OK, result);
                        finish();
                    }

                    @Override
                    public void onDirectoryChosen(File directory) {
                        finishWithError();
                    }

                    @Override
                    public void onCancel() {
                        finishWithError();
                    }

                    private void finishWithError() {
                        Toast.makeText(getApplicationContext(), info, Toast.LENGTH_SHORT).show();
                        Intent result = new Intent("com.example.RESULT_ACTION");
                        setResult(Activity.RESULT_OK, result);
                        finish();
                    }
                });

                if (getActionBar() != null)
                    getActionBar().hide();

                getWindow().setBackgroundDrawable(null);

                sfcDialog.show(getFragmentManager(), "SimpleFileChooserDialog");
                return;
            }
        }

        if (getActionBar() != null)
            getActionBar().setDisplayHomeAsUpEnabled(true);

		getFragmentManager().beginTransaction().replace(android.R.id.content, new PreferencesFragment()).commit();
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

	public static class PreferencesFragment extends PreferenceFragment implements OnPreferenceChangeListener {
		@SuppressWarnings("deprecation")
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			addPreferencesFromResource(R.xml.preferences);

			final Activity parent = getActivity();

            LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

            if (!locationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER)) {
                findPreference(LoggerConstants.PREF_GPS).setEnabled(false);
                ((CheckBoxPreference) findPreference(LoggerConstants.PREF_GPS)).setChecked(false);
                findPreference(LoggerConstants.PREF_GPS).setSummary(R.string.settings_sensor_sum);
            }

			SensorManager sm = (SensorManager) parent.getSystemService(Context.SENSOR_SERVICE);

			if (sm.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) == null) {
                findPreference(LoggerConstants.PREF_SENSOR_MODE).setEnabled(false);
                findPreference(LoggerConstants.PREF_SENSOR_MODE).setSummary(R.string.settings_sensor_sum);
            }

			if (sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) == null) {
                findPreference(LoggerConstants.PREF_SENSOR_MAG).setEnabled(false);
                ((CheckBoxPreference) findPreference(LoggerConstants.PREF_SENSOR_MAG)).setChecked(false);
                findPreference(LoggerConstants.PREF_SENSOR_MAG).setSummary(R.string.settings_sensor_sum);
            }

			if (sm.getDefaultSensor(Sensor.TYPE_ORIENTATION) == null) {
                findPreference(LoggerConstants.PREF_SENSOR_ORIENT).setEnabled(false);
                ((CheckBoxPreference) findPreference(LoggerConstants.PREF_SENSOR_ORIENT)).setChecked(false);
                findPreference(LoggerConstants.PREF_SENSOR_ORIENT).setSummary(R.string.settings_sensor_sum);
            }

			if (sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE) == null) {
                findPreference(LoggerConstants.PREF_SENSOR_GYRO).setEnabled(false);
                ((CheckBoxPreference) findPreference(LoggerConstants.PREF_SENSOR_GYRO)).setChecked(false);
                findPreference(LoggerConstants.PREF_SENSOR_GYRO).setSummary(R.string.settings_sensor_sum);
            }

            AudioCalibratePreference audio = (AudioCalibratePreference) findPreference(LoggerConstants.PREF_MIC_DELTA);
            audio.setSummary();

			if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN_MR1)
				findPreference(LoggerConstants.PREF_USE_API17).setEnabled(false);

			EditTextPreference userName = (EditTextPreference) findPreference(LoggerConstants.PREF_USER_NAME);
			userName.setSummary(userName.getText());
			userName.setOnPreferenceChangeListener(this);

			IntEditTextPreference periodPreference = (IntEditTextPreference) findPreference(LoggerConstants.PREF_PERIOD_SEC);
			periodPreference.setSummary(getString(R.string.settings_period_sum) + periodPreference.getPersistedString("1"));
			periodPreference.setOnPreferenceChangeListener(this);

			Preference catPathPreference = findPreference(LoggerConstants.PREF_CAT_PATH);

			catPathPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(final Preference preference) {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("file/*");
                    startActivityForResult(intent, CHOOSE_FILE);

                    return true;
                }
            });

            final Preference selectExternalDevice = findPreference(LoggerConstants.PREF_EXTERNAL_DEVICE);
            selectExternalDevice.setSummary(PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(LoggerConstants.PREF_EXTERNAL_DEVICE, null));

            selectExternalDevice.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(final Preference preference) {
                    AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
                    final ArduinoEngine engine = LoggerApplication.getApplication().getArduinoEngine();

                    if (!engine.isBTEnabled()) {
                        dialog.setTitle(R.string.external_goto_settings);
                        dialog.setMessage(R.string.external_bt_disabled);
                    } else {
                        dialog.setTitle(R.string.external_paired);
                        final ArrayAdapter<String> devicesNames = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item);

                        for (BluetoothDevice device : engine.getPairedDevices())
                            devicesNames.add(device.getName() + " (" + device.getAddress() + ")");

                        dialog.setAdapter(devicesNames,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        String name = devicesNames.getItem(which);
                                        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
                                                .putString(LoggerConstants.PREF_EXTERNAL_DEVICE, name).commit();
                                        engine.setDeviceMAC(engine.splitDeviceMAC(name));
                                        engine.setDeviceName(engine.splitDeviceName(name));
                                        selectExternalDevice.setSummary(name);
                                    }
                                });
                    }

                    dialog.setNegativeButton(android.R.string.cancel, null);
                    dialog.setPositiveButton(R.string.app_settings,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent btSettings = new Intent();
                                    btSettings.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                                    startActivity(btSettings);
                                }
                            });

                    dialog.show();

                    return true;
                }
            });
		}

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == CHOOSE_FILE)
                FileUtil.copyPreset(getActivity(), data);
            else
                super.onActivityResult(requestCode, resultCode, data);
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            switch (preference.getKey()) {
                case LoggerConstants.PREF_USER_NAME:
                    preference.setSummary((String) newValue);
                    return true;
                case LoggerConstants.PREF_PERIOD_SEC:
                    int period;

                    try {
                        period = Integer.parseInt((String) newValue);
                        boolean max = period > maxPeriodSec;
                        boolean min = period < minPeriodSec;

                        if (max)
                            period = maxPeriodSec;

                        if (min)
                            period = minPeriodSec;

                        ((IntEditTextPreference) preference).persistString(Integer.toString(period));
                        preference.setSummary(getString(R.string.settings_period_sum) + period);

                        if (min || max)
                            throw new IllegalArgumentException();
                    } catch (Exception e) {
                        Toast.makeText(preference.getContext(), R.string.settings_period_toast, Toast.LENGTH_LONG).show();
                    }

                    return false;
            }

            return false;
        }
    }
}
