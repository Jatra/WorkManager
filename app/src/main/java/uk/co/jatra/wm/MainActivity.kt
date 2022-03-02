package uk.co.jatra.wm

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.random.Random

const val UPLOAD_RESULT = "UPLOAD_RESULT"
const val MERGED_RESULT = "MERGED_RESULT"
const val FINAL_RESULT = "FINAL_RESULT"

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.doit).setOnClickListener {
            workAndShow()
        }

    }


    fun workAndShow() {
        val finalId = doWork()

        WorkManager.getInstance(this).getWorkInfoByIdLiveData(finalId)
            .observe(this, Observer { info ->
                if (info != null && info.state.isFinished) {
                    val myResult = info.outputData.getString(FINAL_RESULT)

                    Toast.makeText(this, myResult, Toast.LENGTH_LONG).show()
                }
            })
    }

    fun doWork(): UUID {
        val uploads: MutableList<OneTimeWorkRequest> = mutableListOf()
        repeat(10) { counter ->
            uploads.add(
                OneTimeWorkRequestBuilder<UploadWorker>()
                    //params, data etc
                    .build()
            )
        }

        val finalRequest = OneTimeWorkRequestBuilder<FinalWorker>()
            .setInputMerger(FinalMerger::class.java)
            .build()

        WorkManager.getInstance(this)
            .beginWith(uploads)
            .then(finalRequest)
            .enqueue()

        return finalRequest.id
    }
}



class FinalMerger : InputMerger() {
    override fun merge(inputs: MutableList<Data>): Data {
        val result: String = inputs.map { data ->
            data.getDouble(UPLOAD_RESULT, 0.0)
        }.joinToString(", ")

        //do something here, or

        return Data.Builder()
            .putString(MERGED_RESULT, result)
            .build()
    }

}

class UploadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            val halfSeconds = Random.nextLong(1, 6)
            delay(halfSeconds * 500)
            val nextDouble = Random.nextDouble()
            val data = Data.Builder().putDouble(UPLOAD_RESULT, nextDouble).build()
            return@withContext Result.success(data)
        }
    }
}

class FinalWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {

        val uploadResults: Data = inputData

        val resultString = uploadResults.getString(MERGED_RESULT)

        val finalResult = "FINAL RESULT: $resultString"

        val data = Data.Builder()
            .putString(FINAL_RESULT, finalResult)
            .build()

        return Result.success(data)
    }
}


