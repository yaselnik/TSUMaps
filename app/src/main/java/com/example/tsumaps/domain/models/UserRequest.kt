package com.example.tsumaps.domain.models

import java.time.LocalTime

data class UserRequest(
    val choice: Set<String>,
    val currentTime: LocalTime = LocalTime.now(),
)