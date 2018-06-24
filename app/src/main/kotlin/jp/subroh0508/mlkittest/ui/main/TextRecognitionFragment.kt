package jp.subroh0508.mlkittest.ui.main

import android.Manifest
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.SparseIntArray
import android.view.*
import androidx.core.content.systemService
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.cloud.FirebaseVisionCloudDetectorOptions
import com.google.firebase.ml.vision.cloud.text.FirebaseVisionCloudText
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionText
import com.tbruyelle.rxpermissions2.RxPermissions
import jp.subroh0508.mlkittest.R
import jp.subroh0508.mlkittest.ui.CameraDelegate
import kotlinx.android.synthetic.main.text_recognition_fragment.*

class TextRecognitionFragment : Fragment() {

    companion object {
        fun newInstance() = TextRecognitionFragment()
    }

    private val cameraDelegate: CameraDelegate by lazy { context?.let { CameraDelegate(it.systemService<CameraManager>()) } ?: throw IllegalStateException() }

    private val rxPermissions: RxPermissions by lazy { activity?.let(::RxPermissions) ?: throw IllegalStateException() }

    private val ORIENTATIONS = SparseIntArray().also {
        it.append(Surface.ROTATION_0, 0)
        it.append(Surface.ROTATION_90, 90)
        it.append(Surface.ROTATION_180, 180)
        it.append(Surface.ROTATION_270, 270)
    }

    private var picture: Bitmap? = null
    private val firebaseVision: FirebaseVision by lazy { FirebaseVision.getInstance() }
    private val cloudOptions: FirebaseVisionCloudDetectorOptions by lazy {
        FirebaseVisionCloudDetectorOptions.Builder()
                .setModelType(FirebaseVisionCloudDetectorOptions.LATEST_MODEL)
                .setMaxResults(10)
                .build()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.text_recognition_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        cameraTexture?.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
                if (rxPermissions.isGranted(Manifest.permission.CAMERA) && rxPermissions.isGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    cameraDelegate.openCamera(cameraTexture)
                }

                rxPermissions.requestEachCombined(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        .subscribe {
                            if (it.granted) {
                                cameraDelegate.openCamera(cameraTexture)
                            }
                        }
            }
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) = Unit
            override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) = Unit
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?) = true
        }

        capture?.setOnClickListener {
            val context = context ?: return@setOnClickListener

            if (cameraDelegate.isPreviewed) {
                picture = null
                result?.text = "RESULT: NONE"
                time?.text = "TIME: NONE"
                onDevice?.isClickable = false
                cameraDelegate.restartPreview()
                return@setOnClickListener
            }

            picture = cameraDelegate.takePicture(context.filesDir,  "${System.currentTimeMillis()}.jpg")

            if (picture != null) {
                onDevice?.isClickable = true
            }
        }

        onDevice?.setOnClickListener {
            startOnDevice()
        }

        onCloud?.setOnClickListener {
            startOnCloud()
        }
    }

    private fun startOnDevice() {
        val picture = this.picture ?: return
        val firebaseVisionImage = FirebaseVisionImage.fromBitmap(picture)

        val startTime = System.currentTimeMillis()
        progress?.visibility = View.VISIBLE
        time?.visibility = View.GONE
        result?.visibility = View.GONE
        firebaseVision.visionTextDetector
                .detectInImage(firebaseVisionImage)
                .addOnSuccessListener {
                    loadFromVisionText(it)
                }
                .addOnFailureListener { e ->
                    result?.text = e.message
                    e.printStackTrace()
                }
                .addOnCompleteListener {
                    progress?.visibility = View.GONE
                    time?.visibility = View.VISIBLE
                    result?.visibility = View.VISIBLE

                    time?.text = "TIME: ${(System.currentTimeMillis() - startTime) / 1000.0}sec"
                }
    }

    private fun loadFromVisionText(texts: FirebaseVisionText) {
        val results: MutableList<String> = mutableListOf()

        results.add("[recognized] ${texts.blocks.joinToString("\n") { it.text }}\n")

        texts.blocks.forEach { block ->
            val boundingBox = block.boundingBox
            val cornerPoints = block.cornerPoints
            val text = block.text

            results.add(
                    "[boundingBox] top=${boundingBox?.top} bottom=${boundingBox?.bottom} left=${boundingBox?.left} right=${boundingBox?.right}\n" +
                            "[cornerPoints] ${cornerPoints?.joinToString(",") { "(${it.x}, ${it.y})" }}\n" +
                            "[text] $text\n"
            )
        }

        result?.text = results.joinToString("\n")
    }

    private fun startOnCloud() {
        val picture = this.picture ?: return
        val firebaseVisionImage = FirebaseVisionImage.fromBitmap(picture)

        val startTime = System.currentTimeMillis()
        progress?.visibility = View.VISIBLE
        time?.visibility = View.GONE
        result?.visibility = View.GONE
        firebaseVision.getVisionCloudTextDetector(cloudOptions)
                .detectInImage(firebaseVisionImage)
                .addOnSuccessListener {
                    loadFromVisionCloudText(it)
                }
                .addOnFailureListener { e ->
                    result?.text = e.message
                    e.printStackTrace()
                }
                .addOnCompleteListener {
                    progress?.visibility = View.GONE
                    time?.visibility = View.VISIBLE
                    result?.visibility = View.VISIBLE

                    time?.text = "TIME: ${(System.currentTimeMillis() - startTime) / 1000.0}sec"
                }
    }

    private fun loadFromVisionCloudText(texts: FirebaseVisionCloudText) {
        val results: MutableList<String> = mutableListOf()

        results.add("[recognized] ${texts.text}")
        results.add("=============")

        texts.pages.forEach { page ->
            results.add("--- page summary ---")

            val languages = page.textProperty?.detectedLanguages
            val height = page.height
            val width = page.width
            val confidence = page.confidence

            results.add(
                    "[languages] ${languages?.joinToString(",") { it.languageCode ?: "XXX" }}\n" +
                            "[size] height=$height width=$width\n" +
                            "[confidence] $confidence"
            )

            page.blocks.forEach { block ->
                results.add("- block summary -")

                val blockLanguages = block.textProperty?.detectedLanguages
                val paragraphs = block.paragraphs
                val boundingBox = block.boundingBox

                val symbols: MutableList<String> = mutableListOf()
                paragraphs?.forEachIndexed { i, p ->
                    p.words.forEachIndexed { j, w ->
                        w.symbols.forEach { symbols.add("<$i, $j> ${it.text}") }
                    }
                }

                results.add(
                        "[languages] ${blockLanguages?.joinToString(",") { it.languageCode ?: "XXX" }}\n" +
                            "[words] ${symbols.joinToString("\n")}\n" +
                            "[boundingBox] top=${boundingBox?.top} bottom=${boundingBox?.bottom} left=${boundingBox?.left} right=${boundingBox?.right}\n"
                )
            }
            results.add("- end block summary -")
        }
        results.add("--- end page summary ---")

        results.add("=============")

        result?.text = results.joinToString("\n")
    }
}
