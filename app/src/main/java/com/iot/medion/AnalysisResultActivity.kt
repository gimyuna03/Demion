package com.iot.medion

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class AnalysisResultActivity : AppCompatActivity() {

    private lateinit var analysisResultTitle: TextView
    private lateinit var imageViewWoundPhoto: ImageView
    private lateinit var textViewDiseaseDesc: TextView
    private lateinit var textViewSymptomStatus: TextView
    private lateinit var textViewSolution: TextView
    private lateinit var buttonConfirm: Button
    private lateinit var textViewAnalysisResult: TextView

    private var currentRecordId: Long = -1
    private var currentUserId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_analysis_result)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // --- 1. UI 요소들을 XML ID와 연결 ---
        analysisResultTitle = findViewById(R.id.analysisResultTitle)
        imageViewWoundPhoto = findViewById(R.id.imageViewWoundPhoto)
        textViewDiseaseDesc = findViewById(R.id.textViewDiseaseDesc)
        textViewSymptomStatus = findViewById(R.id.textViewSymptomStatus)
        textViewSolution = findViewById(R.id.textViewSolution)
        textViewAnalysisResult = findViewById(R.id.textViewAnalysisResult)
        buttonConfirm = findViewById(R.id.buttonConfirm)

        // --- 2. Intent에서 전달받은 데이터 확인 ---
        currentRecordId = intent.getLongExtra("RECORD_ID", -1L)
        currentUserId = intent.getLongExtra("USER_ID", -1L)
        val imageUriString = intent.getStringExtra("IMAGE_URI") // SelfDiagnosisActivity에서 전달받은 Image Path

        if (currentRecordId == -1L || currentUserId == -1L || imageUriString == null) {
            Toast.makeText(this, "오류: 분석 결과를 불러올 수 없습니다. 다시 시도해주세요.", Toast.LENGTH_LONG).show()
            Log.e("AnalysisResult", "필수 데이터 누락: recordId=$currentRecordId, userId=$currentUserId, imageUri=$imageUriString")
            finish()
            return
        }

        // --- 3. 받은 이미지 URI를 ImageView에 표시 ---
        try {
            val imageUri = Uri.parse(imageUriString)
            imageViewWoundPhoto.setImageURI(imageUri)
            imageViewWoundPhoto.visibility = View.VISIBLE
            Log.d("AnalysisResult", "이미지 URI 로드 성공: $imageUriString")
        } catch (e: Exception) {
            Toast.makeText(this, "오류: 이미지 로딩에 실패했습니다.", Toast.LENGTH_LONG).show()
            Log.e("AnalysisResult", "이미지 로딩 오류: ${e.message}", e)
            imageViewWoundPhoto.visibility = View.GONE
        }

        // --- 4. DB 인스턴스 준비 ---
        val dbHelper = DatabaseHelper(this)
        val db = dbHelper.readableDatabase

        // --- 5. UI에 표시할 변수들 초기화 ---
        var finalDiagnosis = "진단 정보 없음"
        var feedback = "정보 없음"
        var userSymptoms = "정보 없음" // "symptoms"에서 "userSymptoms"로 이름 변경
        var diseaseDescription = "설명 정보를 찾을 수 없습니다."
        var cnnResult = "AI 분석 결과 없음" // AIResultDB에서 cnn_result 가져오기 위해 추가

        // --- 6. DB에서 데이터 조회 시작 ---
        var cursor = db.query(
            DatabaseHelper.AIResultDBEntry.TABLE_NAME,
            arrayOf(
                DatabaseHelper.AIResultDBEntry.COLUMN_CNN_RESULT,
                DatabaseHelper.AIResultDBEntry.COLUMN_FINAL_DIAGNOSIS,
                DatabaseHelper.AIResultDBEntry.COLUMN_FEEDBACK
            ),
            "${DatabaseHelper.AIResultDBEntry.COLUMN_RECORD_ID} = ?",
            arrayOf(currentRecordId.toString()),
            null, null, null
        )

        if (cursor.moveToFirst()) {
            cnnResult = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.AIResultDBEntry.COLUMN_CNN_RESULT))
            finalDiagnosis = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.AIResultDBEntry.COLUMN_FINAL_DIAGNOSIS))
            feedback = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.AIResultDBEntry.COLUMN_FEEDBACK))
        }
        cursor.close()

        cursor = db.query(
            DatabaseHelper.HealthRecordDBEntry.TABLE_NAME,
            arrayOf(DatabaseHelper.HealthRecordDBEntry.COLUMN_SYMPTOMS,
                DatabaseHelper.HealthRecordDBEntry.COLUMN_PAIN_LEVEL,
                DatabaseHelper.HealthRecordDBEntry.COLUMN_TEMPERATURE),
            "${DatabaseHelper.HealthRecordDBEntry.COLUMN_RECORD_ID} = ?",
            arrayOf(currentRecordId.toString()),
            null, null, null
        )

        if (cursor.moveToFirst()) {
            userSymptoms = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.HealthRecordDBEntry.COLUMN_SYMPTOMS))
            val painLevel = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.HealthRecordDBEntry.COLUMN_PAIN_LEVEL))
            val temperature = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.HealthRecordDBEntry.COLUMN_TEMPERATURE))

            // "현재 증상 상태"는 입력된 증상, 고통 수준, 온도 등을 종합하여 표시
            // 여기에 복잡한 로직을 추가하여 심각도를 판단할 수 있습니다. (현재는 간단히 정보 표시)
            textViewSymptomStatus.text = "증상: $userSymptoms\n고통 수준: $painLevel\n체온: %.2f °C".format(temperature)
        }
        cursor.close()

        cursor = db.query(
            DatabaseHelper.DiseaseInfoDBEntry.TABLE_NAME,
            arrayOf(DatabaseHelper.DiseaseInfoDBEntry.COLUMN_DESCRIPTION),
            "${DatabaseHelper.DiseaseInfoDBEntry.COLUMN_DISEASE_NAME} = ?",
            arrayOf(finalDiagnosis), // AI가 내린 최종 진단명을 사용
            null, null, null
        )

        if (cursor.moveToFirst()) {
            diseaseDescription = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.DiseaseInfoDBEntry.COLUMN_DESCRIPTION))
        }
        cursor.close()

        db.close() // DB 작업 완료 후 닫기

        // --- 7. 조회한 모든 정보로 TextView 업데이트 ---
        analysisResultTitle.text = finalDiagnosis // AI가 예측한 최종 진단명
        textViewAnalysisResult.text = "AI 예측: $cnnResult" // AI 결과와
        textViewDiseaseDesc.text = diseaseDescription // CSV 설명 결합
        textViewSolution.text = feedback // AI 예측에 따른 피드백


        // --- 8. "확인" 버튼 클릭 시 동작 ---
        buttonConfirm.setOnClickListener {
            val nextIntent = Intent(this, OptionActivity::class.java) // Master 학생의 'OptionActivity'로 가정
            nextIntent.putExtra("USER_ID", currentUserId)
            nextIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(nextIntent)
            finish()
        }
    }
}