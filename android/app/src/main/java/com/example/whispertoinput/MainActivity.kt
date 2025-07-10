/*
 * This file is part of Whisper To Input, see <https://github.com/j3soon/whisper-to-input>.
 *
 * Copyright (c) 2023-2024 Yan-Bin Diau, Johnson Sun
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
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

// 200 and 201 are an arbitrary values, as long as they do not conflict with each other
private const val MICROPHONE_PERMISSION_REQUEST_CODE = 200
private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 201
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
val REQUEST_STYLE = stringPreferencesKey("request-style")
val ENDPOINT = stringPreferencesKey("endpoint")
val LANGUAGE_CODE = stringPreferencesKey("language-code")
val API_KEY = stringPreferencesKey("api-key")
val MODEL = stringPreferencesKey("model")
val AUTO_RECORDING_START = booleanPreferencesKey("is-auto-recording-start")
val POSTPROCESSING = stringPreferencesKey("postprocessing")

class MainActivity : AppCompatActivity() {
    private var setupSettingItemsDone: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupSettingItems()
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
        val permission_and_code = arrayOf(
            Pair(Manifest.permission.RECORD_AUDIO, MICROPHONE_PERMISSION_REQUEST_CODE),
            Pair(Manifest.permission.POST_NOTIFICATIONS, NOTIFICATION_PERMISSION_REQUEST_CODE),
        )
        for ((permission, code) in permission_and_code) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) == PackageManager.PERMISSION_DENIED
            ) {
                // Shows a popup for permission request.
                // If the permission has been previously (hard-)denied, the popup will not show.
                // onRequestPermissionsResult will be called in either case.
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(permission),
                    code
                )
            }
        }
    }

    // Handles the results of permission requests.
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        var msg: String

        // Only handles requests marked with the unique code.
        if (requestCode == MICROPHONE_PERMISSION_REQUEST_CODE) {
            msg = getString(R.string.mic_permission_required)
        } else if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            msg = getString(R.string.notification_permission_required)
        } else {
            return
        }

        // All permissions should be granted.
        for (result in grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                return
            }
        }
    }

    // Below are settings related functions
    abstract inner class SettingItem() {
        protected var isDirty: Boolean = false
        abstract fun setup() : Job
        abstract suspend fun apply()
        protected suspend fun <T> readSetting(key: Preferences.Key<T>): T? {
            // work is moved to `Dispatchers.IO` under the hood
            // Ref: https://developer.android.com/codelabs/android-preferences-datastore#3
            return dataStore.data.map { preferences ->
                preferences[key]
            }.first()
        }
        protected suspend fun <T> writeSetting(key: Preferences.Key<T>, newValue: T) {
            // work is moved to `Dispatchers.IO` under the hood
            // Ref: https://developer.android.com/codelabs/android-preferences-datastore#3
            dataStore.edit { settings ->
                settings[key] = newValue
            }
        }
    }

    inner class SettingText(
        private val viewId: Int,
        private val preferenceKey: Preferences.Key<String>,
        private val defaultValue: String = ""
    ): SettingItem() {
        override fun setup(): Job {
            return CoroutineScope(Dispatchers.Main).launch {
                val btnApply: Button = findViewById(R.id.btn_settings_apply)
                val editText = findViewById<EditText>(viewId)
                editText.isEnabled = false
                editText.doOnTextChanged { _, _, _, _ ->
                    if (!setupSettingItemsDone) return@doOnTextChanged
                    isDirty = true
                    btnApply.isEnabled = true
                }

                // Read data. If none, apply default value.
                val settingValue: String? = readSetting(preferenceKey)
                val value: String = settingValue ?: defaultValue
                if (settingValue == null) {
                    writeSetting(preferenceKey, defaultValue)
                }
                editText.setText(value)
                editText.isEnabled = true
            }
        }
        override suspend fun apply() {
            if (!isDirty) return
            val newValue: String = findViewById<EditText>(viewId).text.toString()
            writeSetting(preferenceKey, newValue)
            isDirty = false
        }
    }

    inner class SettingDropdown(
        private val viewId: Int,
        private val preferenceKey: Preferences.Key<Boolean>,
        private val stringToValue: HashMap<String, Boolean>,
        private val defaultValue: Boolean = true
    ): SettingItem() {
        override fun setup(): Job {
            return CoroutineScope(Dispatchers.Main).launch {
                val btnApply: Button = findViewById(R.id.btn_settings_apply)
                val spinner = findViewById<Spinner>(viewId)
                spinner.isEnabled = false
                spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                        if (!setupSettingItemsDone) return
                        isDirty = true
                        btnApply.isEnabled = true
                    }
                    override fun onNothingSelected(parent: AdapterView<*>) { }
                }

                val valueToString = stringToValue.map { (k, v) -> v to k }.toMap()
                // Read data. If none, apply default value.
                val settingValue: Boolean? = readSetting(preferenceKey)
                val value: Boolean = settingValue ?: defaultValue
                val string: String = valueToString[value]!!
                if (settingValue == null) {
                    writeSetting(preferenceKey, defaultValue)
                }
                val index: Int? = (0 until spinner.adapter.count).firstOrNull {
                    spinner.adapter.getItem(it) == string
                }
                spinner.setSelection(index!!, false)
                spinner.isEnabled = true
            }
        }
        override suspend fun apply() {
            if (!isDirty) return
            val selectedItem = findViewById<Spinner>(viewId).selectedItem
            val newValue: Boolean = stringToValue[selectedItem]!!
            writeSetting(preferenceKey, newValue)
            isDirty = false
        }
    }

    inner class SettingStringDropdown(
        private val viewId: Int,
        private val preferenceKey: Preferences.Key<String>,
        private val optionValues: List<String>,
        private val defaultValue: String = ""
    ): SettingItem() {
        override fun setup(): Job {
            return CoroutineScope(Dispatchers.Main).launch {
                val btnApply: Button = findViewById(R.id.btn_settings_apply)
                val spinner = findViewById<Spinner>(viewId)
                spinner.isEnabled = false
                spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                        if (!setupSettingItemsDone) return
                        isDirty = true
                        btnApply.isEnabled = true
                        // Deal with individual spinner
                        if (parent.id == R.id.spinner_request_style) {
                            val selectedItem = parent.getItemAtPosition(pos)
                            if (selectedItem == getString(R.string.settings_option_openai_api)) {
                                val endpointEditText: EditText = findViewById<EditText>(R.id.field_endpoint)
                                endpointEditText.setText(getString(R.string.settings_option_openai_api_default_endpoint))
                                val modelEditText: EditText = findViewById<EditText>(R.id.field_model)
                                modelEditText.setText(getString(R.string.settings_option_openai_api_default_model))
                            } else if (selectedItem == getString(R.string.settings_option_whisper_webservice)) {
                                val endpointEditText: EditText = findViewById<EditText>(R.id.field_endpoint)
                                endpointEditText.setText(getString(R.string.settings_option_whisper_webservice_default_endpoint))
                                val modelEditText: EditText = findViewById<EditText>(R.id.field_model)
                                modelEditText.setText(getString(R.string.settings_option_whisper_webservice_default_model))
                            } else if (selectedItem == getString(R.string.settings_option_nvidia_nim)) {
                                val endpointEditText: EditText = findViewById<EditText>(R.id.field_endpoint)
                                endpointEditText.setText(getString(R.string.settings_option_nvidia_nim_default_endpoint))
                                val modelEditText: EditText = findViewById<EditText>(R.id.field_model)
                                modelEditText.setText(getString(R.string.settings_option_nvidia_nim_default_model))
                            }
                        }
                    }
                    override fun onNothingSelected(parent: AdapterView<*>) { }
                }

                // Read data. If none, apply default value.
                val settingValue: String? = readSetting(preferenceKey)
                val value: String = settingValue ?: defaultValue
                if (settingValue == null) {
                    writeSetting(preferenceKey, defaultValue)
                }
                val index: Int? = (0 until spinner.adapter.count).firstOrNull {
                    spinner.adapter.getItem(it) == value
                }
                spinner.setSelection(index ?: 0, false)
                spinner.isEnabled = true
            }
        }
        override suspend fun apply() {
            if (!isDirty) return
            val selectedItem = findViewById<Spinner>(viewId).selectedItem
            val newValue: String = selectedItem.toString()
            writeSetting(preferenceKey, newValue)
            isDirty = false
        }
    }

    private fun setupSettingItems() {
        setupSettingItemsDone = false
        // Add setting items here to apply functions to them
        CoroutineScope(Dispatchers.Main).launch {
            val settingItems = arrayOf(
                SettingStringDropdown(R.id.spinner_request_style, REQUEST_STYLE, listOf(
                    getString(R.string.settings_option_openai_api),
                    getString(R.string.settings_option_whisper_webservice),
                    getString(R.string.settings_option_nvidia_nim)
                ), getString(R.string.settings_option_openai_api)),
                SettingText(R.id.field_endpoint, ENDPOINT, getString(R.string.settings_option_openai_api_default_endpoint)),
                SettingText(R.id.field_language_code, LANGUAGE_CODE),
                SettingText(R.id.field_api_key, API_KEY),
                SettingText(R.id.field_model, MODEL, getString(R.string.settings_option_openai_api_default_model)),
                SettingDropdown(R.id.spinner_auto_recording_start, AUTO_RECORDING_START, hashMapOf(
                    getString(R.string.settings_option_yes) to true,
                    getString(R.string.settings_option_no) to false,
                )),
                SettingStringDropdown(R.id.spinner_postprocessing, POSTPROCESSING, listOf(
                    getString(R.string.settings_option_no_conversion),
                    getString(R.string.settings_option_to_simplified),
                    getString(R.string.settings_option_to_traditional)
                ), getString(R.string.settings_option_to_traditional)),
            )
            val btnApply: Button = findViewById(R.id.btn_settings_apply)
            btnApply.isEnabled = false
            btnApply.setOnClickListener {
                CoroutineScope(Dispatchers.Main).launch {
                    btnApply.isEnabled = false
                    for (settingItem in settingItems) {
                        settingItem.apply()
                    }
                    btnApply.isEnabled = false
                }
                Toast.makeText(this@MainActivity, R.string.successfully_set, Toast.LENGTH_SHORT).show()
            }
            settingItems.map { settingItem -> settingItem.setup() }.joinAll()
            setupSettingItemsDone = true
        }
    }
}
