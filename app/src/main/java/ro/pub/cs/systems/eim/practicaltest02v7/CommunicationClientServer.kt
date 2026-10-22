package ro.pub.cs.systems.eim.practicaltest02v7

import android.util.Log
import java.io.BufferedReader
import java.io.IOException
import java.io.PrintWriter
import java.net.Socket
import android.os.SystemClock

class CommunicationThread(
    private val serverThread: ServerThread,
    private val socket: Socket
) : Thread() {

    override fun run() {
        var bufferedReader: BufferedReader? = null
        var printWriter: PrintWriter? = null

        try {
            bufferedReader = Utilities.getReader(socket)
            printWriter = Utilities.getWriter(socket)

            if (bufferedReader == null || printWriter == null) {
                Log.e(TAG, "Communication Thread - BufferedReader / PrintWriter are null!")
                return
            }

            // comanda de la client
            val timerAction = bufferedReader.readLine()
            if (timerAction.isNullOrEmpty()) {
                Log.e(TAG, "Communication Thread - Error receiving parameters from client!")
                return
            }

            Log.i(TAG, "Communication Thread - Received: $timerAction")

            // Parsare comandă
            val cleanedAction = timerAction.trim()
            val actionData = cleanedAction.split(",")

            if (actionData.isEmpty()) {
                Log.e(TAG, "Communication Thread - Invalid command format!")
                return
            }

            val clientIP = socket.inetAddress.toString()
            val data = serverThread.getData()

            when (actionData[0]) {
                "set" -> handleSet(actionData, clientIP, printWriter)

                "poll" -> handlePoll(clientIP, data, printWriter)

                "reset" -> handleReset(clientIP, data, printWriter)

                else -> {
                    Log.e(TAG, "Invalid action: ${actionData[0]}")
                    printWriter.println("error: invalid command")
                }
            }

        } catch (e: IOException) {
            Log.e(TAG, "Communication Thread - An exception has occurred: ${e.message}", e)
        } finally {
            try {
                socket.close()
            } catch (e: IOException) {
                Log.e(TAG, "Communication Thread - Error closing socket: ${e.message}", e)
            }
        }
    }

    private fun handleSet(
        actionData: List<String>,
        clientIP: String,
        printWriter: PrintWriter
    ) {
        if (actionData.size != 3) {
            printWriter.println("error: invalid parameters")
            return
        }

        val minutes = actionData[1].trim().toIntOrNull()
        val seconds = actionData[2].trim().toIntOrNull()

        if (minutes == null || seconds == null) {
            printWriter.println("error: invalid time format")
            return
        }

        if (minutes !in 0..59 || seconds !in 0..59) {
            printWriter.println("error: invalid time range")
            return
        }

        val durationMillis =
            (minutes * 60L + seconds) * 1000L

        val startTimeMillis = SystemClock.elapsedRealtime()

        serverThread.setData(
            clientIP,
            TimerInformation(
                durationMillis = durationMillis,
                startTimeMillis = startTimeMillis
            )
        )

        Log.i(TAG, "Timer set for $clientIP: $minutes:$seconds")

        printWriter.println("ok")
    }

    private fun handlePoll(
        clientIP: String,
        data: Map<String, TimerInformation>,
        printWriter: PrintWriter
    ) {
        val timerInfo = data[clientIP]

        if (timerInfo == null) {
            Log.i(TAG, "No timer for $clientIP")
            printWriter.println("none")
            return
        }

        val elapsedMillis =
            SystemClock.elapsedRealtime() -
                    timerInfo.startTimeMillis

        val isExpired =
            elapsedMillis >= timerInfo.durationMillis

        if (isExpired) {
            printWriter.println("active")
            Log.i(TAG, "Timer expired for $clientIP")
        } else {
            printWriter.println("inactive")
            Log.i(TAG, "Timer not expired for $clientIP")
        }
    }

    private fun handleReset(clientIP: String, data: MutableMap<String, TimerInformation>,  printWriter: PrintWriter) {
        data.remove(clientIP)
        printWriter.println("reset done\n")
        printWriter.flush()
        Log.i(TAG, "Communication Thread - Reset timer for $clientIP")
    }

    companion object {
        private const val TAG = "PracticalTest02"
    }
}