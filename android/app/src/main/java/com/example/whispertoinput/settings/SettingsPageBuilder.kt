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
                val settingEntry: View = when (tagType) {
                    "text" -> layoutInflater.inflate(R.layout.settings_text, settingsList, false)
                    "dropdown" -> layoutInflater.inflate(R.layout.settings_dropdown, settingsList, false)
                    else -> { throw Exception("Unknown tag type") }
                }
                settingEntry.findViewById<TextView>(R.id.label).text = "Configuration Label"
                settingEntry.findViewById<TextView>(R.id.description).text = "Descriptive description."
                settingsList.addView(settingEntry)
                event = parser.next()
            } else {
                event = parser.next()
            }
        }
    }
}