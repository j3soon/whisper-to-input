package com.example.whispertoinput

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast

private const val MICROPHONE_PERMISSION_REQUEST_CODE = 200

class MainActivity : AppCompatActivity() {
    private val permissions : Array<String> = arrayOf(Manifest.permission.RECORD_AUDIO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityCompat.requestPermissions(this, permissions, MICROPHONE_PERMISSION_REQUEST_CODE)
        setContentView(R.layout.activity_main)
    }

    // Handles the results of permission requests.
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Only handles requests marked with the unique code.
        if (requestCode != MICROPHONE_PERMISSION_REQUEST_CODE)
        {
            return
        }

        // All permissions should be granted.
        for (result in grantResults)
        {
            if (result != PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(this, getString(R.string.mic_permission_required), Toast.LENGTH_SHORT).show()
                return
            }
        }
    }

}