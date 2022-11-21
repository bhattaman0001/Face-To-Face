package com.amanbhatt.videocallapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.amanbhatt.videocallapplication.databinding.ActivityMainBinding
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoCanvas
import io.agora.rtc.video.VideoEncoderConfiguration

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var TAG = "AMAN"

    private val permissionId = 7

    private val requestPermission = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA,
        Manifest.permission.READ_PHONE_STATE
    )

    private var endCall = false
    private var muted = false
    private var secondUser: SurfaceView? = null
    private var firstUser: SurfaceView? = null
    private var engineRTC: RtcEngine? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        engineRTC = RtcEngine.create(baseContext, getString(R.string.app_id), mRtcEventHandler)

        if (checkSelfPermission(requestPermission[0], permissionId) &&
            checkSelfPermission(
                requestPermission[1], permissionId
            ) && checkSelfPermission(requestPermission[2], permissionId)
        ) {
            initAndJoinChannel()
        }

        binding.buttonCall.setOnClickListener {
            if (endCall) {
                startCall()
                endCall = false
                binding.buttonCall.setImageResource(R.drawable.btn_endcall)
                binding.buttonMute.visibility = VISIBLE
                binding.buttonSwitchCamera.visibility = VISIBLE
            } else {
                endCall()
                endCall = true
                startActivity(Intent(this@MainActivity, FirstClass::class.java))
                Toast.makeText(this@MainActivity, "You left the room", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        binding.buttonSwitchCamera.setOnClickListener {
            engineRTC?.switchCamera()
        }

        binding.buttonMute.setOnClickListener {
            muted = !muted
            engineRTC?.muteLocalAudioStream(muted)
            val res: Int = if (muted) {
                R.drawable.btn_mute
            } else {
                R.drawable.btn_unmute
            }

            binding.buttonMute.setImageResource(res)
        }
    }

    private val mRtcEventHandler = object : IRtcEngineEventHandler() {

        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            runOnUiThread {
                Toast.makeText(
                    applicationContext,
                    "Joined Channel Successfully",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onFirstRemoteVideoDecoded(uid: Int, width: Int, height: Int, elapsed: Int) {
            runOnUiThread {
                setupRemoteVideoView(uid, elapsed)
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread {
                onRemoteUserLeft()
            }
        }
    }

    private fun checkSelfPermission(permission: String, requestCode: Int): Boolean {
        if (ContextCompat.checkSelfPermission(
                this,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(this, requestPermission, requestCode)
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == permissionId) {
            if (
                grantResults[0] != PackageManager.PERMISSION_GRANTED ||
                grantResults[1] != PackageManager.PERMISSION_GRANTED ||
                grantResults[2] != PackageManager.PERMISSION_GRANTED
            ) {

                Toast.makeText(applicationContext, "Permissions needed", Toast.LENGTH_LONG).show()
                finish()
                return
            }

            initAndJoinChannel()
        }
    }

    private fun initAndJoinChannel() {
        initRtcEngine()
        setupVideoConfig()
        setupLocalVideoView()
        joinChannel()
    }

    private fun initRtcEngine() {
        try {
            engineRTC = RtcEngine.create(baseContext, getString(R.string.app_id), mRtcEventHandler)
        } catch (e: Exception) {
            Log.d(TAG, "initRtcEngine: $e")
        }
    }

    private fun setupLocalVideoView() {

        firstUser = RtcEngine.CreateRendererView(baseContext)
        firstUser!!.setZOrderMediaOverlay(true)
        binding.localVideoView.addView(firstUser)

        engineRTC?.setupLocalVideo(VideoCanvas(firstUser, VideoCanvas.RENDER_MODE_HIDDEN, 0))
    }

    private fun setupRemoteVideoView(uid: Int, elapsed: Int) {
        if (binding.remoteVideoView.childCount > 1) {
            return
        }
        secondUser = RtcEngine.CreateRendererView(baseContext)
        binding.remoteVideoView.addView(secondUser)

        engineRTC?.setupRemoteVideo(VideoCanvas(secondUser, VideoCanvas.RENDER_MODE_FILL, uid))

        // elapsed is in ms so 15 sec is 15000 ms
        if (elapsed >= 15000) {
            Toast.makeText(this@MainActivity, "Unable to connect", Toast.LENGTH_SHORT)
                .show()
            startActivity(Intent(this@MainActivity, FirstClass::class.java))
            finish()
        }

    }

    private fun setupVideoConfig() {
        engineRTC?.enableVideo()

        engineRTC?.setVideoEncoderConfiguration(
            VideoEncoderConfiguration(
                VideoEncoderConfiguration.VD_640x360,
                VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                VideoEncoderConfiguration.STANDARD_BITRATE,
                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT
            )
        )
    }

    private fun joinChannel() {
        val token = intent.extras?.getString("token")
        engineRTC?.joinChannel(token, "ChannelOne", "Extra Optional Data", 0)
    }

    private fun startCall() {
        setupLocalVideoView()
        joinChannel()
    }

    private fun endCall() {
        removeLocalVideo()
        removeRemoteVideo()
        leaveChannel()
    }

    private fun removeLocalVideo() {
        if (firstUser != null) {
            binding.localVideoView.removeView(firstUser)
        }
        firstUser = null
    }

    private fun removeRemoteVideo() {
        if (secondUser != null) {
            binding.remoteVideoView.removeView(secondUser)
        }
        secondUser = null
    }

    private fun leaveChannel() {
        engineRTC?.leaveChannel()
    }

    private fun onRemoteUserLeft() {
        removeRemoteVideo()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!endCall) {
            leaveChannel()
        }
        engineRTC?.stopPreview()

        Thread {
            RtcEngine.destroy()
            engineRTC = null
        }.start()

    }

}