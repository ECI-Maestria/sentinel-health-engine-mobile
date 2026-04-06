package com.example.tesisv3

object PatientSession {
    @Volatile var patientId: String = "550e8400-e29b-41d4-a716-446655440000"
    @Volatile var currentUser: UserProfile? = null
    @Volatile var accessToken: String? = null
    @Volatile var refreshToken: String? = null
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
