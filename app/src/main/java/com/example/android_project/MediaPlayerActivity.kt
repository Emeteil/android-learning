package com.example.android_project

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.media.MediaMetadataRetriever
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MediaPlayerActivity : AppCompatActivity()
{
    private lateinit var songTitle: TextView
    private lateinit var artistName: TextView
    private lateinit var albumArt: ImageView
    private lateinit var progressBar: SeekBar
    private lateinit var currentTime: TextView
    private lateinit var totalTime: TextView
    private lateinit var buttonPlay: ImageButton
    private lateinit var buttonNext: ImageButton
    private lateinit var buttonPrevious: ImageButton
    private lateinit var buttonOpenFolder: ImageButton
    private lateinit var buttonBack: ImageButton

    private lateinit var audioPlayer: AudioPlayer

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_mediaplayer)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        songTitle = findViewById(R.id.songTitle)
        artistName = findViewById(R.id.artistName)
        albumArt = findViewById(R.id.albumArt)
        progressBar = findViewById(R.id.progressBar)
        currentTime = findViewById(R.id.currentTime)
        totalTime = findViewById(R.id.totalTime)
        buttonPlay = findViewById(R.id.buttonPlay)
        buttonNext = findViewById(R.id.buttonNext)
        buttonPrevious = findViewById(R.id.buttonPrevious)
        buttonOpenFolder = findViewById(R.id.buttonOpenFolder)
        buttonBack = findViewById(R.id.buttonBack)

        audioPlayer = AudioPlayer(this)

    }

    override fun onResume()
    {
        super.onResume()

        audioPlayer.onSongChanged = { uri ->
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(applicationContext, uri)

            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)

            val albumArtBitmap = retriever.embeddedPicture?.let {
                android.graphics.BitmapFactory.decodeByteArray(it, 0, it.size)
            }

            songTitle.text = title ?: GetFileName(uri).substringAfter("-")
            artistName.text = artist ?: GetFileName(uri).substringBefore("-")

            if (albumArtBitmap != null)
                albumArt.setImageBitmap(albumArtBitmap)
            else
                albumArt.setImageResource(R.drawable.album_placeholder)

            buttonPlay.setImageResource(R.drawable.ic_pause)
        }

        audioPlayer.onProgressChanged = { position, duration ->
            progressBar.max = duration
            progressBar.progress = position
            currentTime.text = audioPlayer.FormatTime(position)
            totalTime.text = audioPlayer.FormatTime(duration)
        }

        buttonPlay.setOnClickListener {
            if (audioPlayer.IsMediaPlayerReady() && !audioPlayer.IsPlaying())
            {
                audioPlayer.ResumeSong()
                buttonPlay.setImageResource(R.drawable.ic_pause)
            }
            else
            {
                audioPlayer.PauseSong()
                buttonPlay.setImageResource(R.drawable.ic_play)
            }
        }

        buttonNext.setOnClickListener { audioPlayer.NextSong() }
        buttonPrevious.setOnClickListener { audioPlayer.PreviousSong() }
        buttonOpenFolder.setOnClickListener { OpenFolderPicker() }
        buttonBack.setOnClickListener { finish() }

        progressBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean)
            {
                if (fromUser)
                    audioPlayer.SeekTo(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun GetFileName(uri: Uri): String
    {
        val path = DocumentsContract.getDocumentId(uri)
        return path.substringAfterLast("/").substringBefore(".")
    }

    override fun onPause()
    {
        super.onPause()
        if (audioPlayer.IsPlaying())
        {
            audioPlayer.PauseSong()
            buttonPlay.setImageResource(R.drawable.ic_play)

            if (!isFinishing && !isChangingConfigurations)
                Toast.makeText(this, "Для прослушивания в фоне - купите подписку за 15$/месяц", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy()
    {
        super.onDestroy()
        audioPlayer.ReleasePlayer()
    }

    private val PICK_FOLDER_REQUEST_CODE = 1001
    private fun OpenFolderPicker()
    {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        startActivityForResult(intent, PICK_FOLDER_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FOLDER_REQUEST_CODE && resultCode == Activity.RESULT_OK)
        {
            val uri = data?.data ?: return
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            audioPlayer.LoadMusicFromFolder(uri)
        }
    }
}
