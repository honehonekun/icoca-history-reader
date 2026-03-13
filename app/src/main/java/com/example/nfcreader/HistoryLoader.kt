package com.example.nfcreader

import android.content.Context
import android.util.SparseArray
import kotlin.experimental.and


fun historyLoader(context: Context,data: ByteArray): List<CardModel> {
    val historyList: MutableList<CardModel> = mutableListOf()
    val deviceList: SparseArray<String?> = SparseArray<String?>()
    val actionList: SparseArray<String?> = SparseArray<String?>()

    StationResolver.initialize(context)


    deviceList.put(3, "精算機")
    deviceList.put(4, "携帯型端末")
    deviceList.put(5, "車載端末")
    deviceList.put(7, "券売機")
    deviceList.put(8, "券売機")
    deviceList.put(9, "入金機")
    deviceList.put(18, "券売機")
    deviceList.put(20, "券売機等")
    deviceList.put(21, "券売機等")
    deviceList.put(22, "改札機")
    deviceList.put(23, "簡易改札機")
    deviceList.put(24, "窓口端末")
    deviceList.put(25, "窓口端末")
    deviceList.put(26, "改札端末")
    deviceList.put(27, "携帯電話")
    deviceList.put(28, "乗継精算機")
    deviceList.put(29, "連絡改札機")
    deviceList.put(31, "簡易入金機")
    deviceList.put(70, "VIEW ALTTE")
    deviceList.put(72, "VIEW ALTTE")
    deviceList.put(199, "物販端末")
    deviceList.put(200, "自販機")

    actionList.put(1, "運賃")
    actionList.put(2, "チャージ")
    actionList.put(3, "券購(磁気券購入)")
    actionList.put(4, "精算")
    actionList.put(5, "精算 (入場精算)")
    actionList.put(6, "窓出 (改札窓口処理)")
    actionList.put(7, "新規 (新規発行)")
    actionList.put(8, "控除 (窓口控除)")
    actionList.put(13, "バス (PiTaPa系)")
    actionList.put(15, "バス (IruCa系)")
    actionList.put(17, "再発行処理")
    actionList.put(19, "支払 (新幹線利用)")
    actionList.put(20, "入場時オートチャージ")
    actionList.put(21, "出場時オートチャージ")
    actionList.put(31, "入金 (バスチャージ)")
    actionList.put(35, "券購 (バス路面電車企画券購入)")
    actionList.put(70, "支払")
    actionList.put(72, "特典 (特典チャージ)")
    actionList.put(73, "入金 (レジ入金)")
    actionList.put(74, "物販取消")
    actionList.put(75, "入物 (入場物販)")
    actionList.put(198, "物現 (現金併用物販)")
    actionList.put(203, "入物 (入場現金併用物販)")
    actionList.put(132, "精算 (他社精算)")
    actionList.put(133, "精算 (他社入場精算)")
    val sells = arrayOf(70, 73, 74, 75, 198, 203)
    val bus = arrayOf(13, 15, 31, 35 )

    for (i in 0..<20) {
        val machineTypeId = data[i * 16].toUByte().toInt()
        val transactionId = data[1 + 16 * i].toUByte().toInt()
        if (transactionId == 0) {
            break
        }
        val time = calcTime(data[4 + 16 * i],data[5 + 16 * i])
        val credit = ((data[11 + 16 * i].toUByte().toInt()) shl 8) or
                (data[10 + 16 * i].toUByte().toInt())

        val machineName: String? = deviceList.get(machineTypeId)
        val transactionName: String = actionList.get(transactionId) ?: ""

        if (transactionId in sells || transactionId in bus) {
            historyList.add(
                CardModel(
                    machineName,
                    transactionName,
                    time = time,
                    inStation = null,
                    outStation = null,
                    credit
                )
            )
        } else {
            val region = data[15 + 16 * i].toUByte()
            val inLineId = data[6 + 16 * i].toUByte()
            val inStationId = data[7 + 16 * i].toUByte()
            val outLineId = data[8 + 16 * i].toUByte()
            val outStationId = data[9 + 16 * i].toUByte()

            val inAreaId: Byte = when{
                inLineId <= 0x7f.toUByte() -> 0
                inLineId >= 0x80.toUByte() && region == 0.toUByte() -> 1
                else -> 2
            }

            val outAreaId: Byte = when{
                outLineId <= 0x7f.toUByte() -> 0
                outLineId >= 0x80.toUByte() && region == 0.toUByte() -> 1
                else -> 2
            }

            val fullInStationId = intArrayOf(inAreaId.toInt(),inLineId.toInt(),inStationId.toInt())
            val fullOutStationId = intArrayOf(outAreaId.toInt(),outLineId.toInt(),outStationId.toInt())

            val inStationName = StationResolver.getStationName(fullInStationId[0],fullInStationId[1],fullInStationId[2])
            val outStationName = StationResolver.getStationName(fullOutStationId[0],fullOutStationId[1],fullOutStationId[2])





            historyList.add(
                CardModel(
                    machineName,
                    transactionName,
                    time = time,
                    inStation = inStationName,
                    outStation = outStationName,
                    credit
                )
            )
        }
    }
    return historyList
}

fun calcTime(b1: Byte, b2: Byte,): String {
    val fullByte = ((b1.toUByte().toInt() shl 8) or b2.toUByte().toInt())
    val year: Int
    val month: Int
    val day: Int


    year = ((b1.toUByte().toInt() ushr 1).toUByte() and 0b01111111.toUByte()).toInt()
    month = ((fullByte ushr 5) and 0b00000001111)
    day = (b2 and 0b00011111.toByte()).toInt()



    return "${year+2000}/$month/$day"
}

