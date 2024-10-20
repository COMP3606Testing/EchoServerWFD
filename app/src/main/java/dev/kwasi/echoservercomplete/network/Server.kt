package dev.kwasi.echoservercomplete.network

import android.util.Log
import com.google.gson.Gson
import dev.kwasi.echoservercomplete.models.ContentModel
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.Exception
import kotlin.concurrent.thread

/// The [Server] class has all the functionality that is responsible for the 'server' connection.
/// This is implemented using TCP. This Server class is intended to be run on the GO.

class Server(private val iFaceImpl:NetworkMessageInterface) {
    companion object {
        const val PORT: Int = 9999

    }

    private val svrSocket: ServerSocket = ServerSocket(PORT, 0, InetAddress.getByName("192.168.49.1"))
    private val clientMap: HashMap<String, Socket> = HashMap()

    init {
        thread{
            while(true){
                try{
                    val clientConnectionSocket = svrSocket.accept()
                    Log.e("SERVER", "The server has accepted a connection: ")
                    handleSocket(clientConnectionSocket)

                }catch (e: Exception){
                    Log.e("SERVER", "An error has occurred in the server!")
                    e.printStackTrace()
                }
            }
        }
    }


    private fun handleSocket(socket: Socket){
        socket.inetAddress.hostAddress?.let {ipAddress ->
            clientMap[ipAddress] = socket
            //clientMap[it] = socket
            // Update the clientMap for the lecturer's view
            iFaceImpl.onClientConnected(ipAddress) // Assuming you add a new method for this in the interface
            Log.e("SERVER", "A new connection has been detected!")
            thread {
                val clientReader = socket.inputStream.bufferedReader()
                //val clientWriter = socket.outputStream.bufferedWriter()
                var receivedJson: String?

                while(socket.isConnected){
                    try{
                        receivedJson = clientReader.readLine()
                        if (receivedJson!= null){
                            Log.e("SERVER", "Received a message from client $ipAddress")
                            val clientContent = Gson().fromJson(receivedJson, ContentModel::class.java)
                            Log.e("SERVER", "Received message from client $ipAddress: ${clientContent.message}")
                            //val reversedContent = ContentModel(clientContent.message.reversed(), "192.168.49.1")

                            //val reversedContentStr = Gson().toJson(reversedContent)
                            //clientWriter.write("$reversedContentStr\n")
                            //clientWriter.flush()

                            // To show the correct alignment of the items (on the server), I'd swap the IP that it came from the client
                            // This is some OP hax that gets the job done but is not the best way of getting it done.
                            //val tmpIp = clientContent.senderIp
                            //clientContent.senderIp = reversedContent.senderIp
                            //reversedContent.senderIp = tmpIp

                            iFaceImpl.onContent(clientContent)
                            //iFaceImpl.onContent(reversedContent)

                        }
                    } catch (e: Exception){
                        Log.e("SERVER", "An error has occurred with the client $ipAddress")
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun getClientMap(): Map<String, Socket> {
        return clientMap
    }

    fun sendMessageToStudent(content: ContentModel, studentIp: String) {
        val studentSocket = clientMap[studentIp]
        if (studentSocket != null) {
            val writer = studentSocket.outputStream.bufferedWriter()
            val contentAsStr = Gson().toJson(content)
            writer.write("$contentAsStr\n")
            writer.flush()
        } else {
            Log.e("SERVER", "Could not find the student with IP $studentIp")
        }
    }

    /*fun sendMessageToStudent(content: ContentModel, studentIp: String) {
        clientMap[studentIp]?.let { socket ->
            val writer = socket.outputStream.bufferedWriter()
            val contentAsStr = Gson().toJson(content)
            writer.write("$contentAsStr\n")
            writer.flush()
        }
    }*/

    fun close(){
        svrSocket.close()
        clientMap.clear()
    }

}