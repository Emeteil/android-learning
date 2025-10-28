package com.example.android_project

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile

class AudioPlayer(private val context: Context)
{
    private var mediaPlayer: MediaPlayer? = null
    private var musicList: MutableList<Uri> = mutableListOf()
    private var currentIndex: Int = 0
    private val handler = Handler(Looper.getMainLooper())

    public var onSongChanged: ((uri: Uri) -> Unit)? = null
    public var onProgressChanged: ((position: Int, duration: Int) -> Unit)? = null

    public fun LoadMusicFromFolder(folderUri: Uri)
    {
        var _musicList: MutableList<Uri> = mutableListOf()
        val folder = DocumentFile.fromTreeUri(context, folderUri)
        folder?.listFiles()?.forEach { file ->
            if (file.isFile && (file.name?.endsWith(".mp3") == true || file.name?.endsWith(".wav") == true || file.name?.endsWith(".m4a") == true))
            {
                _musicList.add(file.uri)
            }
        }

        if (_musicList.isEmpty())
        {
            Toast.makeText(context, "В папке нет файлов с музыкой", Toast.LENGTH_SHORT).show()
            return
        }

        musicList.clear()
        musicList = _musicList

        musicList.shuffle()
        currentIndex = 0
        PlaySong(currentIndex)
    }

    public fun PlaySong(index: Int)
    {
        ReleasePlayer()
        if (musicList.isEmpty()) return

        val uri = musicList[index]
        mediaPlayer = MediaPlayer.create(context, uri)
        mediaPlayer?.start()

        onSongChanged?.invoke(uri)
        onProgressChanged?.invoke(0, mediaPlayer!!.duration)

        UpdateProgressBar()
        mediaPlayer?.setOnCompletionListener { NextSong() }
    }

    public fun PauseSong() { mediaPlayer?.pause() }
    public fun SeekTo(position: Int) { mediaPlayer?.seekTo(position) }
    public fun IsPlaying(): Boolean { return mediaPlayer?.isPlaying == true }
    public fun IsMediaPlayerReady(): Boolean { return mediaPlayer != null }

    public fun ResumeSong()
    {
        mediaPlayer?.start()
        UpdateProgressBar()
    }

    public fun NextSong()
    {
        if (musicList.isEmpty())
            return

        currentIndex = (currentIndex + 1) % musicList.size
        PlaySong(currentIndex)
    }

    public fun PreviousSong()
    {
        if (musicList.isEmpty())
            return

        currentIndex = if (currentIndex - 1 < 0) musicList.size - 1 else currentIndex - 1
        PlaySong(currentIndex)
    }

    public fun FormatTime(ms: Int): String
    {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return minutes.toString() + ":" + (if (seconds < 10) "0" else "") + seconds.toString()
    }

    public fun ReleasePlayer()
    {
        mediaPlayer?.release()
        mediaPlayer = null
        handler.removeCallbacksAndMessages(null)
    }

    private fun UpdateProgressBar()
    {
        handler.postDelayed(object : Runnable {
            override fun run()
            {
                if (mediaPlayer != null)
                {
                    val current = mediaPlayer!!.currentPosition
                    val total = mediaPlayer!!.duration
                    onProgressChanged?.invoke(current, total)
                    handler.postDelayed(this, 500)
                }
            }
        }, 500)
    }
}
