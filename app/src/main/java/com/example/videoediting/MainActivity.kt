package com.example.videoediting

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import com.example.videoediting.databinding.ActivityMainBinding
import ly.img.android.pesdk.VideoEditorSettingsList
import ly.img.android.pesdk.backend.decoder.AudioSource
import ly.img.android.pesdk.backend.decoder.ImageSource
import ly.img.android.pesdk.backend.model.EditorSDKResult
import ly.img.android.pesdk.backend.model.config.AudioTrackAsset
import ly.img.android.pesdk.backend.model.state.LoadSettings
import ly.img.android.pesdk.backend.model.state.VideoEditorSaveSettings
import ly.img.android.pesdk.ui.activity.VideoEditorBuilder
import ly.img.android.pesdk.ui.model.state.UiConfigAudio
import ly.img.android.pesdk.ui.panels.AudioOverlayOptionsToolPanel
import ly.img.android.pesdk.ui.panels.item.*

class MainActivity : AppCompatActivity() {
    companion object {
        private const val GALLERY_REQUEST_CODE = 0x69
        private const val EDITOR_REQUEST_CODE = 501
    }
    lateinit var binding:ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.video.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            try {
                startActivityForResult(intent, GALLERY_REQUEST_CODE)
            } catch (ex: ActivityNotFoundException) {
                showMessage("No Gallery app installed")
            }
        }
    }

    private fun showMessage(s: String) {
        Toast.makeText(this,s, Toast.LENGTH_SHORT).show()
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        intent ?: return
        when (requestCode) {
            EDITOR_REQUEST_CODE -> {
                val result = EditorSDKResult(intent)
                when (result.resultStatus) {
                    EditorSDKResult.Status.CANCELED -> showMessage("Editor cancelled")
                    EditorSDKResult.Status.EXPORT_DONE -> showMessage("Result saved at ${result.resultUri}")
                    else -> {
                    }
                }
            }
            GALLERY_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    intent.data?.let { showEditor(it) } ?: showMessage("Invalid Uri")
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    showMessage("User cancelled selection")
                }
            }
        }
    }
    private fun showEditor(uri: Uri) {
        // In this example, we do not need access to the Uri(s) after the editor is closed
        // so we pass false in the constructor
        val settingsList = VideoEditorSettingsList(false)
            // Set the source as the Uri of the video to be loaded
            .configure<LoadSettings> {
                it.source = uri
            }
            .configure<VideoEditorSaveSettings> {
                it.setOutputToGallery()
            }
        settingsList.config.addAsset(
            AudioTrackAsset("id_elsewhere", AudioSource.create(R.raw.omahi)),

            )
        settingsList.configure<UiConfigAudio> {
            // Set the audio track list using the ids defined in the AudioTrackAssets above
            it.setAudioTrackLists(
                AudioTrackCategoryItem(
                    "audio_cat_elsewhere",
                    "Android",
                    AudioTrackItem("id_elsewhere"),
                    AudioTrackItem("id_trapped")
                )
            )
            // Start the video editor using VideoEditorBuilder
            // The result will be obtained in onActivityResult() corresponding to EDITOR_REQUEST_CODE
            VideoEditorBuilder(this)
                .setSettingsList(settingsList)
                .startActivityForResult(this, EDITOR_REQUEST_CODE)
            // Release the SettingsList once done
            settingsList.release()
        }
        settingsList.configure<UiConfigAudio> {
            // Set the audio track list using the ids defined in the AudioTrackAssets above
            it.setAudioTrackLists(
                AudioTrackCategoryItem(
                    id = "audio_custom_category",
                    "Android",
                    AudioTrackItem("id_elsewhere"),
                    AudioTrackItem("id_trapped"),
                )
            )

            // By default the editor allows all available quick actions that can be used in this tool
            // For this example, only the play/pause button is enabled
            // Here, we use the fillListSpacedByGroups() method. Alternatively, we could have added the SpaceItems manually.
            SpaceItem.fillListSpacedByGroups(
                list = it.quickOptionList, groups = listOf(
                    listOf(),
                    listOf(
                        ToggleOption(
                            AudioOverlayOptionsToolPanel.OPTION_PLAY_PAUSE,
                            "Play/Pause",
                            ImageSource.create(
                                ly.img.android.pesdk.ui.R.drawable.imgly_icon_play_pause_option
                            )
                        )
                    ),
                    listOf()
                )
            )
        }

    }
}