package com.fitness.myhealthfitness
data class User(
    var userId: String? = null,
    var email: String? = null,
    var mobile: String? = null,
    var password: String? = null,
    var username: String? = null,
    var profile_image: String? = "",
    var total_purchase: String? = "",
    var available_credits: String? = "",
    var credits_spend: String? = "",
    var date_of_birth: String? = ""
)