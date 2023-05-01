package com.example.aigrandchildren.model

class Message(var message: String?, var sentBy: String?) {

    companion object {
        var SENT_BY_ME = "me"
        var SENT_BY_BOT = "bot"
    }
} // 자바파일인데 코틀린으로 변경해서 문제 생길 수 있음