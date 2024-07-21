package com.fitness.myhealthfitness

data class ProductList(
    var productTitle: String,
    var productCredits: Int,
    var productPrice: String,
    var productDescription: String,
    var productImageURL: String?,
    val packageId: String?
)