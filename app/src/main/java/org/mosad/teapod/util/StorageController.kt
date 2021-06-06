package org.mosad.teapod.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.*
import java.io.File
import java.io.FileReader
import java.io.FileWriter

/**
 * This controller contains the logic for permanently saved data.
 * On load, it loads the saved files into the variables
 */
object StorageController {

    private const val fileNameMyList = "my_list.json"

    val myList = ArrayList<Int>() // a list of saved mediaIds

    fun load(context: Context) {
        loadMyList(context)
    }

    fun loadMyList(context: Context) {
        val file = File(context.filesDir, fileNameMyList)

        if (!file.exists()) runBlocking { saveMyList(context).join() }

        try {
            myList.clear()
            myList.addAll(JsonParser.parseString(file.readText()).asJsonArray.map { it.asInt }.distinct())
        } catch (ex: Exception) {
            myList.clear()
            Log.e(javaClass.name, "Parsing of My-List failed.")
        }
    }

    fun saveMyList(context: Context): Job {
        val file = File(context.filesDir, fileNameMyList)

        return CoroutineScope(Dispatchers.IO).launch {
            file.writeText(Gson().toJson(myList.distinct()))
        }
    }

    fun exportMyList(context: Context, uri: Uri) {
        try {
            context.contentResolver.openFileDescriptor(uri, "w")?.use {
                FileWriter(it.fileDescriptor).use { writer ->
                    writer.write(Gson().toJson(myList.distinct()))
                }
            }
        } catch (ex: Exception) {
            Log.e(javaClass.name, "Exporting my list failed.", ex)
        }
    }

    /**
     * import my list from a (previously exported) json file
     * @param context the current context
     * @param uri the uri of the selected file
     * @return 0 if import was successfull, else 1
     */
    fun importMyList(context: Context, uri: Uri): Int {
        try {
            val text = context.contentResolver.openFileDescriptor(uri, "r")?.use {
                FileReader(it.fileDescriptor).use { reader ->
                    reader.readText()
                }
            }

            myList.clear()
            myList.addAll(JsonParser.parseString(text).asJsonArray.map { it.asInt }.distinct())

            // after the list has been imported also save it
            saveMyList(context)
        } catch (ex: Exception) {
            myList.clear()
            Log.e(javaClass.name, "Importing my list failed.", ex)
            return 1
        }

        return 0
    }

}