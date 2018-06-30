package jp.subroh0508.mlkittest.ui.main

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import androidx.core.content.systemService
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark
import com.tbruyelle.rxpermissions2.RxPermissions
import jp.subroh0508.mlkittest.R
import jp.subroh0508.mlkittest.ui.CameraDelegate
import jp.subroh0508.mlkittest.ui.ImageLoadDelegate
import kotlinx.android.synthetic.main.face_detection_fragment.*

class FaceDetectionFragment : Fragment() {
    companion object {
        fun newInstance() = FaceDetectionFragment()

        private val sampleImageNames: List<String> = listOf("face_detection1.jpg", "face_detection2.jpg", "face_detection3.jpg")
    }

    private val cameraDelegate: CameraDelegate by lazy { context?.let { CameraDelegate(it.systemService<CameraManager>()) } ?: throw IllegalStateException() }
    private val imageLoadDelegate: ImageLoadDelegate by lazy(::ImageLoadDelegate)

    private val rxPermissions: RxPermissions by lazy { activity?.let(::RxPermissions) ?: throw IllegalStateException() }

    private var picture: Bitmap? = null
    private val firebaseVision: FirebaseVision by lazy { FirebaseVision.getInstance() }
    private var options: FirebaseVisionFaceDetectorOptions = FirebaseVisionFaceDetectorOptions.Builder()
            .setModeType(FirebaseVisionFaceDetectorOptions.ACCURATE_MODE)
            .setLandmarkType(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
            .setClassificationType(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
            .setMinFaceSize(0.1f)
            .setTrackingEnabled(false)
            .build()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.face_detection_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        isCamera.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                cameraTexture?.visibility = View.VISIBLE
                capture?.visibility = View.VISIBLE
            } else {
                cameraTexture?.visibility = View.GONE
                capture?.visibility = View.GONE
            }
        }
        isAsset.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                sample?.visibility = View.VISIBLE
                samples?.visibility = View.VISIBLE
            } else {
                sample?.visibility = View.GONE
                samples?.visibility = View.GONE
            }
        }

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

        imageLoadDelegate.loadImagesFromAsset(samples, sampleImageNames) { filename ->
            val bitmap = BitmapFactory.decodeStream(
                    context?.assets?.open(filename),
                    null,
                    BitmapFactory.Options().also { it.inSampleSize = 4 }
            )

            sample?.setImageBitmap(bitmap)

            val rawBitmap = BitmapFactory.decodeStream(context?.assets?.open(filename))
            picture = rawBitmap
        }
    }

    private fun startOnDevice() {
        val picture = this.picture ?: return
        val firebaseVisionImage = FirebaseVisionImage.fromBitmap(picture)

        val startTime = System.currentTimeMillis()
        progress?.visibility = View.VISIBLE
        time?.visibility = View.GONE
        result?.visibility = View.GONE
        firebaseVision.getVisionFaceDetector(options)
                .detectInImage(firebaseVisionImage)
                .addOnSuccessListener {
                    val faceInfo = it.map(this::loadFromVisionFace).flatten()
                    result.text = faceInfo.joinToString("\n")
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

    private fun loadFromVisionFace(face: FirebaseVisionFace): List<String> {
        val results: MutableList<String> = mutableListOf()

        results.add("=== Face Summary ===\n")

        val boundingBox = face.boundingBox
        val rotY = face.headEulerAngleY
        val rotZ = face.headEulerAngleZ

        val facePosition = "[Face Position] left=${boundingBox.left} right=${boundingBox.right} top=${boundingBox.top} bottom=${boundingBox.bottom}"
        val headAngles = "[Face Angle] Y=$rotY Z=$rotZ"

        results.add(facePosition)
        results.add(headAngles)

        results.addAll(getFaceLandmarks(face))
        results.addAll(getFaceClassification(face))
        results.add(getTrackingId(face))

        results.add("=== End Face Summary ===")

        return results
    }

    private fun getFaceLandmarks(face: FirebaseVisionFace): List<String> {
        val landmarksPosition: MutableList<String> = mutableListOf()

        val leftEar = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EAR)
        val rightEar = face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EAR)
        val leftMouth = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_MOUTH)
        val rightMouth = face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_MOUTH)
        val bottomMouth = face.getLandmark(FirebaseVisionFaceLandmark.BOTTOM_MOUTH)
        val leftEye = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EYE)
        val rightEye = face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EYE)
        val leftCheek = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_CHEEK)
        val rightCheek = face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_CHEEK)
        val noseBase = face.getLandmark(FirebaseVisionFaceLandmark.NOSE_BASE)

        listOf(
                FaceLandmark.LEFT_EAR to leftEar, FaceLandmark.RIGHT_EAR to rightEar,
                FaceLandmark.LEFT_MOUTH to leftMouth, FaceLandmark.RIGHT_MOUTH to rightMouth, FaceLandmark.BOTTOM_MOUTH to bottomMouth,
                FaceLandmark.LEFT_EYE to leftEye, FaceLandmark.RIGHT_EYE to rightEye,
                FaceLandmark.LEFT_CHEEK to leftCheek, FaceLandmark.RIGHT_CHEEK to rightCheek,
                FaceLandmark.NOSE_BASE to noseBase
        ).forEach { (label, landmark) ->
            val point = landmark?.position?.let { "(${it.x}, ${it.y}, ${it.z})" } ?: "null"

            landmarksPosition.add("$label: $point")
        }

        return landmarksPosition
    }

    private fun getFaceClassification(face: FirebaseVisionFace): List<String> {
        val probabilities: MutableList<String> = mutableListOf()

        val smilingProbability = face.smilingProbability
        val leftEyeOpenProbability = face.leftEyeOpenProbability
        val rightEyeOpenProbability = face.rightEyeOpenProbability

        listOf(
                FaceClassification.SMILE to smilingProbability,
                FaceClassification.LEFT_EYE_OPEN to leftEyeOpenProbability,
                FaceClassification.RIGHT_EYE_OPEN to rightEyeOpenProbability
        ).forEach { (label, probability) ->

            probabilities.add(
                    "$label: ${
                        if (probability == FirebaseVisionFace.UNCOMPUTED_PROBABILITY)
                            "Uncomputed Probability"
                        else
                            probability.toString()
                    }"
            )
        }

        return probabilities
    }

    private fun getTrackingId(face: FirebaseVisionFace): String {
        return "TRACKING_ID: " +
                if (face.trackingId == FirebaseVisionFace.INVALID_ID)
                    "Invalid ID"
                else
                    face.trackingId.toString()
    }

    private enum class FaceLandmark {
        LEFT_EAR, RIGHT_EAR,
        LEFT_MOUTH, RIGHT_MOUTH, BOTTOM_MOUTH,
        LEFT_EYE, RIGHT_EYE,
        LEFT_CHEEK, RIGHT_CHEEK,
        NOSE_BASE
    }

    private enum class FaceClassification {
        SMILE, LEFT_EYE_OPEN, RIGHT_EYE_OPEN
    }
}
