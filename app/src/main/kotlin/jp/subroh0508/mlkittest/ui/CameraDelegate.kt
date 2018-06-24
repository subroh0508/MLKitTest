package jp.subroh0508.mlkittest.ui

import android.graphics.Bitmap
import android.hardware.camera2.*
import android.os.Handler
import android.view.Surface
import android.view.TextureView
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

class CameraDelegate(private val manager: CameraManager) {
    var isPreviewed = false
    val cameraId: String?
        get() = if (::_cameraId.isInitialized) null else _cameraId

    private var textureView: TextureView? = null
    private lateinit var _cameraId: String
    private val handler: Handler by lazy { Handler() }

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var previewRequestBuilder: CaptureRequest.Builder? = null
    private var previewRequest: CaptureRequest? = null

    fun openCamera(textureView: TextureView) {
        this.textureView = textureView

        try {
            _cameraId = manager.cameraIdList[0]
        } catch (e: CameraAccessException) {

        }

        try {
            manager.openCamera(_cameraId, stateCallback, handler)
        } catch (e: CameraAccessException) {

        } catch (e: SecurityException) {

        }
    }

    fun takePicture(dir: File, filename: String): Bitmap? {
        var bitmap: Bitmap? = null

        try {
            val textureView = this.textureView ?: return null

            captureSession?.stopRepeating()

            if (textureView.isAvailable) {
                val file = File(dir,  filename)
                FileOutputStream(file).also {
                    bitmap = textureView.bitmap
                    bitmap?.compress(Bitmap.CompressFormat.JPEG, 50, it)
                    it.close()
                }
            }

            isPreviewed = true
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return bitmap
    }

    fun restartPreview() {
        createCameraPreviewSession()
    }

    private fun createCameraPreviewSession() {
        val texture = textureView?.surfaceTexture ?: return
        texture.setDefaultBufferSize(2000, 2000)

        val surface = Surface(texture)

        try {
            previewRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)?.also {
                it.addTarget(surface)
                previewRequest = it.build()
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

        try {
            cameraDevice?.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession?) {
                    session ?: return

                    captureSession = session

                    try {
                        previewRequest?.let {
                            session.setRepeatingRequest(it, null, null)
                            isPreviewed = false
                        }
                    } catch (e: CameraAccessException) {
                        e.printStackTrace()
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession?) = Unit
            }, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private val stateCallback: CameraDevice.StateCallback by lazy {
        object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice?) {
                cameraDevice = camera
                createCameraPreviewSession()
            }

            override fun onError(camera: CameraDevice?, error: Int) {
                camera?.close()
                cameraDevice = null
            }

            override fun onDisconnected(camera: CameraDevice?) {
                camera?.close()
                cameraDevice = null
            }
        }
    }
}