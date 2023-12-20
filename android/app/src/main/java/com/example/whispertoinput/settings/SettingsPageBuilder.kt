package com.example.whispertoinput.settings

import android.content.Context
import android.content.res.XmlResourceParser
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.whispertoinput.R


class SettingsPageBuilder(
    private val context: Context,
    private val btnApply: Button,
    private val layoutInflater: LayoutInflater,
    private val settingsList: LinearLayout,
    private val settingsResId: Int
) {

    fun build() {
        // Initialize an XmlResourceParser from a resource id
        val parser: XmlResourceParser = context.resources.getXml(settingsResId)
        val settingsPage: SettingsPage = SettingsPage()

        var tag: String? = null
        var tagType: String? = null
        var event = parser.eventType
        while (event != XmlResourceParser.END_DOCUMENT) {
            // Retrieve current tag and tag type
            tag = parser.name
            tagType = parser.getAttributeValue(null, "type")

            // Parse tag starts
            if (event == XmlResourceParser.START_TAG && tag == "setting" && !tagType.isNullOrEmpty()) {
                event = when (tagType) {
                    "text" -> buildText(parser, settingsPage)
                    "dropdown" -> buildDropdown(parser)
                    else -> throw Exception("Unknown tag type")
                }
            } else {
                event = parser.next()
            }
        }

        settingsPage.setup(context, btnApply)
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
    private fun buildDropdown(parser: XmlResourceParser): Int {
        val dropdownSetting: View =
            layoutInflater.inflate(R.layout.settings_dropdown, settingsList, false)
        dropdownSetting.findViewById<TextView>(R.id.label).text = attrToString(parser, "label")
        dropdownSetting.findViewById<TextView>(R.id.description).text = attrToString(parser, "desc")
        settingsList.addView(dropdownSetting)

        // Process dropdown options
        val spinner = dropdownSetting.findViewById<Spinner>(R.id.spinner)

        val spinnerArray = ArrayList<String>()
        spinnerArray.add("one")
        spinnerArray.add("two")
        spinnerArray.add("three")
        spinnerArray.add("four")
        spinnerArray.add("five")

        val spinnerArrayAdapter: ArrayAdapter<String> =
            ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, spinnerArray)
        spinner.adapter = spinnerArrayAdapter
        spinner.setSelection(2)

        // TODO: Complete all callbacks and events here

        return parser.next()
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