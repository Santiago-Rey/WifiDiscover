package com.example.wifidiscover

class WifiFrame {
    var sendMessage: String = "Mensaje"
    var dateSend: String = "Fecha"
    var dateReceived : String = "FechaRecibida"

    fun getMessage() = "\n - Mensaje: ${sendMessage} " +
            "\n Hora de envio: ${dateSend}" +
            "\n Hora de llegada: ${dateReceived}"

}