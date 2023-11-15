package com.example.whispertoinput

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.content.ContextCompat

private const val MICROPHONE_PERMISSION_REQUEST_CODE = 200

class MainActivity : AppCompatActivity() {
    private val permissions : Array<String> = arrayOf(Manifest.permission.RECORD_AUDIO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissions()
        setContentView(R.layout.activity_main)
    }

    // Checks whether permissions are granted. If not, automatically make a request.
    private fun checkPermissions()
    {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED)
        {
            // Shows a popup for permission request.
            // If the permission has been previously (hard-)denied, the popup will not show.
            // onRequestPermissionsResult will be called in either case.
            ActivityCompat.requestPermissions(this, permissions, MICROPHONE_PERMISSION_REQUEST_CODE)
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