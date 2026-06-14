package com.example.boxy

data class PackageModel(
    val id: String = "",
    val compartimento: String = "",
    val dataCriacao: String = "",
    val dataEntrega: String = "", // Atualizado pelo ESP32
    val descricao: String = "",
    val idUsuario: String = "",
    val qrCodeToken: String = "",
    val status: String = "pendente", // "pendente" ou "entregue"
    val fotoUrl: String = "" // Inserido pelo ESP32 após o upload do Storage
)
