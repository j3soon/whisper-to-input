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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MICROPHONE_PERMISSION_REQUEST_CODE = 200
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
val ENDPOINT = stringPreferencesKey("endpoint")
val LANGUAGE_CODE = stringPreferencesKey("language-code")

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
        // TODO: Refactor. Perhaps use a class to process configuration UI and behaviors.
        // Launches a non-blocking job in the main thread.
        // Perform data retrieval in the IO thread.
        val endpointInput: EditText = findViewById(R.id.edittext_endpoint)
        val btnSetEndpoint: Button = findViewById(R.id.btn_set_endpoint)
        val languageCodeInput: EditText = findViewById(R.id.edittext_language_code)
        val btnSetLanguageCode: Button = findViewById(R.id.btn_set_language_code)

        CoroutineScope(Dispatchers.Main).launch {

            // Disable input & button, and show loading hint
            endpointInput.isEnabled = false
            endpointInput.hint = getString(R.string.loading)
            btnSetEndpoint.isEnabled = false
            languageCodeInput.isEnabled = false
            languageCodeInput.hint = getString(R.string.loading)
            btnSetLanguageCode.isEnabled = false

            // Retrieve stored endpoint & language code
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

            // Set retrieved endpoint in input, or set hint
            if (retrievedEndpoint.isNullOrEmpty()) {
                endpointInput.hint = getString(R.string.endpoint_hint)
            } else {
                endpointInput.setText(retrievedEndpoint)
            }

            // Set retrieved endpoint input, or set hint
            // TODO: This could a dropdown list?
            if (retrievedLanguageCode.isNullOrEmpty()) {
                languageCodeInput.hint = getString(R.string.language_code_hint)
            } else {
                languageCodeInput.setText(retrievedLanguageCode)
            }

            // Re-enable input & button
            endpointInput.isEnabled = true
            btnSetEndpoint.isEnabled = true
            languageCodeInput.isEnabled = true
            btnSetLanguageCode.isEnabled = true

            // After retrieval is done, assign onClick event to the set buttons
            btnSetEndpoint.setOnClickListener { onSetConfig(context, ENDPOINT, endpointInput.text.toString()) }
            btnSetLanguageCode.setOnClickListener { onSetConfig(context, LANGUAGE_CODE, languageCodeInput.text.toString()) }
        }
    }

    // The onClick event of set config buttons
    private fun <T>onSetConfig(context: Context, key: Preferences.Key<T>, newValue: T) {
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