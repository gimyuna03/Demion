package com.iot.medion

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.util.Log
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.random.Random
import android.widget.Toast
import android.content.Intent
import android.widget.ImageView
import android.net.Uri
import android.widget.RadioGroup
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.RadioButton

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import android.content.ContentValues // ContentValues import 추가

class SelfDiagnosisActivity : AppCompatActivity() {

    // ==== UI 요소들 선언 ====
    private lateinit var textView6: TextView
    private lateinit var selfDiagnosisTitle: TextView
    private lateinit var layoutPhotoSelection: LinearLayout
    private lateinit var buttonSelectPhoto: Button
    private lateinit var imageViewPhotoPreview: ImageView
    private lateinit var labelSymptoms: TextView
    private lateinit var editTextSymptoms: EditText
    private lateinit var labelPainLevel: TextView
    private lateinit var radioGroupPainLevel: RadioGroup
    private lateinit var buttonMeasureTemperature: Button
    private lateinit var textViewTemperatureValue: TextView
    private lateinit var buttonSubmitDiagnosis: Button
    private lateinit var loadingLayout: LinearLayout
    private lateinit var loadingTextView: TextView
    private lateinit var loadingProgressBar: ProgressBar

    // ==== 데이터 및 상태 변수들 ====
    private var currentUserId: Long = -1
    private var currentTemperature: Double = 0.0
    private var currentImagePath: String? = null // AI 분석을 위해 선택된 이미지 URI (String)
    private var predictedLabelFromAI: String = "분류되지 않음" // AI 예측 라벨
    private var confidenceFromAI: Float = 0.0f        // AI 예측 신뢰도

    // ==== ActivityResultLauncher 선언 ====
    private lateinit var pickImageLauncher: ActivityResultLauncher<String>
    // PredictionActivity로부터 결과(AI 예측 라벨 및 신뢰도)를 받아오기 위한 Launcher
    private lateinit var predictionResultLauncher: ActivityResultLauncher<Intent>

    private val REQUEST_BLUETOOTH_PERMISSIONS = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_self_diagnosis)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        checkBluetoothPermissions()

        currentUserId = intent.getLongExtra("USER_ID", -1L)
        if (currentUserId == -1L) {
            Toast.makeText(this, "오류: 사용자 정보를 불러올 수 없습니다.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // ==== UI 요소들을 XML의 ID에 연결 ====
        textView6 = findViewById(R.id.textView6)
        selfDiagnosisTitle = findViewById(R.id.selfDiagnosisTitle)
        layoutPhotoSelection = findViewById(R.id.layoutPhotoSelection)
        buttonSelectPhoto = findViewById(R.id.buttonSelectPhoto)
        imageViewPhotoPreview = findViewById(R.id.imageViewPhotoPreview)
        labelSymptoms = findViewById(R.id.labelSymptoms)
        editTextSymptoms = findViewById(R.id.editTextSymptoms)
        labelPainLevel = findViewById(R.id.labelPainLevel)
        radioGroupPainLevel = findViewById(R.id.radioGroupPainLevel)
        buttonMeasureTemperature = findViewById(R.id.buttonMeasureTemperature)
        textViewTemperatureValue = findViewById(R.id.textViewTemperatureValue)
        buttonSubmitDiagnosis = findViewById(R.id.buttonSubmitDiagnosis)
        loadingLayout = findViewById(R.id.loadingLayout)
        loadingTextView = loadingLayout.findViewById(R.id.loading_text_view)
        loadingProgressBar = loadingLayout.findViewById(R.id.progressBar)


        // --- 갤러리 이미지 선택 ActivityResultLauncher 초기화 ---
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                Toast.makeText(this, "사진이 성공적으로 선택되었습니다! AI 분석을 시작합니다.", Toast.LENGTH_LONG).show()
                currentImagePath = uri.toString() // 선택된 이미지 URI 저장
                imageViewPhotoPreview.setImageURI(uri) // 미리보기 ImageView에 표시
                imageViewPhotoPreview.visibility = View.VISIBLE // 미리보기 보이게 함

                // ==== AI 분석 시작: PredictionActivity 호출! ====
                // PredictionActivity는 결과를 SelfDiagnosisActivity로 돌려줄 것입니다.
                val intent = Intent(this, PredictionActivity::class.java).apply {
                    putExtra("image_uri", uri.toString()) // 이미지 URI를 String 형태로 PredictionActivity에 전달
                }
                predictionResultLauncher.launch(intent) // 새로운 Launcher로 PredictionActivity 실행

            } else {
                Toast.makeText(this, "사진 선택을 취소했습니다.", Toast.LENGTH_SHORT).show()
                imageViewPhotoPreview.visibility = View.GONE
                currentImagePath = null // 이미지 경로 초기화
                predictedLabelFromAI = "분류되지 않음" // AI 결과 초기화
                confidenceFromAI = 0.0f
            }
        }

        // --- PredictionActivity로부터 AI 분석 결과를 받기 위한 ActivityResultLauncher 초기화 ---
        predictionResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data: Intent? = result.data
                predictedLabelFromAI = data?.getStringExtra("predicted_label") ?: "분류 실패"
                confidenceFromAI = data?.getFloatExtra("confidence", 0.0f) ?: 0.0f

                Toast.makeText(this, "AI 분석 완료! ${predictedLabelFromAI} (확률: ${(confidenceFromAI * 100).toInt()}%)", Toast.LENGTH_LONG).show()
                Log.d("SelfDiagnosis", "AI 분석 결과 수신: $predictedLabelFromAI, $confidenceFromAI")

                // (선택 사항) 여기에 AI 분석 결과를 사용자에게 임시로 보여줄 수도 있습니다.
                // 예를 들어, 다이얼로그나 Toast 메시지 등.
                // imageViewPhotoPreview 아래에 작은 TextView를 추가하여 결과값을 표시할 수도 있습니다.

            } else if (result.resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "AI 분석이 취소되거나 실패했습니다.", Toast.LENGTH_LONG).show()
                predictedLabelFromAI = "분류 실패"
                confidenceFromAI = 0.0f
                currentImagePath = null // 이미지 처리 실패 시 이미지 경로 초기화
                imageViewPhotoPreview.visibility = View.GONE
            }
        }

        // ==== "사진 선택" 버튼 클릭 리스너 설정 ====
        buttonSelectPhoto.setOnClickListener {
            pickImageLauncher.launch("image/*") // 갤러리 열기
        }

        // ==== "온도 측정" 버튼 클릭 리스너 설정 ====
        buttonMeasureTemperature.setOnClickListener {
            if (checkBluetoothPermissionsAreGranted()) {
                Toast.makeText(this, "블루투스 기기 연결 중...", Toast.LENGTH_SHORT).show()
                // 블루투스 연결 및 측정 로직 (현재는 가상으로 3초 후 결과 반환)
                Handler(Looper.getMainLooper()).postDelayed({
                    currentTemperature = Random.nextDouble(36.0, 39.0)
                    textViewTemperatureValue.text = "현재 온도: %.2f °C".format(currentTemperature)
                    Toast.makeText(this, "온도 측정 완료!", Toast.LENGTH_SHORT).show()
                    Log.d("SelfDiagnosis", "온도 측정 완료: $currentTemperature °C")
                }, 3000)
            } else {
                Toast.makeText(this, "블루투스 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
                checkBluetoothPermissions() // 권한 요청
            }
        }

        // ==== "기록 제출" 버튼 클릭 리스너 설정 ====
        buttonSubmitDiagnosis.setOnClickListener {
            val selectedPainLevelId = radioGroupPainLevel.checkedRadioButtonId
            val painLevel = if (selectedPainLevelId != -1) {
                findViewById<RadioButton>(selectedPainLevelId).text.toString().toInt()
            } else {
                -1
            }
            val symptoms = editTextSymptoms.text.toString().trim() // 공백 제거

            // 유효성 검사 (AI 분석 결과도 검사 조건에 추가)
            if (currentImagePath == null || currentImagePath!!.isBlank()) {
                Toast.makeText(this, "상처 이미지를 선택하고 AI 분석을 완료해주세요.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (predictedLabelFromAI == "분류 실패" || confidenceFromAI == 0.0f) {
                Toast.makeText(this, "AI 분석 결과가 유효하지 않습니다. 다시 시도해주세요.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (painLevel == -1) {
                Toast.makeText(this, "고통 수준을 선택해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (symptoms.isBlank()) {
                Toast.makeText(this, "증상을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (currentTemperature == 0.0) { // 온도가 측정되지 않았다면 (가정)
                Toast.makeText(this, "체온을 측정해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 모든 유효성 검사를 통과하면 로딩 레이아웃 표시
            loadingLayout.visibility = View.VISIBLE
            loadingTextView.text = "기록을 제출하는 중..."

            // ==== 실제 데이터베이스 저장 로직 호출 ====
            Handler(Looper.getMainLooper()).postDelayed({
                val recordId = saveHealthRecord(currentUserId, currentImagePath!!, symptoms, painLevel, currentTemperature, predictedLabelFromAI, confidenceFromAI)

                loadingLayout.visibility = View.GONE

                if (recordId != -1L) {
                    Toast.makeText(this, "기록이 성공적으로 제출되었습니다!", Toast.LENGTH_SHORT).show()
                    Log.d("SelfDiagnosis", "기록 제출 성공. Record ID: $recordId")

                    // AnalysisResultActivity로 이동
                    val intent = Intent(this, AnalysisResultActivity::class.java).apply {
                        putExtra("RECORD_ID", recordId)
                        putExtra("USER_ID", currentUserId)
                        // AnalysisResultActivity가 직접 DB에서 image_path를 조회하도록 합니다.
                    }
                    startActivity(intent)
                    finish() // 현재 Activity 종료

                } else {
                    Toast.makeText(this, "기록 제출에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_LONG).show()
                    Log.e("SelfDiagnosis", "기록 제출 실패.")
                }
            }, 3000) // 가상으로 3초 지연 후 DB 저장 및 이동
        }
    } // --- onCreate 끝 ---

    // ==== 블루투스 권한 확인 및 요청 함수들 (이전 코드와 동일) ====
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            if (checkBluetoothPermissionsAreGranted()) {
                Toast.makeText(this, "블루투스 권한이 모두 허용되었습니다.", Toast.LENGTH_SHORT).show()
                startBluetoothOperations()
            } else {
                Toast.makeText(this, "블루투스 권한이 거부되어 기능을 사용할 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkBluetoothPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // API 31 (Android 12) 이상
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN )
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            // ACCESS_FINE_LOCATION은 BLUETOOTH_SCAN에 필요한 위치 권한이므로 포함.
            // 이미 매니페스트에 ACCESS_FINE_LOCATION이 있다면 추가적으로 BLUETOOTH_SCAN 시에도 요구될 수 있음.
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        } else { // Android 11 이하 (API 30 이하)
            // BLUETOOTH와 BLUETOOTH_ADMIN은 일반 앱에서는 런타임 권한이 필요하지 않지만,
            // 매니페스트에 선언해야 하며 시스템 수준 동작을 위해 requestPermissions가 필요할 수 있음.
            // 위치 권한은 Bluetooth LE 스캔을 위해 필요함.
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            // 이전 버전의 Android에서는 BLUETOOTH 및 BLUETOOTH_ADMIN은 설치 시 부여되는 일반 권한이었습니다.
            // 따라서 런타임에서 requestPermissions를 명시적으로 호출할 필요는 없습니다.
            // 하지만 예외적으로 특정 기기/ROM에서 필요한 경우가 있을 수 있으므로 포함해둠.
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_ADMIN)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                REQUEST_BLUETOOTH_PERMISSIONS
            )
        } else {
            // 모든 블루투스 권한이 이미 허용된 경우
            startBluetoothOperations()
        }
    }


    private fun checkBluetoothPermissionsAreGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else { // Android 11 이하 (API 30 이하)
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
        }
    }


    private fun startBluetoothOperations() {
        Toast.makeText(this, "블루투스 기능 준비 완료!", Toast.LENGTH_SHORT).show()
    }


    /**
     * 사용자가 입력한 건강 기록과 AI 분석 결과를 DB에 저장하는 함수
     * @return 새로 저장된 record_id (실패 시 -1L)
     */
    private fun saveHealthRecord(
        userId: Long,
        imagePath: String,
        symptoms: String,
        painLevel: Int,
        temperature: Double,
        aiPredictedLabel: String,
        aiConfidence: Float
    ): Long {
        val dbHelper = DatabaseHelper(this)
        val db = dbHelper.writableDatabase

        // 1. HealthRecordDB에 기본 정보 저장
        val healthRecordValues = ContentValues().apply {
            put(DatabaseHelper.HealthRecordDBEntry.COLUMN_USER_ID, userId)
            put(DatabaseHelper.HealthRecordDBEntry.COLUMN_IMAGE_PATH, imagePath)
            put(DatabaseHelper.HealthRecordDBEntry.COLUMN_SYMPTOMS, symptoms)
            put(DatabaseHelper.HealthRecordDBEntry.COLUMN_PAIN_LEVEL, painLevel)
            put(DatabaseHelper.HealthRecordDBEntry.COLUMN_TEMPERATURE, temperature)
            // COLUMN_CREATED_AT은 테이블 스키마에서 DEFAULT CURRENT_TIMESTAMP로 자동 처리
        }

        var newHealthRecordId: Long = -1L
        db.beginTransaction() // 트랜잭션 시작
        try {
            newHealthRecordId = db.insert(DatabaseHelper.HealthRecordDBEntry.TABLE_NAME, null, healthRecordValues)

            if (newHealthRecordId != -1L) {
                // 2. AIResultDB에 AI 분석 결과 저장 (newHealthRecordId를 참조)
                val aiResultValues = ContentValues().apply {
                    put(DatabaseHelper.AIResultDBEntry.COLUMN_RECORD_ID, newHealthRecordId)
                    // AI 모델의 출력인 predictedLabelFromAI와 confidenceFromAI를 저장
                    // cnn_result는 (예: "Acne (78%)"와 같이 표시)
                    put(DatabaseHelper.AIResultDBEntry.COLUMN_CNN_RESULT, "$aiPredictedLabel (${(aiConfidence * 100).toInt()}%)")
                    put(DatabaseHelper.AIResultDBEntry.COLUMN_DISEASE_CANDIDATES, "AI 분석 결과 기반 후보 (필요시 상세 JSON)") // 필요시 더 구체적인 JSON
                    put(DatabaseHelper.AIResultDBEntry.COLUMN_FINAL_DIAGNOSIS, aiPredictedLabel) // AI가 예측한 최종 질병명
                    put(DatabaseHelper.AIResultDBEntry.COLUMN_FEEDBACK, getFeedbackForDisease(aiPredictedLabel)) // 예측된 질병에 따른 피드백
                }
                val newAiResultId = db.insert(DatabaseHelper.AIResultDBEntry.TABLE_NAME, null, aiResultValues)

                if (newAiResultId == -1L) {
                    throw Exception("AI 결과 DB 저장 실패")
                }
                db.setTransactionSuccessful() // 모든 작업 성공
                Log.d("SelfDiagnosis", "HealthRecord 및 AIResult DB 저장 성공. Record ID: $newHealthRecordId")
            } else {
                throw Exception("HealthRecord DB 저장 실패")
            }
        } catch (e: Exception) {
            Log.e("SelfDiagnosis", "DB 저장 중 오류 발생: ${e.message}", e)
            newHealthRecordId = -1L // 실패 시 -1 반환
        } finally {
            db.endTransaction() // 트랜잭션 종료
            db.close()
        }
        return newHealthRecordId
    }

    // AI 예측 라벨에 따라 적절한 피드백 메시지를 반환하는 헬퍼 함수
    // 실제로는 더 복잡한 로직(예: DB에서 조회)이 필요할 수 있습니다.
    private fun getFeedbackForDisease(diseaseLabel: String): String {
        return when (diseaseLabel) {
            "Abrasions" -> "가벼운 긁힘이므로 소독 후 밴드를 붙여주세요. 깊은 상처라면 병원 방문이 필요합니다."
            "Bruises" -> "냉찜질로 붓기를 가라앉히고, 통증이 심하면 진통제를 복용하세요."
            "Burns" -> "즉시 흐르는 물에 식히고, 물집은 터뜨리지 마세요. 범위가 넓거나 깊다면 즉시 병원으로 가세요."
            "Cut" -> "출혈이 멈추지 않거나 상처가 깊다면 병원에 방문하여 봉합이 필요한지 확인하세요."
            "Laceration" -> "상처 부위를 깨끗이 소독하고 거즈로 덮은 후, 병원에서 적절한 치료를 받으세요."
            else -> "전문가의 진단을 권장합니다."
        }
    }

} // --- SelfDiagnosisActivity 클래스 끝 ---