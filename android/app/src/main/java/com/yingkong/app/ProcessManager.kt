package com.yingkong.app

data class ProcessItem(
    val pid: Int,
    val ppid: Int,
    val uid: Int,
    val rssKb: Long,
    val name: String
) {
    val isKernelThread: Boolean get() = name.startsWith("[")
    val isProtected: Boolean get() =
        isKernelThread ||
            name.startsWith("com.android.") ||
            name.startsWith("com.miui.") ||
            name.startsWith("com.xiaomi.") ||
            name.startsWith("com.google.android.gms") ||
            name.startsWith("com.google.android.gsf")
    val isKillable: Boolean get() = uid >= 10000 && !isProtected && name != "com.yingkong.app"
    val rssMb: Float get() = rssKb / 1024f
}

object ProcessManager {
    fun list(): List<ProcessItem> {
        val output = RootShell.runShell("ps -A -o PID,PPID,UID,RSS,NAME", 10) ?: return emptyList()
        return output.lineSequence()
            .drop(1)
            .mapNotNull { line ->
                val parts = line.trim().split(Regex("\\s+"), limit = 5)
                if (parts.size < 5) return@mapNotNull null
                val pid = parts[0].toIntOrNull() ?: return@mapNotNull null
                val ppid = parts[1].toIntOrNull() ?: 0
                val uid = parts[2].toIntOrNull() ?: 0
                val rss = parts[3].toLongOrNull() ?: 0L
                val name = parts[4]
                if (pid <= 0 || name.isEmpty()) null else ProcessItem(pid, ppid, uid, rss, name)
            }
            .sortedByDescending { it.rssKb }
            .toList()
    }

    fun kill(item: ProcessItem): String {
        if (!item.isKillable) return "系统进程不可结束"
        if (item.pid == android.os.Process.myPid()) return "不能结束颖控自身"
        if (item.name == "com.yingkong.app") return "不能结束颖控自身"
        if (RootShell.isRootAvailable()) {
            return RootShell.runRoot("kill -9 ${item.pid}", 5)?.let { "已发送结束信号" } ?: "root 无响应"
        }
        val packageName = item.name.substringBefore(":")
        return RootShell.runShell("am force-stop $packageName", 8)?.let { "已强制停止" } ?: "Shizuku 无响应"
    }
}



