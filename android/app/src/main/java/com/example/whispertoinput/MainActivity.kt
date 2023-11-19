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
import android.provider.*
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MICROPHONE_PERMISSION_REQUEST_CODE = 200
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
val API_KEY = stringPreferencesKey("api-key")

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupApiKeyWidgets(this)
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

    // Sets up API Key-related widgets.
    private fun setupApiKeyWidgets(context: Context) {
        // Launches a non-blocking job in the main thread.
        // Perform data retrieval in the IO thread.
        val apiKeyInput: EditText = findViewById(R.id.edittext_api_key)
        val btnSetApiKey: Button = findViewById(R.id.btn_set_api_key)

        CoroutineScope(Dispatchers.Main).launch {

            // Disable input & button, and show loading hint
            apiKeyInput.isEnabled = false
            apiKeyInput.hint = getString(R.string.loading)
            btnSetApiKey.isEnabled = false

            // Retrieve Api Key
            val retrievedApiKey = withContext(Dispatchers.IO) {
                return@withContext dataStore.data.map { preferences ->
                    preferences[API_KEY]
                }.first()
            }

            // Set retrieved api key in input, or set "Enter API Key" hint
            if (retrievedApiKey.isNullOrEmpty()) {
                apiKeyInput.hint = getString(R.string.enter_openai_api_key)
            } else {
                apiKeyInput.setText(retrievedApiKey)
            }

            // Re-enable input & button
            apiKeyInput.isEnabled = true
            btnSetApiKey.isEnabled = true

            // After retrieval is done, assign onClick event to the setApiKey button
            btnSetApiKey.setOnClickListener { onSetApiKey(context, apiKeyInput.text.toString()) }
        }
    }

    // The onClick event of the button set api key
    private fun onSetApiKey(context: Context, newApiKey: String?) {
        CoroutineScope(Dispatchers.Main).launch {
            withContext(Dispatchers.IO) {
                dataStore.edit { settings ->
                    settings[API_KEY] = newApiKey ?: ""
                }
            }

            Toast.makeText(context, getString(R.string.api_key_successfully_set), Toast.LENGTH_SHORT).show()
        }
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