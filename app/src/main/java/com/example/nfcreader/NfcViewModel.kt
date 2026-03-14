package com.example.nfcreader

import android.app.Activity
import android.app.Application
import android.app.PendingIntent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.NfcF
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class NfcViewModel(application: Application) : AndroidViewModel(application) {

    private val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(getApplication())


    var status by mutableStateOf("読み取り準備完了\nカードをタッチしてください")
        private set

    var history by mutableStateOf(listOf<CardModel>())
        private set

    fun enableForegroundDispatch(activity: Activity, pendingIntent: PendingIntent) {
        if (nfcAdapter != null && nfcAdapter.isEnabled) {
            val nfcIntentFilter = arrayOf(
                IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED),
                IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),
                IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
            )
            nfcAdapter.enableForegroundDispatch(activity, pendingIntent, nfcIntentFilter, null)
        } else {
            status = "このデバイスはNFCに対応していません"
        }
    }
    fun disableForegroundDispatch(activity: Activity) {
        nfcAdapter?.disableForegroundDispatch(activity)
    }


    //ReaderModeを使用
    fun enableReaderMode(activity: Activity) {
        val options = android.os.Bundle()
        // 応答待ち時間を短く設定（必要に応じて）
        // options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250)

        nfcAdapter?.enableReaderMode(
            activity,
            { tag ->
                // 別スレッドで処理されるため、直接processTagを呼ぶ
                processTag(tag)
            },
            NfcAdapter.FLAG_READER_NFC_F or
                    NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, // これが重要
            options
        )
    }
    private fun processTag(tag: Tag) {
        viewModelScope.launch {
            status = "読み取り中"
            status = readTagData(tag)
        }
    }


    fun disableReaderMode(activity: Activity) {
        nfcAdapter?.disableReaderMode(activity)
    }


    fun processIntent(intent: android.content.Intent) {
        val tag: android.nfc.Tag? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }
        tag?.let {
            // ここで読み取り処理を行う（例：IDを取得）
            viewModelScope.launch {
                status = "読み取り中"
                status = readTagData(it)
            }
        }
    }

    private suspend fun readTagData(tag: android.nfc.Tag): String {

        withContext(Dispatchers.IO) {
            try {
                val felicaCard = NfcF.get(tag) ?: return@withContext "このカードはフェリカではありません"
                felicaCard.connect()
                val idm = felicaCard.tag.id
                val systemCode = felicaCard.systemCode
                val isCommonSystem =
                    systemCode.contentEquals(byteArrayOf(0x00.toByte(), 0x03.toByte()))
                if (!isCommonSystem) {
                    return@withContext "このカードは交通系ICカードではありません"
                }

                //make response
                val command1 = commandMaker(idm, 0, 12)
                val command2 = commandMaker(idm, 12, 8)

                val response1WithUnnecessaryData = felicaCard.transceive(command1)
                val response2WithUnnecessaryData = felicaCard.transceive(command2)

                val response = ByteArray(16 * 20)

                System.arraycopy(response1WithUnnecessaryData, 13, response, 0, 16 * 12)
                System.arraycopy(response2WithUnnecessaryData, 13, response, 16 * 12, 16 * 8)

                history = historyLoader(getApplication(),response)
                felicaCard.close()

            } catch (e: Exception) {
                return@withContext "例外: ${e.message}\n"
            }

        }
        if (history.isNotEmpty()) {
            return "読み込み成功"
        }else{
            return "カードとの接続が切断されました\nもう一度やり直してください"
        }
    }
}