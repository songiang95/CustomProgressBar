package com.example.customprogressbar

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var button: Button
    private lateinit var editText: EditText
    private lateinit var progressView: DownloadProgressView
    private lateinit var indeterminate: Button
    private lateinit var seekBar: SeekBar


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button = findViewById(R.id.button)
        editText = findViewById(R.id.editTextNumber)
        progressView = findViewById(R.id.progress)
        indeterminate = findViewById(R.id.indeterminate)
        seekBar = findViewById(R.id.seekbar)

        button.setOnClickListener {
            progressView.setProgress(editText.text.toString().toInt())
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                progressView.setProgress(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })

        var isIndeterminate = false

        indeterminate.setOnClickListener {
            isIndeterminate = !isIndeterminate
            progressView.setIndeterminate(isIndeterminate)
        }

/*        CoroutineScope(Dispatchers.Main).launch {
            (0..100).forEach {
                delay(100)
                progressView.setProgress(it)
            }
        }*/

    }
}