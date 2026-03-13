package com.example.nfcreader

data class CardModel(
    val machineType: String?,
    val transaction: String,
    val time: String,
    val inStation: String?,
    val outStation:String?,
    val credit:Int,
)
