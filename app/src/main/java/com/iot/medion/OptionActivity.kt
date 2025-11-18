package com.iot.medion

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.system.exitProcess

class OptionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_option)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // --- UI 요소들을 찾아옵니다. ---
        val buttonDiagnoseAgain = findViewById<Button>(R.id.buttonDiagnoseAgain)
        val buttonLogout = findViewById<Button>(R.id.buttonLogout)
        val buttonExitApp = findViewById<Button>(R.id.buttonExitApp)
        val currentUserId = intent.getLongExtra("USER_ID", -1L)

        // --- 클릭 리스너 설정 ---

        // 1. '다시 진단하기' 버튼
        buttonDiagnoseAgain.setOnClickListener {
            val intent = Intent(this@OptionActivity, SelfDiagnosisActivity::class.java)
            intent.putExtra("USER_ID", currentUserId)
            // 새로운 진단을 위해 SelfDiagnosisActivity를 새로 시작합니다.
            startActivity(intent)
            finish() // 현재 OptionActivity는 종료합니다.
        }

        // 2. '로그아웃' 버튼
        buttonLogout.setOnClickListener {
            // LoginActivity로 이동하면서, 현재 태스크의 모든 액티비티를 종료합니다.
            // 이렇게 하면 뒤로가기를 눌러도 다른 화면으로 돌아가지 않고 LoginActivity만 남게 됩니다.
            val intent = Intent(this@OptionActivity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            // finish()는 필요없습니다. FLAG_ACTIVITY_CLEAR_TASK가 현재 태스크를 비우기 때문입니다.
        }

        // 3. '앱 종료' 버튼
        buttonExitApp.setOnClickListener {
            finishAffinity() // 현재 액티비티와 상위 모든 액티비티를 종료합니다.
            // exitProcess(0) // 앱 프로세스를 완전히 종료합니다. (선택 사항이며, 보통 finishAffinity()로 충분합니다)
    }
}
}