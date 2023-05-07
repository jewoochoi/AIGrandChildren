# AIGrandChildren

import java.lang.management.ManagementFactory

fun main() {
    val runtimeMXBean = ManagementFactory.getRuntimeMXBean()
    
    val processName = runtimeMXBean.name
    
    if (processName.startsWith("com.google.android.youtube") || processName.startsWith("com.android.vending")) {
        println("현재 실행 중인 프로세스: $processName")
        println("유튜브 또는 플레이스토어가 실행 중입니다. 프로세스를 종료합니다.")
        val process = Runtime.getRuntime().exec("kill -9 ${runtimeMXBean.pid}")
        process.waitFor()
        println("프로세스가 종료되었습니다.")
    } else {
        println("현재 실행 중인 프로세스: $processName")
        println("유튜브 또는 플레이스토어가 실행 중이 아닙니다.")
    }
}
