/*
 * This file is part of Whisper To Input, see <https://github.com/j3soon/whisper-to-input>.
 *
 * Copyright (c) 2023 Yan-Bin Diau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.example.whispertoinput

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.provider.*
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.whispertoinput.recorder.RecorderManager
import com.example.whispertoinput.settings.SettingsPage
import com.example.whispertoinput.settings.SettingsPageBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

private const val MICROPHONE_PERMISSION_REQUEST_CODE = 200
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
val ENDPOINT = stringPreferencesKey("endpoint")
val LANGUAGE_CODE = stringPreferencesKey("language-code")
val REQUEST_STYLE = booleanPreferencesKey("is-openai-api-request-style")
val API_KEY = stringPreferencesKey("api-key")

class MainActivity : AppCompatActivity() {
    private var settingsPage: SettingsPage? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val settingsList: LinearLayout = findViewById(R.id.settings_list)
        val btnApply: Button = findViewById(R.id.btn_settings_apply)
        val builder = SettingsPageBuilder(this, btnApply, layoutInflater, settingsList, R.xml.settings)
        settingsPage = builder.build()

        checkPermissions()
    }

    // The onClick event of the grant permission button.
    // Opens up the app settings panel to manually configure permissions.
    fun onRequestMicrophonePermission(view: View) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        with(intent) {
            data = Uri.fromParts("package", packageName, null)
            addCategory(Intent.CATEGORY_DEFAULT)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        }

        startActivity(intent)
    }

    // Checks whether permissions are granted. If not, automatically make a request.
    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_DENIED
        ) {
            // Shows a popup for permission request.
            // If the permission has been previously (hard-)denied, the popup will not show.
            // onRequestPermissionsResult will be called in either case.
            ActivityCompat.requestPermissions(
                this,
                RecorderManager.requiredPermissions(),
                MICROPHONE_PERMISSION_REQUEST_CODE
            )
        }
    }

    // Handles the results of permission requests.
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Only handles requests marked with the unique code.
        if (requestCode != MICROPHONE_PERMISSION_REQUEST_CODE) {
            return
        }

        // All permissions should be granted.
        for (result in grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                    this,
                    getString(R.string.mic_permission_required),
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
        }
    }

}