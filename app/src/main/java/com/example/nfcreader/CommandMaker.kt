package com.example.nfcreader

import java.io.ByteArrayOutputStream

fun commandMaker(idm:ByteArray,start:Int,number:Int): ByteArray{
    val tempCommand = ByteArrayOutputStream(100)
    tempCommand.write(0x0E + 0x02*number) //14+ 2*num = length
    tempCommand.write(0x06) //read without encryption
    tempCommand.write(idm)
    tempCommand.write(0x01) //service count
    tempCommand.write(0x0F) //service code low
    tempCommand.write(0x09) //service code high
    tempCommand.write(number) //block count
    for(i in start ..< number+start){
        tempCommand.write(0x80)
        tempCommand.write(i)
    }


    val command = tempCommand.toByteArray()

    return command
}