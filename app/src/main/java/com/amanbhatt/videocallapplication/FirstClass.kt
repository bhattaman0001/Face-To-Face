package com.amanbhatt.videocallapplication

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.amanbhatt.videocallapplication.databinding.ActivityFirstClassBinding
import com.google.android.ads.nativetemplates.NativeTemplateStyle
import com.google.android.gms.ads.*
import com.google.android.gms.ads.nativead.NativeAdOptions


class FirstClass : AppCompatActivity() {

    private lateinit var binding: ActivityFirstClassBinding

    private lateinit var adLoader: AdLoader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFirstClassBinding.inflate(layoutInflater)
        setContentView(binding.root)

        MobileAds.initialize(this) {}

        adLoader = AdLoader.Builder(this, "ca-app-pub-3940256099942544/2247696110")
            .forNativeAd { NativeAd ->
                if (isDestroyed) {
                    NativeAd.destroy()
                    return@forNativeAd
                }
                val styles = NativeTemplateStyle.Builder().withMainBackgroundColor(
                    ColorDrawable(
                        Color.WHITE
                    )
                ).build()
                binding.myTemplate.setStyles(styles)
                binding.myTemplate.setNativeAd(NativeAd)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Toast.makeText(this@FirstClass, "Failed to load the ad!!", Toast.LENGTH_SHORT)
                        .show()
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .build()
            )
            .build()

        adLoader.loadAd(AdRequest.Builder().build())

        binding.join.setOnClickListener {
            val intent = Intent(this@FirstClass, MainActivity::class.java)
            val token = binding.tokenId.text.toString()
            if (binding.tokenId.text.isEmpty()) {
                Toast.makeText(
                    this@FirstClass, "first enter the token", Toast.LENGTH_SHORT
                ).show()
            } else {
                intent.putExtra("token", token)
                startActivity(intent)
                finish()
            }
        }
    }
}