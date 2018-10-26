package com.ap.circularimageview

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        circularImageView.postDelayed({
            circularImageView.enableOutline(true)
            circularImageView.setOutlineColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            circularImageView.setOutlineThickness(8)
        }, 2000)
    }
}
