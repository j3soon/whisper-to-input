package com.example.whispertoinput.settings

import android.content.Context
import android.content.res.XmlResourceParser
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.whispertoinput.R


class SettingsPageBuilder(
    private val context: Context,
    private val btnApply: Button,
    private val layoutInflater: LayoutInflater,
    private val settingsList: LinearLayout,
    private val settingsResId: Int
) {

    fun build() : SettingsPage {
        // Initialize an XmlResourceParser from a resource id
        val parser: XmlResourceParser = context.resources.getXml(settingsResId)
        val settingsPage: SettingsPage = SettingsPage(context, btnApply)

        var tag: String? = null
        var tagType: String? = null
        var event = parser.eventType
        while (event != XmlResourceParser.END_DOCUMENT) {
            // Retrieve current tag and tag type
            tag = parser.name
            tagType = parser.getAttributeValue(null, "type")

            // Parse tag starts
            event = if (event == XmlResourceParser.START_TAG && tag == "setting" && !tagType.isNullOrEmpty()) {
                when (tagType) {
                    "text" -> buildText(parser, settingsPage)
                    "dropdown" -> buildDropdown(parser, settingsPage)
                    else -> throw Exception("Unknown tag type")
                }
            } else {
                parser.next()
            }
        }

        settingsPage.setup()
        return settingsPage
    }

    // Inflate and initialize a text setting field.
    private fun buildText(parser: XmlResourceParser, settingsPage: SettingsPage): Int {
        val view: View = layoutInflater.inflate(R.layout.settings_text, settingsList, false)
        val label: String = attrToString(parser, "label")
        val desc: String = attrToString(parser, "desc")
        val hint: String = attrToString(parser, "hint")
        val preferenceKey: Preferences.Key<String> = stringPreferencesKey(attrToString(parser, "dataStoreKey"))
        val settingText = SettingText(btnApply, view, label, desc, hint, preferenceKey)

        settingsPage.add(settingText)
        settingsList.addView(view)

        return parser.next()
    }

    // Inflate and set up a dropdown setting field.
    private fun buildDropdown(parser: XmlResourceParser, settingsPage: SettingsPage): Int {
        val view: View = layoutInflater.inflate(R.layout.settings_dropdown, settingsList, false)
        val label: String = attrToString(parser, "label")
        val desc: String = attrToString(parser, "desc")
        val dataStoreType: String = parser.getAttributeValue(null, "dataStoreType")

        // Process dropdown options
        val spinner = view.findViewById<Spinner>(R.id.spinner)

        when (dataStoreType) {
            "bool" -> {
                val options = gatherDropdownOptions(parser) { str -> str.toBoolean() }
                val spinnerArrayAdapter: ArrayAdapter<String> =
                    ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, options.map { option -> option.getLabel() })
                val preferenceKey: Preferences.Key<Boolean> = booleanPreferencesKey(attrToString(parser, "dataStoreKey"))
                val settingDropdown: SettingDropdown<Boolean> = SettingDropdown(btnApply, view, label, desc, options, preferenceKey)

                spinner.adapter = spinnerArrayAdapter
                settingsPage.add(settingDropdown)
            }

            // TODO: Other types if necessary
            else -> throw Exception("Unknown dropdown settings type")
        }

        settingsList.addView(view)
        return parser.next()
    }

    // Expects the parser to have one or multiple incoming <item>s
    // Stops at the first </setting>
    private fun <T> gatherDropdownOptions(parser: XmlResourceParser, evaluator: (String) -> T) : ArrayList<SettingDropdown.Option<T>> {
        val options: ArrayList<SettingDropdown.Option<T>> = ArrayList()
        var event: Int = parser.eventType

        while (true) {
            event = if (event == XmlResourceParser.END_DOCUMENT) {
                throw Exception("Invalid document format")
            } else if (event == XmlResourceParser.START_TAG && parser.name == "item") {
                val label: String = attrToString(parser, "label")
                val value: String = attrToString(parser, "value")
                options.add(SettingDropdown.Option(label, evaluator(value)))
                parser.next()
            } else if (event == XmlResourceParser.END_TAG && parser.name == "setting") {
                return options
            } else {
                parser.next()
            }
        }
    }

    // Supports both string literal and string resource in attributes
    private fun attrToString(parser: XmlResourceParser, attribute: String): String {
        val emptyString: String = context.getString(R.string.empty_string)
        val attributeResource: String = context.getString(
            parser.getAttributeResourceValue(
                null,
                attribute,
                R.string.empty_string
            )
        )

        // attributeResource is obtained via context.getString
        // It is either the actual (parsed) string resource, or an empty string
        // (when attribute does not exist, or when attribute is a string literal)
        // If it is an empty string, we instead return the string literal.
        return if (attributeResource == emptyString) {
            parser.getAttributeValue(null, attribute) ?: ""
        } else {
            attributeResource
        }
    }
}