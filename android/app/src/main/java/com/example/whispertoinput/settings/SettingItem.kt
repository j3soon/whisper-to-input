package com.example.whispertoinput.settings

import android.content.Context
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.core.widget.doOnTextChanged
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.whispertoinput.R
import com.example.whispertoinput.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

// The abstract class for setting widget management.
// setup(): Initialize data from dataStore (if any),
//          and configure when to setIsDirty() (usually upon edited).
// apply(): write to settings if the widget is dirty.
// Dirtiness can be set, retrieved and reset.
abstract class SettingItem(private val btnApply: Button) {
    private var isDirty: Boolean = false
    private var ignoringDirtiness: Boolean = false

    // Should be used in a CoroutineScope
    abstract suspend fun apply(context: Context)

    // Should be used in a CoroutineScope
    abstract suspend fun setup(context: Context)

    fun getIsDirty(): Boolean {
        return isDirty
    }

    protected fun setIsDirty() {
        if (ignoringDirtiness) {
            return
        }
        isDirty = true
        btnApply.isEnabled = true
    }

    fun resetIsDirty() {
        isDirty = false
    }

    protected fun setIgnoringDirtiness(ignoringDirtiness: Boolean) {
        this.ignoringDirtiness = ignoringDirtiness
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

// Setting widgets for editable text.
// Pass in the inflated view (currently settings_text.xml),
// text label, description, input field hint, and a data store key.
class SettingText(
    btnApply: Button,
    private val view: View,
    private val label: String,
    private val desc: String,
    private val hint: String,
    private val preferenceKey: Preferences.Key<String>
) : SettingItem(btnApply) {

    override suspend fun setup(context: Context) {
        // Ignore dirtiness until fully set up
        setIgnoringDirtiness(true)

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
            setIgnoringDirtiness(false)
        }
        textField.isEnabled = true
    }

    override suspend fun apply(context: Context) {
        if (!getIsDirty()) return

        val newValue: String = view.findViewById<EditText>(R.id.field).text.toString()
        writeSetting(context, preferenceKey, newValue)
    }
}

// Setting widgets for dropdown menus.
// Pass in the inflated view (currently settings_dropdown.xml),
// text label, description, a list of options, and a data store key.
class SettingDropdown<T>(
    btnApply: Button,
    private val view: View,
    private val label: String,
    private val desc: String,
    private val options: ArrayList<Option<T>>,
    private val preferenceKey: Preferences.Key<T>
) : SettingItem(btnApply), AdapterView.OnItemSelectedListener {

    class Option<T>(private val label: String, private val value: T) {
        fun getLabel() = label
        fun getValue() = value
    }

    private val valueToIdx: HashMap<T, Int> = HashMap<T, Int>()

    override suspend fun setup(context: Context) {
        // Ignore dirtiness until fully set up
        setIgnoringDirtiness(true)

        // Assign components
        view.findViewById<TextView>(R.id.label).text = label
        view.findViewById<TextView>(R.id.description).text = desc

        // Build value to index
        options.forEachIndexed { index, option ->
            valueToIdx[option.getValue()] = index
        }

        // Configure spinner & assign onEdit callback
        val spinner = view.findViewById<Spinner>(R.id.spinner)

        // Add options
        val spinnerList: List<String> = options.map { option ->
            option.getLabel()
        }

        val spinnerArrayAdapter: ArrayAdapter<String> =
            ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, spinnerList)
        spinner.adapter = spinnerArrayAdapter

        spinner.isEnabled = false
        spinner.onItemSelectedListener = this

        // Read data
        val value: T? = readSetting(context, preferenceKey)
        if (value != null) {
            spinner.setSelection(valueToIdx[value]!!)
            setIgnoringDirtiness(false)
        }
        spinner.isEnabled = true
    }

    override suspend fun apply(context: Context) {
        if (!getIsDirty()) return

        val selectedOption = options[view.findViewById<Spinner>(R.id.spinner).selectedItemPosition]
        val newValue: T = selectedOption.getValue()
        writeSetting(context, preferenceKey, newValue)
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        setIsDirty()
    }

    override fun onNothingSelected(parent: AdapterView<*>?) { }
}
