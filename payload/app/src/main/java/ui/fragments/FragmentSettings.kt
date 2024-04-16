/*
    Copyright (C) 2016 sandstranger
    Copyright (C) 2018, 2019 Ilya Zhuravlev

    This file is part of OpenMW-Android.

    OpenMW-Android is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenMW-Android is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenMW-Android.  If not, see <https://www.gnu.org/licenses/>.
*/

package ui.fragments

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.preference.EditTextPreference
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.PreferenceGroup
import androidx.documentfile.provider.DocumentFile
import com.libopenmw.openmw.R
import file.GameInstaller
import ui.activity.ConfigureControls
import ui.activity.MainActivity
import ui.activity.ModsActivity
import ui.activity.SettingsActivity
import android.os.Environment
import utils.MyApp
import java.util.*


class FragmentSettings : PreferenceFragment(), OnSharedPreferenceChangeListener {

    companion object {
        private const val REQUEST_CODE_OPEN_DOCUMENT_TREE = 12345 // Choose any unique request code
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.settings)
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)

        findPreference("pref_controls").setOnPreferenceClickListener {
            val intent = Intent(activity, ConfigureControls::class.java)
            this.startActivity(intent)
            true
        }

        findPreference("pref_game_settings").setOnPreferenceClickListener {
            val intent = Intent(activity, SettingsActivity::class.java)
            this.startActivity(intent)
            true
        }

        findPreference("pref_mods").setOnPreferenceClickListener {
            // Just prevent crash here if data files are not selected
            val sharedPref = preferenceScreen.sharedPreferences
            val inst = GameInstaller(sharedPref.getString("game_files", "")!!)
            if (!inst.check()) {
                AlertDialog.Builder(getActivity())
                    .setTitle(R.string.no_data_files_title)
                    .setMessage(R.string.no_data_files_message)
                    .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int -> }
                    .show()

                false
            }
            else
            {
                val intent = Intent(activity, ModsActivity::class.java)
                this.startActivity(intent)
                true
            }
        }

        findPreference("game_files").setOnPreferenceClickListener {
            // Use ACTION_OPEN_DOCUMENT_TREE intent to allow the user to choose a directory
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            startActivityForResult(intent, REQUEST_CODE_OPEN_DOCUMENT_TREE)
            true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_OPEN_DOCUMENT_TREE && resultCode == Activity.RESULT_OK) {
            data?.data?.also { uri ->

            val selectedDirectory = DocumentFile.fromTreeUri(activity, uri) ?: return
            val storageDir = Environment.getExternalStorageDirectory()
            val storagePath = storageDir.absolutePath
            val path = storagePath + "/" + uri.lastPathSegment?.replace("primary:", "")
            val iniFile = selectedDirectory.findFile("Morrowind.ini")
            val dataFilesFolder = selectedDirectory.findFile("Data Files")
            val sharedPref = preferenceScreen.sharedPreferences

                if (iniFile != null && dataFilesFolder != null && dataFilesFolder.isDirectory) {
                    val gameFilesPreference = findPreference("game_files")
                    gameFilesPreference?.summary = path
                    with(sharedPref.edit()) {
                        putString("game_files", path)
                        apply()
                    }
                } else {
                    val builder = AlertDialog.Builder(activity)
                    val input = EditText(activity)
                    builder.setView(input)
                    builder.setTitle("Enter a path that contains both the Morrowind.ini file and Data Files directory")
                    builder.setPositiveButton("OK") { _, _ ->
                    val pathtext = input.text.toString()
                        if (pathtext != null && !pathtext.isEmpty()) {
                            val iniFile = File(
                                pathtext,
                                "Morrowind.ini"
                            ) // Assuming ini file is named iniFile.ini
                            val dataFilesFolder = File(pathtext, "Data Files")
                            if (iniFile.exists() && dataFilesFolder.isDirectory) {
                                val gameFilesPreference = findPreference("game_files")
                                gameFilesPreference?.summary = pathtext
                                with(sharedPref.edit()) {
                                    putString("game_files", pathtext)
                                    apply()
                                }
                            } else {
                                showError(R.string.data_error_title, R.string.data_error_message)
                                with(sharedPref.edit()) {
                                    putString("game_files", "")
                                    apply()

                                }
                            }
                        }
                }
                    builder.setNegativeButton("Cancel") {
                    dialog, _ -> dialog.cancel()
                        showError(R.string.data_error_title, R.string.data_error_message)
                        with(sharedPref.edit()) {
                            putString("game_files", "")
                            apply()
                        }
                    }
                    builder.show()
                }
            }
        }
    }

    /**
     * Shows an alert dialog displaying a specific error
     * @param title Title string resource
     * @param message Message string resource
     */
    private fun showError(title: Int, message: Int, url: String? = null) {
        val dialog = AlertDialog.Builder(activity)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int -> }

        if (url != null) {
            dialog.setNeutralButton(R.string.dialog_howto) { _, _ ->
                (activity as MainActivity).openUrl(url)
            }
        }
        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        for (i in 0 until preferenceScreen.preferenceCount) {
            val preference = preferenceScreen.getPreference(i)
            if (preference is PreferenceGroup) {
                for (j in 0 until preference.preferenceCount) {
                    val singlePref = preference.getPreference(j)
                    updatePreference(singlePref, singlePref.key)
                }
            } else {
                updatePreference(preference, preference.key)
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        updatePreference(findPreference(key), key)
    }

    private fun updatePreference(preference: Preference?, key: String) {
        if (preference == null)
            return
        if (preference is EditTextPreference) {
            if (key == "pref_uiScaling" && (preference.text == null || preference.text.isEmpty()))
            // Show "Auto (1.23)"
                preference.summary = MyApp.app.getString(R.string.uiScaling_auto)
                    .format(Locale.ROOT, MyApp.app.defaultScaling)
            else
                preference.summary = preference.text
        }
        // Show selected value as a summary for game_files
        if (key == "game_files") {
            preference.summary = preference.sharedPreferences.getString("game_files", "")
        }
    }
}