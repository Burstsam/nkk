package org.mosad.teapod.util

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.*
import java.io.File
import java.lang.Exception

/**
 * This controller contains the logic for permanently saved data.
 * On load, it loads the saved files into the variables
 */
object StorageController {

    private const val fileNameMyList = "my_list.json"

    val myList = ArrayList<Int>() // a list of saved mediaIds

    fun load(context: Context) {
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

        return GlobalScope.launch(Dispatchers.IO) {
            file.writeText(Gson().toJson(myList.distinct()))
        }
    }

}