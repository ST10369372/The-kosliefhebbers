package com.example.kosliefhebbers.models

import java.io.Serializable

data class Recipe(
    val id: String = "",
    val title: String = "",
    val image: String = "",
    val summary: String? = null,
    val instructions: String? = null,          // new
    val ingredients: List<String> = emptyList() // new
) : Serializable
