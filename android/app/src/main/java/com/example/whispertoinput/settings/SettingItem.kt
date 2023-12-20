package com.example.whispertoinput.settings

import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.whispertoinput.R
import com.example.whispertoinput.REQUEST_STYLE
import com.example.whispertoinput.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class SettingItem(private val btnApply: Button) {
    private var isDirty: Boolean = false

    // Should be used in a CoroutineScope
    abstract suspend fun apply(context: Context)

    // Should be used in a CoroutineScope
    abstract suspend fun setup(context: Context)

    fun getIsDirty(): Boolean {
        return isDirty
    }

    protected fun setIsDirty() {
        isDirty = true
        btnApply.isEnabled = true
    }

    protected suspend fun <T> writeSetting(context: Context, key: Preferences.Key<T>, newValue: T) {
        withContext(Dispatchers.IO) {
            context.dataStore.edit { settings ->
                settings[key] = newValue
            }
        }
    }

    protected suspend fun <T> readSetting(context: Context, key: Preferences.Key<T>): T? {
        return withContext(Dispatchers.IO) {
            return@withContext context.dataStore.data.map { preferences ->
                preferences[key]
            }.first()
        }
    }
}

class SettingText(
    btnApply: Button,
    private val view: View,
    private val label: String,
    private val desc: String,
    private val hint: String,
    private val preferenceKey: Preferences.Key<String>
) : SettingItem(btnApply) {

    override suspend fun setup(context: Context) {
        // Assign components
        view.findViewById<TextView>(R.id.label).text = label
        view.findViewById<TextView>(R.id.description).text = desc

        // Configure text field & assign onEdit callback
        val textField = view.findViewById<EditText>(R.id.field)

        textField.isEnabled = false
        textField.setText(R.string.empty_string)
        textField.hint = hint
        textField.doOnTextChanged { _, _, _, _ ->
            setIsDirty()
        }

        // Read data
        val value: String? = readSetting(context, preferenceKey)
        if (!value.isNullOrEmpty()) {
            textField.setText(value)
        }
        textField.isEnabled = true
    }

    override suspend fun apply(context: Context) {
        if (!getIsDirty()) return

        val newValue: String = view.findViewById<EditText>(R.id.field).text.toString()
        writeSetting(context, preferenceKey, newValue)
    }
}