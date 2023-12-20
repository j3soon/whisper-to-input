package com.example.whispertoinput.settings

import android.content.Context
import android.widget.Button
import android.widget.Toast
import com.example.whispertoinput.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingsPage {
    private val settingItems: ArrayList<SettingItem>  = ArrayList<SettingItem>()

    fun add(settingItem: SettingItem) {
        settingItems.add(settingItem)
    }

    fun setup(context: Context, btnApply: Button) {
        btnApply.setOnClickListener { apply(context) }

        CoroutineScope(Dispatchers.Main).launch {
            for (settingItem in settingItems) {
                settingItem.setup(context)
            }

            btnApply.isEnabled = false
        }
    }

    private fun apply(context: Context) {
        CoroutineScope(Dispatchers.Main).launch {
            for (settingItem in settingItems) {
                settingItem.apply(context)
            }
        }

        Toast.makeText(context, R.string.successfully_set, Toast.LENGTH_SHORT).show()
    }
}