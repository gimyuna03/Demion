package com.iot.medion

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText // EditText 사용을 위해 이 줄을 추가합니다.
import android.widget.Toast // Toast 사용을 위해 이 줄을 추가합니다.
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class LoginActivity : AppCompatActivity() {
    // private val MASTER_ID = "master"
    // private val MASTER_PW = "1234"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // 1. 필요한 UI 요소들을 찾아옵니다.
        val buttonSignUp = findViewById<Button>(R.id.buttonSignUp) // 회원가입 버튼
        //XML의 "Login\n로그인" 버튼 ID에 맞춰 'buttonLogin' 변수를 연결합니다.
        val buttonLogin = findViewById<Button>(R.id.buttonLogin2)   // LoginActivity에서 로그인 기능을 담당할 버튼.
        // XML에 buttonLogin2 라는 ID가 있으니 이걸 사용합니다.
        //XML에 있는 실제 아이디 입력 EditText의 ID에 맞춰 변수를 연결합니다.
        val editTextId = findViewById<EditText>(R.id.editTextTextEmailAddress2)
        // XML에 있는 실제 비밀번호 입력 EditText의 ID에 맞춰 변수를 연결합니다.
        val editTextPassword = findViewById<EditText>(R.id.editTextTextPassword2)


        // --- 클릭 리스너 설정 ---
        // (기존 코드) 회원가입 버튼 클릭 시
        buttonSignUp.setOnClickListener {
            val intent = Intent(this@LoginActivity, SignupActivity::class.java)
            startActivity(intent)
        }

        //'로그인' 버튼 (이제 buttonLogin 변수가 XML의 buttonLogin2를 가리킵니다) 클릭 시 유효성 검사 로직
        buttonLogin.setOnClickListener { // buttonLogin 변수에 할당된 (ID: buttonLogin2) 버튼에 리스너를 붙입니다.
            val inputId = editTextId.text.toString().trim()
            val inputPw = editTextPassword.text.toString()

            // 1. 입력 필드가 비어있는지 먼저 확인
            if (inputId.isEmpty()) {
                Toast.makeText(this@LoginActivity, "아이디를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (inputPw.isEmpty()) {
                Toast.makeText(this@LoginActivity, "비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val dbHelper = DatabaseHelper(this)
            val db = dbHelper.readableDatabase // 읽기 가능한 DB

            // 입력된 비밀번호를 해싱
            val hashedInputPw = PasswordHasher.hash(inputPw)

            // 조회할 컬럼 정의 (user_id가 필요!)
            val projection = arrayOf(
                DatabaseHelper.UserDBEntry.COLUMN_USER_ID,
                DatabaseHelper.UserDBEntry.COLUMN_USERNAME
            )

            // WHERE 절 정의 (이메일과 해시된 비밀번호가 일치하는가)
            val selection = "${DatabaseHelper.UserDBEntry.COLUMN_USERNAME} = ? AND " +
                    "${DatabaseHelper.UserDBEntry.COLUMN_PASSWORD} = ?"
            val selectionArgs = arrayOf(inputId, hashedInputPw)

            // 쿼리 실행
            val cursor = db.query(
                DatabaseHelper.UserDBEntry.TABLE_NAME, // 테이블
                projection,                            // 가져올 컬럼
                selection,                             // WHERE 절
                selectionArgs,                         // WHERE 값
                null, null, null
            )

            // 쿼리 결과 확인
            var loginSuccess = false
            var loggedInUserId: Long = -1 // 로그인한 사용자의 ID를 저장할 변수

            if (cursor.moveToFirst()) { // 일치하는 사용자가 있다면
                loginSuccess = true
                // @SuppressLint("Range") // 경고 무시 (컬럼 이름으로 직접 가져오기)
                // 컬럼 인덱스를 가져와서 사용하는 것이 더 안전합니다.
                val userIdColumnIndex = cursor.getColumnIndex(DatabaseHelper.UserDBEntry.COLUMN_USER_ID)
                if (userIdColumnIndex != -1) {
                    loggedInUserId = cursor.getLong(userIdColumnIndex)
                }
            }
            cursor.close() // 커서 닫기
            db.close()     // DB 닫기

            // 아이디와 비밀번호가 일치하는지 확인
            if (loginSuccess && loggedInUserId != -1L) {
                Toast.makeText(this@LoginActivity, "로그인 성공!", Toast.LENGTH_SHORT).show()

                // 로그인 성공 후 자가진단 화면으로 이동
                val successIntent = Intent(this@LoginActivity, SelfDiagnosisActivity::class.java)
                successIntent.putExtra("USER_ID", loggedInUserId)

                startActivity(successIntent)
                finish() // 로그인 화면은 뒤로 가기 눌러도 다시 보이지 않도록 종료
            } else {
                Toast.makeText(this@LoginActivity, "아이디 또는 비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}