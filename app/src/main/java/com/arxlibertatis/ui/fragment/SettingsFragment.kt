package com.arxlibertatis.ui.fragment

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.Intent.createChooser
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.preference.Preference
import com.arxlibertatis.R
import com.arxlibertatis.interfaces.SettingsFragmentMvpView
import com.arxlibertatis.presenter.SettingsFragmentPresenter
import com.arxlibertatis.utils.GAME_FILES_SHARED_PREFS_KEY
import com.arxlibertatis.utils.extensions.startActivity
import moxy.MvpView
import moxy.presenter.InjectPresenter

class SettingsFragment : MvpAppCompatFragment(), SettingsFragmentMvpView{

    private val CHOOSE_DIRECTORY_REQUEST_CODE = 4321

    @InjectPresenter
    lateinit var presenter: SettingsFragmentPresenter

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        addPreferencesFromResource(R.xml.settings)

        val gameFilesPreference = findPreference<Preference>(GAME_FILES_SHARED_PREFS_KEY)
        gameFilesPreference?.setOnPreferenceClickListener {
            with(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)) {
                addCategory(Intent.CATEGORY_DEFAULT)
                startActivityForResult(createChooser(this, "Choose directory"),
                    CHOOSE_DIRECTORY_REQUEST_CODE)
            }
            true
        }
        updatePreference(gameFilesPreference!!,GAME_FILES_SHARED_PREFS_KEY)

        findPreference<Preference>("screen_controls_settings")?.setOnPreferenceClickListener {
            presenter.onConfigureScreenControlsClicked(requireContext())
            true
        }

        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.copy_game_assets -> {
                AlertDialog.Builder(this)
                    .setTitle("Confirmation")
                    .setMessage("Are you sure you want to reset asset folder?")
                    .setPositiveButton("Yes") { _, _ ->
                        presenter.copyGameAssets(requireContext(), preferenceScreen.sharedPreferences!!)
                        Toast.makeText(this, "Assets Reset!", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("No", null)
                    .show() // Show the dialog
                true // Return true to indicate the event was handled
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when{
            resultCode != Activity.RESULT_OK -> return
            requestCode == CHOOSE_DIRECTORY_REQUEST_CODE ->
            {
                presenter.saveGamePath(data!!,requireContext(),this.preferenceScreen.sharedPreferences!!)
            }
        }
    }

    override fun updatePreference (prefsKey : String) =
        updatePreference(findPreference(prefsKey)!!,prefsKey)

    private fun updatePreference (preference: Preference, prefsKey: String){
        preference.summary = preferenceScreen.sharedPreferences?.getString(prefsKey, "") ?: ""
    }
}
