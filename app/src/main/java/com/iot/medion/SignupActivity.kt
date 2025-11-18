package com.iot.medion

import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText // EditText 사용을 위해 이 줄을 추가합니다.
import android.widget.Toast   // Toast 사용을 위해 이 줄을 추가합니다.
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.lang.Exception

class SignupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_signup) // 회원가입 화면 레이아웃 연결
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // --- 여기서부터 UI 요소들을 찾아오고, 유효성 검사 로직을 추가합니다. ---

        // 1. 회원가입 제출 버튼
        val buttonSignupSubmit = findViewById<Button>(R.id.buttonSignUp2)

        // 2. 각 입력 필드 EditText들을 찾아옵니다.
        val editEmail = findViewById<EditText>(R.id.editTextTextEmailAddress4)
        val editPassword = findViewById<EditText>(R.id.editTextTextPassword5)
        val editPasswordConfirm = findViewById<EditText>(R.id.editTextTextPassword6)


        // --- '회원가입' 버튼 클릭 리스너에 유효성 검사 로직을 추가합니다. ---
        buttonSignupSubmit.setOnClickListener { // buttonSignupSubmit (R.id.buttonSignUp2) 클릭 시 실행
            val email = editEmail.text.toString().trim()
            val password = editPassword.text.toString()
            val passwordConfirm = editPasswordConfirm.text.toString()

            // 1. 각 필드가 비어있는지 확인
            if (email.isEmpty()) {
                Toast.makeText(this@SignupActivity, "이메일을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                Toast.makeText(this@SignupActivity, "비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (passwordConfirm.isEmpty()) {
                Toast.makeText(this@SignupActivity, "비밀번호 확인을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 2. 이메일 형식 유효성 검사 (간단한 패턴 사용)
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this@SignupActivity, "유효한 이메일 형식이 아닙니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 3. 비밀번호 길이 검사 (예: 8자 이상)
            if (password.length < 8) {
                Toast.makeText(this@SignupActivity, "비밀번호는 8자 이상이어야 합니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 4. 비밀번호와 비밀번호 확인 일치 여부 검사
            if (password != passwordConfirm) {
                Toast.makeText(this@SignupActivity, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // --- 모든 유효성 검사를 통과했을 때 ---

            //1. DB Instance 생성
            val dbHelper = DatabaseHelper(this)
            val db = dbHelper.writableDatabase

            // 2. 비밀번호 해싱
            val hashedPassword = PasswordHasher.hash(password)

            //3. DB에 넣을 데이터 준비
            val values = ContentValues().apply {
                put(DatabaseHelper.UserDBEntry.COLUMN_USERNAME, email)
                put(DatabaseHelper.UserDBEntry.COLUMN_PASSWORD, hashedPassword)
            }

            try {
                val newRowId = db.insert(DatabaseHelper.UserDBEntry.TABLE_NAME, null, values)

                if (newRowId == -1L) {
                    Toast.makeText(this@SignupActivity, "회원가입에 실패했습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@SignupActivity, "회원가입 성공! 로그인 해주세요.", Toast.LENGTH_LONG).show()

                    val intent = Intent(this@SignupActivity, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this@SignupActivity, "데이터베이스 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            } finally {
                db.close()
            }


            // 회원가입 성공 후 LoginActivity로 이동
//            val intent = Intent(this@SignupActivity, LoginActivity::class.java)
//            startActivity(intent)
//            finish() // 현재 SignupActivity는 종료합니다. (뒤로 가기 누르면 다시 안 보이도록)
        }
    }
}