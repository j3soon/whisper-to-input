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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupConfigWidgets(this)
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

    // Sets up config widgets.
    private fun setupConfigWidgets(context: Context) {
        // TODO: Refactor. Perhaps use a class to process configuration UI widgets and behaviors.
        // Launches a non-blocking job in the main thread.
        // Perform data retrieval in the IO thread.
        val endpointInput: EditText = findViewById(R.id.edittext_endpoint)
        val btnSetEndpoint: Button = findViewById(R.id.btn_set_endpoint)
        val languageCodeInput: EditText = findViewById(R.id.edittext_language_code)
        val btnSetLanguageCode: Button = findViewById(R.id.btn_set_language_code)
        val apiKeyInput: EditText = findViewById(R.id.edittext_api_key)
        val btnSetApiKey: Button = findViewById(R.id.btn_set_api_key)
        val requestStyleOption : RadioGroup = findViewById(R.id.radio_request_style)
        val btnGenLogcat : Button = findViewById(R.id.btn_gen_logcat)

        CoroutineScope(Dispatchers.Main).launch {

            // Disable inputs, buttons & controls, and show loading hint
            endpointInput.isEnabled = false
            endpointInput.hint = getString(R.string.loading)
            btnSetEndpoint.isEnabled = false
            languageCodeInput.isEnabled = false
            languageCodeInput.hint = getString(R.string.loading)
            btnSetLanguageCode.isEnabled = false
            apiKeyInput.hint = getString(R.string.loading)
            btnSetApiKey.isEnabled = false
            requestStyleOption.isEnabled = false

            // Retrieve stored endpoint, language code, api key & request style
            val retrievedEndpoint = withContext(Dispatchers.IO) {
                return@withContext dataStore.data.map { preferences ->
                    preferences[ENDPOINT]
                }.first()
            }

            val retrievedLanguageCode = withContext(Dispatchers.IO) {
                return@withContext dataStore.data.map { preferences ->
                    preferences[LANGUAGE_CODE]
                }.first()
            }

            val retrievedRequestStyle = withContext(Dispatchers.IO) {
                return@withContext dataStore.data.map { preferences ->
                    preferences[REQUEST_STYLE]
                }.first()
            }

            val retrievedApiKey = withContext(Dispatchers.IO) {
                return@withContext dataStore.data.map { preferences ->
                    preferences[API_KEY]
                }.first()
            }

            // Set retrieved endpoint in input, or set hint
            if (retrievedEndpoint.isNullOrEmpty()) {
                endpointInput.hint = getString(R.string.endpoint_hint)
            } else {
                endpointInput.setText(retrievedEndpoint)
            }

            // Set retrieved endpoint input, or set hint
            // TODO: This could a dropdown list? Or radio group?
            if (retrievedLanguageCode.isNullOrEmpty()) {
                languageCodeInput.hint = getString(R.string.language_code_hint)
            } else {
                languageCodeInput.setText(retrievedLanguageCode)
            }

            // Set retrieved request style, or assign a default
            if (retrievedRequestStyle == null) {
                dataStore.edit { settings ->
                    settings[REQUEST_STYLE] = true
                }
                requestStyleOption.check(R.id.radio_btn_openai_api)
            } else if (retrievedRequestStyle) {
                requestStyleOption.check(R.id.radio_btn_openai_api)
            } else {
                requestStyleOption.check(R.id.radio_btn_whisper_webservice)
            }

            // Set retrieved api key
            if (retrievedApiKey.isNullOrEmpty()) {
                apiKeyInput.hint = getString(R.string.api_key_hint)
            } else {
                apiKeyInput.setText(retrievedApiKey)
            }

            // Re-enable input & button
            endpointInput.isEnabled = true
            btnSetEndpoint.isEnabled = true
            languageCodeInput.isEnabled = true
            btnSetLanguageCode.isEnabled = true
            apiKeyInput.isEnabled = true
            btnSetApiKey.isEnabled = true
            requestStyleOption.isEnabled = true

            // After retrieval is done, assign onClick event to the set buttons
            btnSetEndpoint.setOnClickListener { onSetConfig(context, ENDPOINT, endpointInput.text.toString()) }
            btnSetLanguageCode.setOnClickListener { onSetConfig(context, LANGUAGE_CODE, languageCodeInput.text.toString()) }
            requestStyleOption.setOnCheckedChangeListener { _, checkedId ->
                onSetConfig(context, REQUEST_STYLE, (checkedId == R.id.radio_btn_openai_api))
            }
            btnSetApiKey.setOnClickListener { onSetConfig(context, API_KEY, apiKeyInput.text.toString()) }
            btnGenLogcat.setOnClickListener { generateLogcat() }
        }
    }

    private fun generateLogcat() {
        try {
            // If path does not exist, create the path first
            val filepath = File(Environment.getExternalStorageDirectory(), "Documents/WhisperToInput/")
            if (!filepath.exists()) {
                filepath.mkdirs()
            }

            // Create an empty file and use command to write contents into that file
            val filename = File(Environment.getExternalStorageDirectory(), "Documents/WhisperToInput/log.txt")
            val cmd = "logcat -r 300 -f " + filename.absolutePath + " ActivityManager:I"
            filename.createNewFile()
            Runtime.getRuntime().exec(cmd)
            Toast.makeText(this, "Generation successful (at ${filename.absolutePath}).", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Generation failed.", Toast.LENGTH_SHORT).show()
        }
    }

    // The onClick event of set config buttons
    private fun <T> onSetConfig(context: Context, key: Preferences.Key<T>, newValue: T) {
        CoroutineScope(Dispatchers.Main).launch {
            withContext(Dispatchers.IO) {
                dataStore.edit { settings ->
                    settings[key] = newValue
                }
            }

            Toast.makeText(context, getString(R.string.successfully_set), Toast.LENGTH_SHORT).show()
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