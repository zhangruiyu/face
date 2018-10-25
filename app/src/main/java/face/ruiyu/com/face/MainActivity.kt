package face.ruiyu.com.face

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import io.fotoapparat.Fotoapparat
import io.fotoapparat.FotoapparatSwitcher
import io.fotoapparat.facedetector.Rectangle
import io.fotoapparat.facedetector.processor.FaceDetectorProcessor
import io.fotoapparat.log.Loggers.*
import io.fotoapparat.parameter.LensPosition
import io.fotoapparat.parameter.selector.LensPositionSelectors.lensPosition
import kotlinx.android.synthetic.main.activity_main.*

import android.graphics.Bitmap
import android.graphics.Matrix


class MainActivity : AppCompatActivity() {

    private val permissionsDelegate = PermissionsDelegate(this)
    private var hasCameraPermission: Boolean = false

    private var fotoapparatSwitcher: FotoapparatSwitcher? = null
    private lateinit var frontFotoapparat: Fotoapparat
    private lateinit var backFotoapparat: Fotoapparat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        hasCameraPermission = permissionsDelegate.hasCameraPermission()

        if (hasCameraPermission) {
            camera_view.visibility = View.VISIBLE
        } else {
            permissionsDelegate.requestCameraPermission()
        }

        frontFotoapparat = createFotoapparat(LensPosition.FRONT)
        backFotoapparat = createFotoapparat(LensPosition.BACK)
        fotoapparatSwitcher = FotoapparatSwitcher.withDefault(backFotoapparat)

        val switchCameraButton = findViewById<View>(R.id.switchCamera)
        switchCameraButton.visibility = if (canSwitchCameras())
            View.VISIBLE
        else
            View.GONE
        switchCameraButton.setOnClickListener { switchCamera() }
        savePic.setOnClickListener { savePic() }
    }

    private fun canSwitchCameras(): Boolean {
        return frontFotoapparat.isAvailable == backFotoapparat.isAvailable
    }

    private var facesList: MutableList<Rectangle> = mutableListOf()

    private fun createFotoapparat(position: LensPosition): Fotoapparat {
        return Fotoapparat
                .with(this)
                .into(camera_view)
                .lensPosition(lensPosition(position))
                .frameProcessor(
                        FaceDetectorProcessor.with(this)
                                .listener { faces ->
                                    Log.d("&&&", "Detected faces: " + faces.size)
                                    faces.forEach {
                                        Log.d("信息:", it.toString())
                                    }
                                    rectanglesView!!.setRectangles(faces)
                                    facesList.clear()
                                    facesList.addAll(faces)
                                }
                                .build()
                )
                .logger(loggers(
                        logcat(),
                        fileLogger(this)
                ))
                .build()
    }

    private fun switchCamera() {
        if (fotoapparatSwitcher!!.currentFotoapparat === frontFotoapparat) {
            fotoapparatSwitcher!!.switchTo(backFotoapparat)
        } else {
            fotoapparatSwitcher!!.switchTo(frontFotoapparat)
        }
    }

    private fun savePic() {
        if (facesList.size == 2) {
            val rectangle = facesList[1]
            val startTime = System.currentTimeMillis()
            if (fotoapparatSwitcher!!.currentFotoapparat === frontFotoapparat) {
                frontFotoapparat
            } else {
                backFotoapparat
            }.takePicture().toBitmap().whenAvailable {
                //图片旋转了
                //                Log.e("MainActivity", "bitmap :${it.rotationDegrees}")
                val bitmap = rotateBitmap(it.bitmap, 360 - it.rotationDegrees)
                val endTime = System.currentTimeMillis()
                Log.e("MainActivity", "total time : ${(endTime - startTime)}")
                PreviewActivity.bitmap = Bitmap.createBitmap(bitmap!!, (bitmap.width * rectangle.x).toInt(), (bitmap.height * rectangle.y).toInt(), (bitmap.width * rectangle.width).toInt(),
                        (bitmap.height * rectangle.height).toInt(), null, false)
//                PreviewActivity.bitmap = bitmap
                val intent = Intent(this, PreviewActivity::class.java)

                startActivity(intent)
            }

        }

    }

    /**
     * 选择变换
     *
     * @param origin 原图
     * @param alpha  旋转角度，可正可负
     * @return 旋转后的图片
     */
    private fun rotateBitmap(origin: Bitmap?, alpha: Int): Bitmap? {
        if (origin == null) {
            return null
        }
        val width = origin.width
        val height = origin.height
        val matrix = Matrix()
        matrix.setRotate(alpha.toFloat())
        // 围绕原地进行旋转
        val newBM = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false)
        if (newBM == origin) {
            return newBM
        }
        origin.recycle()
        return newBM
    }


    override fun onStart() {
        super.onStart()
        if (hasCameraPermission) {
            fotoapparatSwitcher!!.start()
        }
    }

    override fun onStop() {
        super.onStop()
        if (hasCameraPermission) {
            fotoapparatSwitcher!!.stop()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (permissionsDelegate.resultGranted(requestCode, permissions, grantResults)) {
            fotoapparatSwitcher!!.start()
            camera_view.visibility = View.VISIBLE
        }
    }

}