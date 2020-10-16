package org.mosad.teapod.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.*
import java.io.File

/**
 * This controller contains the logic for permanently saved data.
 * On load, it loads the saved files into the variables
 */
object StorageController {

    private const val fileNameMyList = "my_list.json"

    val myList = ArrayList<String>() // a list of saved links

    fun load(context: Context) {
        val file = File(context.filesDir, fileNameMyList)

        if (!file.exists()) runBlocking { saveMyList(context).join() }

        myList.clear()
        myList.addAll(
            GsonBuilder().create().fromJson(file.readText(), ArrayList<String>().javaClass)
        )
    }

    fun saveMyList(context: Context): Job {
        val file = File(context.filesDir, fileNameMyList)

        return GlobalScope.launch(Dispatchers.IO) {
            file.writeText(Gson().toJson(myList))
        }
    }

}