package com.iot.medion

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import android.content.Intent

class PredictionActivity : AppCompatActivity() {

    private lateinit var imageViewWound: ImageView
    private lateinit var textViewPredictionResult: TextView // 이 뷰들은 여전히 필요하지만, 이제는 SelfDiagnosisActivity에서 값을 설정
    private lateinit var textViewConfidence: TextView       // SelfDiagnosisActivity에서 값을 설정

    private lateinit var interpreter: Interpreter
    private lateinit var labels: List<String>

    companion object {
        const val IMAGE_WIDTH = 224
        const val IMAGE_HEIGHT = 224
        const val NUM_CLASSES = 5 // !!! Master 학생의 모델 클래스 수에 맞게 5로 변경되었습니다 !!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_prediction) // 이 액티비티의 레이아웃을 사용

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        imageViewWound = findViewById(R.id.imageView_wound_prediction)
        textViewPredictionResult = findViewById(R.id.textView_prediction_result) // 초기화는 유지
        textViewConfidence = findViewById(R.id.textView_confidence) // 초기화는 유지

        try {
            val modelByteBuffer = FileUtil.loadMappedFile(this, "WoundNet.tflite")
            interpreter = Interpreter(modelByteBuffer)
            labels = FileUtil.loadLabels(this, "labels.txt") // Master 학생의 labels.txt 파일을 활용합니다.
            Log.d("PredictionActivity", "TFLite 모델 및 라벨 로드 성공.")

        } catch (e: IOException) {
            Log.e("PredictionActivity", "모델 또는 라벨 로드 실패: ${e.message}", e)
            Toast.makeText(this, "오류: 모델 파일을 불러올 수 없습니다.", Toast.LENGTH_LONG).show()
            setResult(RESULT_CANCELED) // 모델 로드 실패 시 취소 결과 반환
            finish()
            return
        }

        val imageUriString = intent.getStringExtra("image_uri")
        if (imageUriString != null) {
            val imageUri = Uri.parse(imageUriString)
            imageViewWound.setImageURI(imageUri) // 선택된 이미지를 화면에 표시

            // 예측 실행 및 결과 반환
            val (predictedLabel, confidence) = runInference(imageUri)

            // 이제 PredictionActivity는 결과를 표시하는 대신 SelfDiagnosisActivity로 결과를 돌려줍니다.
            val resultIntent = Intent().apply {
                putExtra("predicted_label", predictedLabel)
                putExtra("confidence", confidence)
            }
            // 이 PredictionActivity가 성공적으로 분석을 마치고 결과가 있음을 알립니다.
            setResult(RESULT_OK, resultIntent)
            finish() // 이 Activity를 종료하고 SelfDiagnosisActivity로 돌아갑니다.

        } else {
            Toast.makeText(this, "오류: 분석할 이미지가 없습니다.", Toast.LENGTH_LONG).show()
            setResult(RESULT_CANCELED) // 이미지가 없으면 취소 결과 반환
            finish()
        }
    }

    /**
     * TFLite 모델을 사용하여 이미지에 대한 상처 분류 추론을 실행하고 결과를 반환합니다.
     * @param imageUri 예측할 이미지의 URI
     * @return Pair<String, Float> - 예측된 상처 라벨과 신뢰도
     */
    private fun runInference(imageUri: Uri): Pair<String, Float> {
        try {
            val inputStream = contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap == null) {
                Log.e("PredictionActivity", "이미지를 비트맵으로 변환할 수 없습니다.")
                return "이미지 변환 오류" to 0.0f
            }

            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, IMAGE_WIDTH, IMAGE_HEIGHT, true)

            // !!! 중요: Python 학습 코드와 픽셀 값 정규화 방식이 EXACTLY 일치해야 합니다. !!!
            // 현재 코드는 0-1 범위로 정규화하고 있습니다.
            val imgData = ByteBuffer.allocateDirect(4 * IMAGE_WIDTH * IMAGE_HEIGHT * 3)
            imgData.order(ByteOrder.nativeOrder())

            val intValues = IntArray(IMAGE_WIDTH * IMAGE_HEIGHT)
            resizedBitmap.getPixels(intValues, 0, resizedBitmap.width, 0, 0, resizedBitmap.width, resizedBitmap.height)

            var pixel = 0
            for (i in 0 until IMAGE_WIDTH) {
                for (j in 0 until IMAGE_HEIGHT) {
                    val `val` = intValues[pixel++]
                    imgData.putFloat(((`val` shr 16) and 0xFF) / 255.0f) // Red
                    imgData.putFloat(((`val` shr 8) and 0xFF) / 255.0f)  // Green
                    imgData.putFloat((`val` and 0xFF) / 255.0f)       // Blue
                }
            }

            val inputTensorBuffer = TensorBuffer.createFixedSize(intArrayOf(1, IMAGE_WIDTH, IMAGE_HEIGHT, 3), DataType.FLOAT32)
            inputTensorBuffer.loadBuffer(imgData)

            val outputTensorBuffer = TensorBuffer.createFixedSize(intArrayOf(1, NUM_CLASSES), DataType.FLOAT32)

            interpreter.run(inputTensorBuffer.buffer, outputTensorBuffer.buffer.rewind())

            val outputArray = outputTensorBuffer.floatArray

            var maxConfidence = -1.0f
            var predictedIndex = -1

            for (i in outputArray.indices) {
                if (outputArray[i] > maxConfidence) {
                    maxConfidence = outputArray[i]
                    predictedIndex = i
                }
            }

            val predictedClassLabel = if (predictedIndex != -1 && predictedIndex < labels.size) {
                labels[predictedIndex]
            } else {
                "분류 실패"
            }

            // 디버깅을 위해 Logcat에 출력 (이 액티비티에서는 UI에 직접 표시하지 않음)
            Log.d("PredictionActivity", "예측 결과: $predictedClassLabel, 확률: ${maxConfidence * 100}%")

            return predictedClassLabel to maxConfidence

        } catch (e: Exception) {
            Log.e("PredictionActivity", "추론 실행 중 오류 발생: ${e.message}", e)
            return "추론 오류 발생" to 0.0f
        }
    }
}