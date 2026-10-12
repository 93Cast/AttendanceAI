package com.example.attendanceai.camera

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.unit.sp
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.attendanceai.ai.YoloDetector
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.attendanceai.ai.Detection
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton

@Composable
fun CameraScreen(
    onBack: () -> Unit,
    onSaveAttendance: (String, Int) -> Unit
) {

    val context = LocalContext.current

    // Detector YOLO
    val detector = remember {
        YoloDetector(context)
    }

    // Número de personas detectadas
    var peopleCount by remember {
        mutableIntStateOf(0)
    }
    // Lista de personas detectadas por YOLO.
    var detections by remember {
        mutableStateOf<List<Detection>>(emptyList())
    }

// Controla si mostramos el formulario de guardado.
    var showSaveDialog by remember {
        mutableStateOf(false)
    }

// Nombre del salón introducido por el usuario.
    var classroom by remember {
        mutableStateOf("")
    }

    // Executor para procesar los frames de la cámara
    val analysisExecutor = remember {
        Executors.newSingleThreadExecutor()
    }

    // Vista donde se mostrará la cámara
    val previewView = remember {
        PreviewView(context)
    }

    /*
     * Inicializamos YOLO.
     */
    LaunchedEffect(Unit) {

        Log.d(
            "CameraScreen",
            "Inicializando detector YOLO..."
        )

        detector.initialize()

        Log.d(
            "CameraScreen",
            "Detector YOLO listo"
        )
    }

    /*
     * Inicializamos CameraX.
     */
    LaunchedEffect(Unit) {

        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({

            val cameraProvider =
                cameraProviderFuture.get()

            /*
             * PREVIEW
             *
             * Esta es la imagen que verá el usuario.
             */
            val preview =
                Preview.Builder()
                    .build()

            preview.surfaceProvider =
                previewView.surfaceProvider

            /*
             * IMAGE ANALYSIS
             *
             * Aquí recibimos los frames que serán
             * enviados al modelo YOLO.
             */
            val imageAnalyzer =
                ImageAnalysis.Builder()
                    /*
                     * Si llegan muchos frames, solamente
                     * procesamos el más reciente.
                     */
                    .setBackpressureStrategy(
                        ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                    )
                    .build()

            /*
             * Procesamos cada frame.
             */
            imageAnalyzer.setAnalyzer(
                analysisExecutor
            ) { imageProxy ->

                processImage(
                    imageProxy = imageProxy,
                    detector = detector
                ) { results ->

                    // Guardamos las coordenadas de cada persona.
                    detections = results

                    // Actualizamos el contador.
                    peopleCount = results.size
                }
            }

            /*
             * Utilizamos la cámara trasera.
             */
            val cameraSelector =
                CameraSelector.DEFAULT_BACK_CAMERA

            try {

                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    context as androidx.lifecycle.LifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )

                Log.d(
                    "CameraScreen",
                    "CameraX + YOLO iniciado correctamente"
                )

            } catch (e: Exception) {

                Log.e(
                    "CameraScreen",
                    "Error iniciando cámara",
                    e
                )
            }

        }, ContextCompat.getMainExecutor(context))
    }
// Cada persona detectada tendrá un color distinto.
// Si hay más personas que colores, la paleta se repetirá.
    val detectionColors = listOf(
        Color(0xFF00E676), // Verde
        Color(0xFF2979FF), // Azul
        Color(0xFFFF1744), // Rojo
        Color(0xFFFFD600), // Amarillo
        Color(0xFFE040FB), // Morado
        Color(0xFF00E5FF), // Celeste
        Color(0xFFFF9100), // Naranja
        Color(0xFF76FF03)  // Verde lima
    )
    /*
     * Interfaz de la cámara.
     */
    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        /*
         * Vista de la cámara.
         */
        AndroidView(
            factory = {
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        /*
         * Capa visual que dibuja un rectángulo
         * por cada persona detectada.
         */
        Canvas(Modifier.fillMaxSize()) {
            detections.forEachIndexed { index, detection ->

                // Selecciona el color correspondiente a esta detección.
                val boxColor = detectionColors[
                    index % detectionColors.size
                ]

                // Convierte las coordenadas normalizadas a píxeles.
                val left = detection.x1 * size.width
                val top = detection.y1 * size.height
                val right = detection.x2 * size.width
                val bottom = detection.y2 * size.height

                // Dibuja el recuadro alrededor de la persona.
                drawRect(
                    color = boxColor,
                    topLeft = Offset(left, top),
                    size = Size(
                        width = right - left,
                        height = bottom - top
                    ),
                    style = Stroke(width = 4.dp.toPx())
                )
            }
        }
// Botón para registrar la asistencia.
// Se coloca encima de la cámara, en la parte inferior.
        Button(
            onClick = {
                showSaveDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(
                    start = 24.dp,
                    end = 24.dp,
                    bottom = 24.dp
                )
        ) {
            Text("Registrar asistencia")
        }
        /*
         * Contador de personas.
         */
        Button(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 12.dp, top = 12.dp)
                .heightIn(min = 32.dp),
            contentPadding = PaddingValues(
                horizontal = 10.dp,
                vertical = 4.dp
            ),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black.copy(alpha = 0.70f)
            )
        ) {
            Text(
                text = "← Inicio",
                fontSize = 12.sp
            )
        }

        Text(
            text = "Personas detectadas: $peopleCount",

            color = Color.White,

            fontSize = 22.sp,

            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(24.dp)
                .background(
                    Color.Black.copy(alpha = 0.65f)
                )
                .padding(
                    horizontal = 16.dp,
                    vertical = 10.dp
                )
        )
    }
    // Diálogo para ingresar el aula y guardar la asistencia.
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = {
                showSaveDialog = false
            },
            title = {
                Text("Registrar asistencia")
            },
            text = {
                OutlinedTextField(
                    value = classroom,
                    onValueChange = { classroom = it },
                    label = {
                        Text("Nombre del aula")
                    },
                    placeholder = {
                        Text("Ej. 3°C")
                    },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (classroom.isNotBlank()) {
                            onSaveAttendance(
                                classroom.trim(),
                                peopleCount
                            )

                            showSaveDialog = false
                            classroom = ""
                        }
                    },
                    enabled = classroom.isNotBlank()
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSaveDialog = false
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }


}


/**
 * Procesa un frame de CameraX.
 */

private fun processImage(
    imageProxy: ImageProxy,
    detector: YoloDetector,
    onResult: (List<Detection>) -> Unit
) {
    try {
        val bitmap = imageProxyToBitmap(imageProxy)

        if (bitmap != null) {
            val results = detector.detect(bitmap)

            Log.d(
                "CameraScreen",
                "Personas detectadas: ${results.size}"
            )

            onResult(results)
        }
    } catch (e: Exception) {
        Log.e(
            "CameraScreen",
            "Error procesando frame",
            e
        )
    } finally {
        imageProxy.close()
    }
}


/**
 * Convierte el formato YUV_420_888 de CameraX
 * en un Bitmap RGB.
 */
private fun imageProxyToBitmap(
    imageProxy: ImageProxy
): Bitmap? {

    return try {

        /*
         * Verificamos que CameraX esté utilizando
         * el formato esperado.
         */
        if (imageProxy.format != ImageFormat.YUV_420_888) {

            Log.e(
                "CameraScreen",
                "Formato inesperado: ${imageProxy.format}"
            )

            return null
        }

        /*
         * Obtenemos los tres planos:
         *
         * Plane 0 = Y
         * Plane 1 = U
         * Plane 2 = V
         */
        val planes =
            imageProxy.planes

        val yBuffer =
            planes[0].buffer

        val uBuffer =
            planes[1].buffer

        val vBuffer =
            planes[2].buffer

        /*
         * Guardamos los bytes de cada plano.
         */
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

        /*
         * Copiamos Y.
         */
        yBuffer.get(
            nv21,
            0,
            ySize
        )

        /*
         * NV21 espera V antes de U.
         */
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

        /*
         * Creamos una imagen YUV.
         */
        val yuvImage =
            YuvImage(
                nv21,
                ImageFormat.NV21,
                imageProxy.width,
                imageProxy.height,
                null
            )

        /*
         * Comprimimos temporalmente a JPEG.
         */
        val outputStream =
            ByteArrayOutputStream()

        yuvImage.compressToJpeg(
            Rect(
                0,
                0,
                imageProxy.width,
                imageProxy.height
            ),
            100,
            outputStream
        )

        /*
         * Convertimos JPEG → Bitmap.
         */
        val imageBytes =
            outputStream.toByteArray()

        val bitmap =
            android.graphics.BitmapFactory.decodeByteArray(
                imageBytes,
                0,
                imageBytes.size
            )

        if (bitmap == null) {
            return null
        }

        /*
         * CameraX puede entregar la imagen rotada.
         *
         * Obtenemos el ángulo correspondiente.
         */
        val rotationDegrees =
            imageProxy.imageInfo.rotationDegrees

        if (rotationDegrees == 0) {

            bitmap

        } else {

            /*
             * Rotamos el Bitmap para que YOLO
             * reciba la orientación correcta.
             */
            val matrix =
                Matrix().apply {
                    postRotate(
                        rotationDegrees.toFloat()
                    )
                }

            Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                matrix,
                true
            )
        }

    } catch (e: Exception) {

        Log.e(
            "CameraScreen",
            "Error convirtiendo YUV a Bitmap",
            e
        )

        null
    }
}
