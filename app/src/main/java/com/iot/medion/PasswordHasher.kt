package com.iot.medion

import java.security.MessageDigest

object PasswordHasher {
    fun hash(password: String): String {
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(password.toByteArray(Charsets.UTF_8))
            // 바이트 배열을 16진수 문자열로 변환
            return hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            // 실제 앱에서는 예외 처리를 더 견고하게 해야 합니다.
            e.printStackTrace()
            return password // 오류 발생 시 (임시방편)
        }
    }
}