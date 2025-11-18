package com.iot.medion

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import java.io.BufferedReader
import java.io.InputStreamReader

class DatabaseHelper(private val context: Context) :
        SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    object UserDBEntry : BaseColumns {
        const val TABLE_NAME = "UserDB"
        const val COLUMN_USER_ID = "user_id" // (INTEGER, PK)
        const val COLUMN_USERNAME = "username" // (TEXT)
        const val COLUMN_PASSWORD = "password" // (TEXT)
        const val COLUMN_AGE = "age" // (INTEGER)
        const val COLUMN_GENDER = "gender" // (TEXT)
        const val COLUMN_CREATED_AT = "created_at" // (DATETIME)
    }

    object HealthRecordDBEntry : BaseColumns {
        const val TABLE_NAME = "HealthRecordDB"
        const val COLUMN_RECORD_ID = "record_id" // (INTEGER, PK)
        const val COLUMN_USER_ID = "user_id" // (INTEGER, FK)
        const val COLUMN_IMAGE_PATH = "image_path" // (TEXT)
        const val COLUMN_SYMPTOM = "symptom" // (TEXT)
        const val COLUMN_PAIN_LEVEL = "pain_level" // (INTEGER)
        const val COLUMN_TEMPERATURE = "temperature" // (REAL)
        const val COLUMN_CREATED_AT = "created_at" // (DATETIME)
        const val COLUMN_SYMPTOMS = "symptoms"
        const val COLUMN_TIMESTAMP = "timestamp"
    }

    object AIResultDBEntry : BaseColumns {
        const val TABLE_NAME = "AIResultDB"
        const val COLUMN_RESULT_ID = "result_id" // (INTEGER, PK)
        const val COLUMN_RECORD_ID = "record_id" // (INTEGER, FK)
        const val COLUMN_CNN_RESULT = "cnn_result" // (TEXT)
        const val COLUMN_DISEASE_CANDIDATES = "disease_candidates" // (TEXT)
        const val COLUMN_FINAL_DIAGNOSIS = "final_diagnosis" // (TEXT)
        const val COLUMN_FEEDBACK = "feedback" // (TEXT)
    }

    object DiseaseInfoDBEntry : BaseColumns {
        const val TABLE_NAME = "DiseaseInfoDB"
        // 질병명을 고유 키(UNIQUE)로 설정하여 중복 저장을 방지합니다.
        const val COLUMN_DISEASE_NAME = "disease_name" // (TEXT, UNIQUE)
        const val COLUMN_DESCRIPTION = "description"   // (TEXT)
    }

    companion object {
        const val DATABASE_VERSION = 4
        const val DATABASE_NAME = "Medion.db"
        // --- 테이블 생성 SQL 문 ---

        // 1. UserDB 생성
        // (user_id는 BaseColumns의 _ID를 사용하지 않고 명세서대로 명시적 PK 사용)
        private const val SQL_CREATE_USERDB =
            "CREATE TABLE ${UserDBEntry.TABLE_NAME} (" +
                    "${UserDBEntry.COLUMN_USER_ID} INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "${UserDBEntry.COLUMN_USERNAME} TEXT NOT NULL, " +
                    "${UserDBEntry.COLUMN_PASSWORD} TEXT NOT NULL, " + // 실제로는 암호화된 값을 저장해야 함
                    "${UserDBEntry.COLUMN_AGE} INTEGER, " +
                    "${UserDBEntry.COLUMN_GENDER} TEXT, " +
                    "${UserDBEntry.COLUMN_CREATED_AT} TEXT DEFAULT CURRENT_TIMESTAMP)"

        // 2. HealthRecordDB 생성
        private const val SQL_CREATE_HEALTHRECORDDB =
            "CREATE TABLE ${HealthRecordDBEntry.TABLE_NAME} (" +
                    "${HealthRecordDBEntry.COLUMN_RECORD_ID} INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "${HealthRecordDBEntry.COLUMN_USER_ID} INTEGER NOT NULL, " +
                    "${HealthRecordDBEntry.COLUMN_IMAGE_PATH} TEXT, " +
                    "${HealthRecordDBEntry.COLUMN_SYMPTOMS} TEXT, " +
                    "${HealthRecordDBEntry.COLUMN_PAIN_LEVEL} INTEGER, " +
                    "${HealthRecordDBEntry.COLUMN_TEMPERATURE} REAL, " +
                    "${HealthRecordDBEntry.COLUMN_CREATED_AT} TEXT DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY(${HealthRecordDBEntry.COLUMN_USER_ID}) REFERENCES " +
                    "${UserDBEntry.TABLE_NAME}(${UserDBEntry.COLUMN_USER_ID}) ON DELETE CASCADE)" // UserDB가 삭제되면 관련 레코드도 삭제

        // 3. AIResultDB 생성
        // (설명서: "각 진단 기록은 하나의 AI 분석 결과와 연결된다" -> record_id에 UNIQUE 제약 추가)
        private const val SQL_CREATE_AIRESULTDB =
            "CREATE TABLE ${AIResultDBEntry.TABLE_NAME} (" +
                    "${AIResultDBEntry.COLUMN_RESULT_ID} INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "${AIResultDBEntry.COLUMN_RECORD_ID} INTEGER NOT NULL UNIQUE, " +
                    "${AIResultDBEntry.COLUMN_CNN_RESULT} TEXT, " +
                    "${AIResultDBEntry.COLUMN_DISEASE_CANDIDATES} TEXT, " +
                    "${AIResultDBEntry.COLUMN_FINAL_DIAGNOSIS} TEXT, " +
                    "${AIResultDBEntry.COLUMN_FEEDBACK} TEXT, " +
                    "FOREIGN KEY(${AIResultDBEntry.COLUMN_RECORD_ID}) REFERENCES " +
                    "${HealthRecordDBEntry.TABLE_NAME}(${HealthRecordDBEntry.COLUMN_RECORD_ID}) ON DELETE CASCADE)" // HealthRecordDB가 삭제되면 관련 레코드도 삭제

        // --- 4. DiseaseInfoDB 생성 SQL ---
        private const val SQL_CREATE_DISEASEINFODB =
            "CREATE TABLE ${DiseaseInfoDBEntry.TABLE_NAME} (" +
                    // BaseColumns의 _ID를 PK로 사용합니다.
                    "${BaseColumns._ID} INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "${DiseaseInfoDBEntry.COLUMN_DISEASE_NAME} TEXT NOT NULL UNIQUE, " +
                    "${DiseaseInfoDBEntry.COLUMN_DESCRIPTION} TEXT NOT NULL)"

    }
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("PRAGMA foreign_keys=ON;")

        // 테이블 생성
        db.execSQL(SQL_CREATE_USERDB)
        db.execSQL(SQL_CREATE_HEALTHRECORDDB)
        db.execSQL(SQL_CREATE_AIRESULTDB)
        db.execSQL(SQL_CREATE_DISEASEINFODB)

        // CSV 데이터를 신규 테이블로 사전 로드하는 함수 호출
        preloadDiseaseInfo(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS ${AIResultDBEntry.TABLE_NAME}")
        db.execSQL("DROP TABLE IF EXISTS ${HealthRecordDBEntry.TABLE_NAME}")
        db.execSQL("DROP TABLE IF EXISTS ${UserDBEntry.TABLE_NAME}")
        db.execSQL("DROP TABLE IF EXISTS ${DiseaseInfoDBEntry.TABLE_NAME}")

        // 테이블 재생성
        onCreate(db)
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        if (!db.isReadOnly) {
            db.execSQL("PRAGMA foreign_keys=ON;")
        }
    }

    // 파일을 읽어 DiseaseInfoDB에 저장하는 함수
    private fun preloadDiseaseInfo(db: SQLiteDatabase) {
        // 1. CSV 파일 열기
        val inputStream = context.resources.openRawResource(R.raw.symptom_description)
        val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8")) // UTF-8 인코딩 지정

        try {
            db.beginTransaction()

            // 첫 줄(헤더) 건너뛰기 (CSV 파일에 헤더가 있다면)
            reader.readLine()

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                // CSV 파일이 콤마(,)가 아닌 다른 문자로 구분되어 있다면
                // 아래의 split(",") 부분을 수정해야 합니다.
                val tokens = line!!.split(",")

                if (tokens.size >= 2) {
                    val diseaseName = tokens[0].trim()
                    val description = tokens[1].trim()

                    val values = ContentValues().apply {
                        put(DiseaseInfoDBEntry.COLUMN_DISEASE_NAME, diseaseName)
                        put(DiseaseInfoDBEntry.COLUMN_DESCRIPTION, description)
                    }

                    db.insertWithOnConflict(
                        DiseaseInfoDBEntry.TABLE_NAME,
                        null,
                        values,
                        SQLiteDatabase.CONFLICT_IGNORE // 중복된 이름(UNIQUE)은 무시
                    )
                }
            }
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.endTransaction()
            reader.close()
            inputStream.close()
        }
    }
}
