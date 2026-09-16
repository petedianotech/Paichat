package com.example.ui.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

object VoiceNoteHelper {
    private const val TAG = "VoiceNoteHelper"
    private var mediaRecorder: MediaRecorder? = null
    private var currentFile: File? = null

    fun startRecording(context: Context): File? {
        try {
            stopRecording() // ensure any previous recording is stopped

            val outputDir = context.cacheDir
            val file = File.createTempFile("voice_", ".m4a", outputDir)
            currentFile = file

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            return file
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            currentFile?.delete()
            currentFile = null
            mediaRecorder = null
            return null
        }
    }

    fun stopRecording(): File? {
        val recorder = mediaRecorder ?: return null
        try {
            recorder.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop recording", e)
            currentFile?.delete()
            currentFile = null
        } finally {
            recorder.release()
            mediaRecorder = null
        }
        val file = currentFile
        currentFile = null
        return file
    }
}
