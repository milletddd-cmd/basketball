package com.example.basketballtraining


import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log


import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts


import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView


import androidx.core.content.ContextCompat


import com.example.basketballtraining.ui.PoseDetector
import com.example.basketballtraining.ui.theme.BasketballTrainingTheme



class MainActivity : ComponentActivity(){


    private var cameraPermissionGranted =
        mutableStateOf(false)



    private val cameraPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ){

                granted ->

            cameraPermissionGranted.value = granted

        }



    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)



        cameraPermissionGranted.value =

            ContextCompat.checkSelfPermission(

                this,

                Manifest.permission.CAMERA

            ) == PackageManager.PERMISSION_GRANTED




        setContent {


            BasketballTrainingTheme {


                BasketballApp(

                    cameraPermissionGranted =
                        cameraPermissionGranted.value,


                    requestCameraPermission = {

                        cameraPermissionLauncher.launch(

                            Manifest.permission.CAMERA

                        )

                    }

                )

            }

        }

    }

}






@Composable
fun BasketballApp(

    cameraPermissionGranted:Boolean,

    requestCameraPermission:()->Unit

){


    var currentPage by remember {

        mutableStateOf("home")

    }



    when(currentPage){



        "home" -> {


            HomeScreen(

                startTraining = {

                    currentPage="training"

                }

            )


        }




        "training" -> {


            TrainingScreen(

                cameraPermissionGranted,

                requestCameraPermission

            )


        }


    }


}







@Composable
fun HomeScreen(

    startTraining:()->Unit

){



    Column(

        modifier =
            Modifier.fillMaxSize(),


        horizontalAlignment =
            Alignment.CenterHorizontally,


        verticalArrangement =
            Arrangement.Center


    ){



        Text(

            text="🏀 Basketball Training",

            fontSize=28.sp,

            fontWeight=FontWeight.Bold

        )



        Spacer(

            modifier =
                Modifier.height(30.dp)

        )



        Button(

            onClick=startTraining

        ){


            Text(

                "开始训练 START TRAINING"

            )


        }


    }


}








@Composable
fun TrainingScreen(


    cameraPermissionGranted:Boolean,


    requestCameraPermission:()->Unit


){



    val context =
        LocalContext.current
    var posePoints by remember {
        mutableStateOf<List<Pair<Float, Float>>>(emptyList())
    }
    val leftElbowAngle =
        if (posePoints.size > 15) {
            calculateAngle(
                posePoints[11],
                posePoints[13],
                posePoints[15]
            )
        } else {
            0.0
        }

    val rightElbowAngle =
        if (posePoints.size > 16) {
            calculateAngle(
                posePoints[12],
                posePoints[14],
                posePoints[16]
            )
        } else {
            0.0
        }

    val leftKneeAngle =
        if (posePoints.size > 27) {
            calculateAngle(
                posePoints[23],
                posePoints[25],
                posePoints[27]
            )
        } else {
            0.0
        }

    val rightKneeAngle =
        if (posePoints.size > 28) {
            calculateAngle(
                posePoints[24],
                posePoints[26],
                posePoints[28]
            )
        } else {
            0.0
        }


    val poseDetector = remember {

        PoseDetector(context).apply {

            onPoseDetected = { points ->

                posePoints = points

            }

            setup()

        }

    }




    Column(

        modifier =
            Modifier.fillMaxSize()

    ){



        Text(

            text="🏀 动作分析 Motion Analysis",

            fontSize=25.sp,

            fontWeight=FontWeight.Bold,


            modifier =
                Modifier.padding(20.dp)

        )





        if(cameraPermissionGranted){



            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {

                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    poseDetector = poseDetector
                )

                PoseOverlay(
                    points = posePoints,
                    modifier = Modifier.fillMaxSize()
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(
                            Color.Black.copy(alpha = 0.65f)
                        )
                        .padding(12.dp)
                ) {

                    Text(
                        text = "Joint Angles",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Left Elbow: ${leftElbowAngle.toInt()}°",
                        color = Color.White,
                        fontSize = 14.sp
                    )

                    Text(
                        text = "Right Elbow: ${rightElbowAngle.toInt()}°",
                        color = Color.White,
                        fontSize = 14.sp
                    )

                    Text(
                        text = "Left Knee: ${leftKneeAngle.toInt()}°",
                        color = Color.White,
                        fontSize = 14.sp
                    )

                    Text(
                        text = "Right Knee: ${rightKneeAngle.toInt()}°",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }



        }else{



            Button(

                onClick=requestCameraPermission,


                modifier =
                    Modifier.padding(20.dp)

            ){


                Text(

                    "允许摄像头权限 Allow Camera Permission"

                )


            }


        }


    }


}









@Composable
fun CameraPreview(


    modifier:Modifier = Modifier,


    poseDetector:PoseDetector


){



    AndroidView(


        modifier=modifier,


        factory={context ->



            val previewView =
                PreviewView(context)

            previewView.scaleType =
                PreviewView.ScaleType.FIT_CENTER



            val cameraProviderFuture =

                ProcessCameraProvider
                    .getInstance(context)




            cameraProviderFuture.addListener({



                val cameraProvider =

                    cameraProviderFuture.get()




                val preview =

                    Preview.Builder()
                        .build()




                val imageAnalyzer =

                    ImageAnalysis.Builder()

                        .setBackpressureStrategy(

                            ImageAnalysis
                                .STRATEGY_KEEP_ONLY_LATEST

                        )

                        .build()




                val cameraExecutor =
                    java.util.concurrent.Executors.newSingleThreadExecutor()

                imageAnalyzer.setAnalyzer(
                    cameraExecutor
                ) { imageProxy ->

                    Log.d(
                        "CameraAnalyzer",
                        "Frame received"
                    )

                    poseDetector.detect(
                        imageProxy
                    )
                }



                preview.setSurfaceProvider(

                    previewView.surfaceProvider

                )




                cameraProvider.unbindAll()



                cameraProvider.bindToLifecycle(


                    context as androidx.lifecycle.LifecycleOwner,


                    CameraSelector.DEFAULT_BACK_CAMERA,


                    preview,


                    imageAnalyzer


                )



            },


                ContextCompat.getMainExecutor(context)



            )




            previewView



        }


    )


}
@Composable
fun PoseOverlay(
    points: List<Pair<Float, Float>>,
    modifier: Modifier = Modifier
) {

    Canvas(
        modifier = modifier
    ) {

        if (points.isEmpty()) {
            return@Canvas
        }

        // MediaPipe处理后的图片尺寸
        // 我们在PoseDetctor.kt中进行了90°旋转
        val imageWidth = 480f
        val imageHeight = 640f

        // 图片比例
        val imageAspectRatio =
            imageWidth / imageHeight

        // 当前手机显示区域比例
        val viewAspectRatio =
            size.width / size.height

        val scale: Float
        val offsetX: Float
        val offsetY: Float

        /*
         * PreviewView使用FIT_CENTER
         *
         * 所以摄像头图片不会铺满整个区域，
         * 而是保持原始比例显示。
         */

        if (viewAspectRatio > imageAspectRatio) {

            // 显示区域比较宽
            scale =
                size.height / imageHeight

            offsetX =
                (size.width - imageWidth * scale) / 2f

            offsetY = 0f

        } else {

            // 显示区域比较高
            scale =
                size.width / imageWidth

            offsetX = 0f

            offsetY =
                (size.height - imageHeight * scale) / 2f
        }


        /*
         * MediaPipe Pose连接关系
         */

        val connections = listOf(

            // Face
            0 to 1,
            1 to 2,
            2 to 3,
            3 to 7,

            0 to 4,
            4 to 5,
            5 to 6,
            6 to 8,

            // Mouth
            9 to 10,

            // Shoulders
            11 to 12,

            // Left arm
            11 to 13,
            13 to 15,

            // Right arm
            12 to 14,
            14 to 16,

            // Torso
            11 to 23,
            12 to 24,
            23 to 24,

            // Left leg
            23 to 25,
            25 to 27,
            27 to 29,
            29 to 31,

            // Right leg
            24 to 26,
            26 to 28,
            28 to 30,
            30 to 32
        )


        /*
         * 画绿色骨骼线
         */

        connections.forEach { (start, end) ->

            if (
                start < points.size &&
                end < points.size &&
                points[start].first in 0f..1f &&
                points[start].second in 0f..1f &&
                points[end].first in 0f..1f &&
                points[end].second in 0f..1f
            ) {

                val startPoint =
                    points[start]

                val endPoint =
                    points[end]


                val startX =
                    startPoint.first *
                            imageWidth *
                            scale +
                            offsetX

                val startY =
                    startPoint.second *
                            imageHeight *
                            scale +
                            offsetY

                val endX =
                    endPoint.first *
                            imageWidth *
                            scale +
                            offsetX

                val endY =
                    endPoint.second *
                            imageHeight *
                            scale +
                            offsetY


                drawLine(

                    start = Offset(
                        startX,
                        startY
                    ),

                    end = Offset(
                        endX,
                        endY
                    ),

                    color = Color.Green,

                    strokeWidth = 5f,

                    cap = StrokeCap.Round
                )
            }
        }


        /*
         * 画红色人体关键点
         */

        points.forEach { point ->

            // Ignore landmarks predicted outside the camera image
            if (
                point.first < 0f ||
                point.first > 1f ||
                point.second < 0f ||
                point.second > 1f
            ) {
                return@forEach
            }

            val x =
                point.first *
                        imageWidth *
                        scale +
                        offsetX

            val y =
                point.second *
                        imageHeight *
                        scale +
                        offsetY

            drawCircle(
                color = Color.Red,
                radius = 7f,
                center = Offset(
                    x,
                    y
                )
            )
        }
    }
}
fun calculateAngle(
    a: Pair<Float, Float>,
    b: Pair<Float, Float>,
    c: Pair<Float, Float>
): Double {

    val angle1 =
        kotlin.math.atan2(
            (a.second - b.second).toDouble(),
            (a.first - b.first).toDouble()
        )

    val angle2 =
        kotlin.math.atan2(
            (c.second - b.second).toDouble(),
            (c.first - b.first).toDouble()
        )

    var angle =
        kotlin.math.abs(
            Math.toDegrees(angle2 - angle1)
        )

    if (angle > 180) {
        angle = 360 - angle
    }

    return angle
}