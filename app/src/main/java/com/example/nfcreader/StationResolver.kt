package com.example.nfcreader

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

object StationResolver {

    private val stationMap  =mutableMapOf<String, String>()

    fun initialize(context: Context){
        if (stationMap.isNotEmpty()) return

        try {
            val assetManager = context.assets
            val inputStream = assetManager.open("StationCode.csv")
            val reader = BufferedReader(InputStreamReader(inputStream))

            //一行ずつ読む
            reader.useLines { lines ->
                //それぞれを中身の関数で処理
                lines.forEach { line->
                    val tokens = line.split(",")
                    if (tokens.size >= 6) {
                        val key = "${tokens[0]}-${tokens[1]}-${tokens[2]}"
                        stationMap[key] = tokens[5]
                    }
                }

            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
    fun getStationName(areaId:Int,lineId:Int,stationId:Int): String{
        val key = "$areaId-$lineId-$stationId"
        return stationMap[key] ?: "不明な駅($areaId-$lineId-$stationId)"
    }
}