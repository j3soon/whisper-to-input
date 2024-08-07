package com.example.whispertoinput.settings

import android.content.Context
import android.util.Log
import android.widget.Button
import android.widget.Toast
import com.example.whispertoinput.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

// The class managing all SettingItems.
// Responsible for setting them all up, and applying all settings.
class SettingsPage(private val context: Context, private val btnApply: Button) {
    private val settingItems: ArrayList<SettingItem>  = ArrayList<SettingItem>()

    fun add(settingItem: SettingItem) {
        settingItems.add(settingItem)
    }

    fun setup() {
        btnApply.setOnClickListener { apply() }

        CoroutineScope(Dispatchers.Main).launch {
            btnApply.isEnabled = false
            // Set up each setting item and wait for them to complete
            settingItems.map { settingItem -> settingItem.setup(context) }.joinAll()
            btnApply.isEnabled = false
        }
    }

    private fun apply() {
        CoroutineScope(Dispatchers.Main).launch {
            btnApply.isEnabled = false
            for (settingItem in settingItems) {
                settingItem.apply(context)
                settingItem.resetIsDirty()
            }

            btnApply.isEnabled = false
        }

        Toast.makeText(context, R.string.successfully_set, Toast.LENGTH_SHORT).show()
    }
}