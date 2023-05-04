package com.example.aigrandchildren

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.aigrandchildren.adapter.MessageAdapter
import com.example.aigrandchildren.model.Message
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {
    var recycler_view: RecyclerView = findViewById<RecyclerView>(R.id.recycler_view)
    var tv_welcome: TextView = findViewById<TextView>(R.id.tv_welcome)
    var et_msg: EditText = findViewById<EditText>(R.id.et_msg)
    var btn_send: ImageButton = findViewById<ImageButton>(R.id.btn_send)
    var btn_voice: ImageButton = findViewById<ImageButton>(R.id.btn_voice)
    var activityResultLauncher: ActivityResultLauncher<Intent>? = null
    var messageList: MutableList<Message>? = null
    var messageAdapter: MessageAdapter? = null
    var client: OkHttpClient? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        recycler_view.setHasFixedSize(true)
        val manager = LinearLayoutManager(this)
        manager.stackFromEnd = true
        recycler_view.layoutManager = manager
        messageList = ArrayList<Message>()
        messageAdapter = MessageAdapter(messageList as ArrayList<Message>)
        recycler_view.adapter = messageAdapter
        btn_send.setOnClickListener( View.OnClickListener // 챗봇에게 질문하려면 버튼을 눌러야 한다
            {
                val question = et_msg.text.toString().trim { it <= ' ' }
                addToChat(question, Message.SENT_BY_ME)
                et_msg.setText("")
                callAPI(question)
                tv_welcome.visibility = View.GONE
            })

        // startActivityForResult가 Deprecated되서 registerForActivityResult를 사용
        activityResultLauncher = registerForActivityResult<Intent, ActivityResult>(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                val resultData = data!!.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                et_msg.setText(resultData!![0])
            }
        }
        btn_voice.setOnClickListener(
            View.OnClickListener
            // 음성인식을 하려면 버튼을 눌러야 한다
            {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                intent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko_KR")
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "음성인식을 시작합니다.")
                activityResultLauncher!!.launch(intent)
            })
        client = OkHttpClient().newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun addToChat(message: String?, sentBy: String?) {
        runOnUiThread {
            messageList!!.add(Message(message, sentBy))
            messageAdapter!!.notifyDataSetChanged()
            recycler_view.smoothScrollToPosition(messageAdapter!!.itemCount)
        }
    }

    fun addResponse(response: String?) {
        messageList!!.removeAt(messageList!!.size - 1)
        addToChat(response, Message.SENT_BY_BOT)
    }
// 할일 - 밑에 수정해야 함, TTS 넣기, 이전 대화 저장 및 적용
    fun callAPI(question: String?) {
        //okhttp
        messageList!!.add(Message("...", Message.SENT_BY_BOT))
        val arr = JSONArray()
        val baseAi = JSONObject()
        val userMsg = JSONObject()
        try {
            //AI 속성설정
            baseAi.put("role", "user")
            baseAi.put("content", "You are a helpful and kind AI Assistant.")
            //유저 메세지
            userMsg.put("role", "user")
            userMsg.put("content", question)
            //array로 담아서 한번에 보낸다
            arr.put(baseAi)
            arr.put(userMsg)
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }
        val `object` = JSONObject()
        try {
            `object`.put("model", "gpt-3.5-turbo")
            `object`.put("messages", arr)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        val body: RequestBody = RequestBody.create(`object`.toString(), JSON)
        val request: Request = Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .header("Authorization", "Bearer " + MY_SECRET_KEY)
            .post(body)
            .build()
        client!!.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                addResponse("Failed to load response due to " + e.message)
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    var jsonObject: JSONObject? = null
                    try {
                        jsonObject = JSONObject(response.body!!.string())
                        val jsonArray = jsonObject.getJSONArray("choices")
                        val result =
                            jsonArray.getJSONObject(0).getJSONObject("message").getString("content")
                        addResponse(result.trim { it <= ' ' })
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                } else {
                    addResponse("Failed to load response due to " + response.body!!.string())
                }
            }
        })
    }

    companion object {
        val JSON: MediaType = get.get("application/json; charset=utf-8")
        private const val MY_SECRET_KEY = "{Your Secret Key}"
    }
}