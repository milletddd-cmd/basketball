package com.example.basketballtraining.ui

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.SystemClock
import android.util.Log

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy

import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

import java.io.ByteArrayOutputStream

class PoseDetector(
    private val context: Context
) {

    private var poseLandmarker: PoseLandmarker? = null

    var onPoseDetected: ((List<Pair<Float, Float>>) -> Unit)? = null


    fun setup() {

        try {

            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("pose_landmarker_full.task")
                .build()


            val options =
                PoseLandmarker.PoseLandmarkerOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setRunningMode(
                        RunningMode.LIVE_STREAM
                    )
                    .setResultListener { result: PoseLandmarkerResult, _ ->

                        val landmarks =
                            result.landmarks()

                        Log.d(
                            "PoseDetector",
                            "Detected people: ${landmarks.size}"
                        )

                        if (landmarks.isNotEmpty()) {

                            val person =
                                landmarks[0]

                            val points =
                                person.map { landmark ->

                                    Pair(
                                        landmark.x(),
                                        landmark.y()
                                    )

                                }

                            onPoseDetected?.invoke(points)
                        }
                    }
                    .setErrorListener { error ->

                        Log.e(
                            "PoseDetector",
                            "MediaPipe error: ${error.message}",
                            error
                        )
                    }                    .build()


            poseLandmarker =
                PoseLandmarker.createFromOptions(
                    context,
                    options
                )


            Log.d(
                "PoseDetector",
                "Pose Landmarker 初始化成功"
            )

        } catch (e: Throwable) {

            Log.e(
                "PoseDetector",
                "模型加载失败: ${e.message}",
                e
            )

        }
    }


    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    fun detect(
        imageProxy: ImageProxy
    ) {
        try {

            val image =
                imageProxy.image
                    ?: return


            val yBuffer =
                image.planes[0].buffer

            val uBuffer =
                image.planes[1].buffer

            val vBuffer =
                image.planes[2].buffer


            val ySize =
                yBuffer.remaining()

            val uSize =
                uBuffer.remaining()

            val vSize =
                vBuffer.remaining()


            val nv21 =
                ByteArray(
                    ySize + uSize + vSize
                )


            yBuffer.get(
                nv21,
                0,
                ySize
            )

            vBuffer.get(
                nv21,
                ySize,
                vSize
            )

            uBuffer.get(
                nv21,
                ySize + vSize,
                uSize
            )


            val yuvImage =
                YuvImage(
                    nv21,
                    ImageFormat.NV21,
                    image.width,
                    image.height,
                    null
                )


            val outputStream =
                ByteArrayOutputStream()


            yuvImage.compressToJpeg(
                Rect(
                    0,
                    0,
                    image.width,
                    image.height
                ),
                90,
                outputStream
            )


            val jpegBytes =
                outputStream.toByteArray()


            val bitmap =
                android.graphics.BitmapFactory
                    .decodeByteArray(
                        jpegBytes,
                        0,
                        jpegBytes.size
                    )


            if (bitmap != null) {

                val rotationDegrees =
                    imageProxy.imageInfo.rotationDegrees

                val rotatedBitmap = if (rotationDegrees != 0) {

                    val matrix =
                        android.graphics.Matrix()

                    matrix.postRotate(
                        rotationDegrees.toFloat()
                    )

                    android.graphics.Bitmap.createBitmap(
                        bitmap,
                        0,
                        0,
                        bitmap.width,
                        bitmap.height,
                        matrix,
                        true
                    )

                } else {

                    bitmap
                }

                val mpImage =
                    BitmapImageBuilder(
                        rotatedBitmap
                    ).build()


                val timestamp =
                    SystemClock.uptimeMillis()


                Log.d(
                    "PoseDetector",
                    "Sending frame to MediaPipe"
                )


                poseLandmarker?.detectAsync(
                    mpImage,
                    timestamp
                )

                // 注意：由于 detectAsync 是异步的，
                // 我们不能在这里立即 recycle rotatedBitmap，
                // 否则 MediaPipe 在后台线程处理时会因为图片已被回收而崩溃或出错。
                // 在 LIVE_STREAM 模式下，通常建议使用一个图片池或者让 GC 处理。
                // 为了简单起见，我们暂时移除 recycle，或者确保在结果回调后再回收。

            }


        } catch (e: Throwable) {

            Log.e(
                "PoseDetector",
                "检测过程中发生错误: ${e.message}",
                e
            )

        } finally {

            imageProxy.close()

        }
    }
}