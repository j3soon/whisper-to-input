package com.example.whispertoinput.settings

import android.content.Context
import android.content.res.XmlResourceParser
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.marginBottom
import com.example.whispertoinput.R

class SettingsPageBuilder {
    fun build(context: Context, layoutInflater: LayoutInflater, settingsList: LinearLayout, settingsResId: Int) {
        // Initialize an XmlResourceParser from a resource id
        val parser: XmlResourceParser = context.resources.getXml(settingsResId)

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
                    "text" -> buildText(context, parser, layoutInflater, settingsList)
                    "dropdown" -> buildDropdown(context, parser, layoutInflater, settingsList)
                    else -> { throw Exception("Unknown tag type") }
                }
            } else {
                event = parser.next()
            }
        }
    }

    // Inflate and set up a text setting field.
    private fun buildText(context: Context, parser: XmlResourceParser, layoutInflater: LayoutInflater, settingsList: LinearLayout): Int {
        val textSetting: View = layoutInflater.inflate(R.layout.settings_text, settingsList, false)
        textSetting.findViewById<TextView>(R.id.label).text = attrToString(parser, "label", context)
        textSetting.findViewById<TextView>(R.id.description).text = attrToString(parser, "desc", context)
        settingsList.addView(textSetting)

        return parser.next()
    }

    // Inflate and set up a dropdown setting field.
    private fun buildDropdown(context: Context, parser: XmlResourceParser, layoutInflater: LayoutInflater, settingsList: LinearLayout): Int {
        val dropdownSetting: View = layoutInflater.inflate(R.layout.settings_dropdown, settingsList, false)
        dropdownSetting.findViewById<TextView>(R.id.label).text = attrToString(parser, "label", context)
        dropdownSetting.findViewById<TextView>(R.id.description).text = attrToString(parser, "desc", context)
        settingsList.addView(dropdownSetting)

        return parser.next()
    }

    // Supports both string literal and string resource in attributes
    private fun attrToString(parser: XmlResourceParser, attribute: String, context: Context) : String {
        val emptyString: String = context.getString(R.string.empty_string)
        val attributeResource: String = context.getString(parser.getAttributeResourceValue(null, attribute, R.string.empty_string))

        // attributeResource is obtained via context.getString
        // It is either the actual (parsed) string resource, or an empty string
        // (when attribute does not exist, or when attribute is a string literal)
        // If it is an empty string, we instead return the string literal.
        return if (attributeResource == emptyString) {
            parser.getAttributeValue(null, attribute)?: ""
        } else {
            attributeResource
        }
    }
}