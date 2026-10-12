package com.example.attendanceai.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.google.ai.edge.litert.CompiledModel

data class Detection(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val confidence: Float
)

class YoloDetector(
    private val context: Context
) {

    companion object {
        private const val TAG = "YoloDetector"

        private const val MODEL_NAME = "yolo26n.tflite"

        private const val INPUT_SIZE = 640

        // Clase 0 de COCO = persona
        private const val PERSON_CLASS = 0

        private const val CONFIDENCE_THRESHOLD = 0.50f

        private const val IOU_THRESHOLD = 0.45f

        private const val NUM_DETECTIONS = 8400
    }

    private var model: CompiledModel? = null

    /**
     * Carga YOLO26n.
     */
    fun initialize() {

        Log.d(TAG, "================================")
        Log.d(TAG, "Inicializando YOLO26n")

        try {

            model = CompiledModel.create(
                context.assets,
                MODEL_NAME
            )

            Log.d(
                TAG,
                "MODELO CARGADO CORRECTAMENTE"
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "ERROR AL CARGAR EL MODELO",
                e
            )
        }
    }

    /**
     * Detecta personas en un Bitmap.
     *
     * Esta función será utilizada tanto por:
     *
     * personas.jpg
     *
     * como por:
     *
     * frames de CameraX.
     */
    fun detect(bitmap: Bitmap): List<Detection> {

        val currentModel = model

        if (currentModel == null) {

            Log.e(
                TAG,
                "El modelo no está inicializado"
            )

            return emptyList()
        }

        try {

            // =====================================================
            // 1. REDIMENSIONAR IMAGEN
            // =====================================================

            val resizedBitmap =
                Bitmap.createScaledBitmap(
                    bitmap,
                    INPUT_SIZE,
                    INPUT_SIZE,
                    true
                )

            // =====================================================
            // 2. PREPARAR INPUT
            // =====================================================

            val inputData =
                FloatArray(
                    3 *
                            INPUT_SIZE *
                            INPUT_SIZE
                )

            val pixels =
                IntArray(
                    INPUT_SIZE *
                            INPUT_SIZE
                )

            resizedBitmap.getPixels(
                pixels,
                0,
                INPUT_SIZE,
                0,
                0,
                INPUT_SIZE,
                INPUT_SIZE
            )

            val planeSize =
                INPUT_SIZE *
                        INPUT_SIZE

            for (i in pixels.indices) {

                val pixel =
                    pixels[i]

                val red =
                    ((pixel shr 16) and 0xFF) / 255.0f

                val green =
                    ((pixel shr 8) and 0xFF) / 255.0f

                val blue =
                    (pixel and 0xFF) / 255.0f

                // NCHW
                inputData[i] =
                    red

                inputData[
                    planeSize + i
                ] =
                    green

                inputData[
                    (planeSize * 2) + i
                ] =
                    blue
            }

            // =====================================================
            // 3. BUFFERS
            // =====================================================

            val inputBuffers =
                currentModel.createInputBuffers()

            val outputBuffers =
                currentModel.createOutputBuffers()

            // =====================================================
            // 4. ENVIAR IMAGEN
            // =====================================================

            inputBuffers[0].writeFloat(
                inputData
            )

            // =====================================================
            // 5. EJECUTAR YOLO
            // =====================================================

            currentModel.run(
                inputBuffers,
                outputBuffers
            )

            // =====================================================
            // 6. OBTENER OUTPUT
            // =====================================================

            val output =
                outputBuffers[0].readFloat()

            // =====================================================
            // 7. EXTRAER DETECCIONES
            // =====================================================

            val detections =
                mutableListOf<Detection>()

            for (i in 0 until NUM_DETECTIONS) {

                val x =
                    output[i]

                val y =
                    output[
                        NUM_DETECTIONS + i
                    ]

                val width =
                    output[
                        (NUM_DETECTIONS * 2) + i
                    ]

                val height =
                    output[
                        (NUM_DETECTIONS * 3) + i
                    ]

                // Clase 0 = persona
                val personConfidence =
                    output[
                        (NUM_DETECTIONS * 4) + i
                    ]

                if (
                    personConfidence <
                    CONFIDENCE_THRESHOLD
                ) {
                    continue
                }

                val x1 =
                    x - width / 2f

                val y1 =
                    y - height / 2f

                val x2 =
                    x + width / 2f

                val y2 =
                    y + height / 2f

                detections.add(
                    Detection(
                        x1 = x1,
                        y1 = y1,
                        x2 = x2,
                        y2 = y2,
                        confidence =
                            personConfidence
                    )
                )
            }

            // =====================================================
            // 8. NMS
            // =====================================================

            return nonMaximumSuppression(
                detections,
                IOU_THRESHOLD
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Error durante detección",
                e
            )

            return emptyList()
        }
    }

    /**
     * NMS.
     */
    private fun nonMaximumSuppression(
        detections: List<Detection>,
        iouThreshold: Float
    ): List<Detection> {

        val sorted =
            detections
                .sortedByDescending {
                    it.confidence
                }
                .toMutableList()

        val selected =
            mutableListOf<Detection>()

        while (sorted.isNotEmpty()) {

            val best =
                sorted.removeAt(0)

            selected.add(best)

            val iterator =
                sorted.iterator()

            while (iterator.hasNext()) {

                val candidate =
                    iterator.next()

                val iou =
                    calculateIoU(
                        best,
                        candidate
                    )

                if (iou > iouThreshold) {
                    iterator.remove()
                }
            }
        }

        return selected
    }

    /**
     * Calcula IoU entre dos cajas.
     */
    private fun calculateIoU(
        a: Detection,
        b: Detection
    ): Float {

        val left =
            maxOf(a.x1, b.x1)

        val top =
            maxOf(a.y1, b.y1)

        val right =
            minOf(a.x2, b.x2)

        val bottom =
            minOf(a.y2, b.y2)

        val width =
            maxOf(
                0f,
                right - left
            )

        val height =
            maxOf(
                0f,
                bottom - top
            )

        val intersection =
            width * height

        val areaA =
            maxOf(
                0f,
                a.x2 - a.x1
            ) *
                    maxOf(
                        0f,
                        a.y2 - a.y1
                    )

        val areaB =
            maxOf(
                0f,
                b.x2 - b.x1
            ) *
                    maxOf(
                        0f,
                        b.y2 - b.y1
                    )

        val union =
            areaA +
                    areaB -
                    intersection

        if (union <= 0f) {
            return 0f
        }

        return intersection / union
    }

    /**
     * Cierra el detector.
     */
    fun close() {

        model = null

        Log.d(
            TAG,
            "YoloDetector cerrado"
        )
    }
}
