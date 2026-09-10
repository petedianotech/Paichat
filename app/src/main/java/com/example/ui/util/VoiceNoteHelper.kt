package com.example.ui.util

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException

object VoiceNoteHelper {
    private const val TAG = "VoiceNoteHelper"
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentRecordingFile: File? = null

    fun startRecording(context: Context): File? {
        stopRecording()
        stopPlayback()

        return try {
            val audioDir = File(context.filesDir, "voice_notes").apply {
                if (!exists()) mkdirs()
            }
            val file = File(audioDir, "voice_${System.currentTimeMillis()}.m4a")
            currentRecordingFile = file

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
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            file
        } catch (e: Exception) {
            Log.e(TAG, "Error starting audio recording: ${e.message}")
            cleanupRecording()
            null
        }
    }

    fun stopRecording(): File? {
        val file = currentRecordingFile
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio recording: ${e.message}")
        } finally {
            mediaRecorder = null
            currentRecordingFile = null
        }
        return if (file != null && file.exists() && file.length() > 0) file else null
    }

    private fun cleanupRecording() {
        try {
            mediaRecorder?.release()
        } catch (_: Exception) {}
        mediaRecorder = null
        currentRecordingFile?.delete()
        currentRecordingFile = null
    }

    fun playAudio(filePath: String, onCompletion: () -> Unit, onError: () -> Unit) {
        stopPlayback()
        try {
            val player = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                setOnCompletionListener {
                    stopPlayback()
                    onCompletion()
                }
                setOnErrorListener { _, _, _ ->
                    stopPlayback()
                    onError()
                    true
                }
                start()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio: ${e.message}")
            stopPlayback()
            onError()
        }
    }

    fun stopPlayback() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
        } catch (_: Exception) {}
        mediaPlayer = null
    }

    fun isPlaying(): Boolean {
        return try {
            mediaPlayer?.isPlaying == true
        } catch (_: Exception) {
            false
        }
    }
}
