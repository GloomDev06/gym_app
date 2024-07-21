package com.fitness.myhealthfitness

data class ShakeRequestClass(
    val userId: String?,
    val username: String?,
    val email: String?,
    val mobile: String?,
    val available_credits: String?,
    val shakeCredits: String = "1",
    val timestamp: String?
)
