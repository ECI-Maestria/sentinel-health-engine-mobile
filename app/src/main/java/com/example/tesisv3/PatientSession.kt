package com.example.tesisv3

object PatientSession {
    @Volatile var patientId: String = ""
    @Volatile var currentUser: UserProfile? = null
    @Volatile var accessToken: String? = null
    @Volatile var refreshToken: String? = null
    @Volatile var resetCode: String? = null
}

data class UserProfile(
    val id: String,
    val email: String,
    val role: String,
    val firstName: String?,
    val lastName: String?,
    val fullName: String?,
    val isActive: Boolean,
    val createdAt: String?
)
