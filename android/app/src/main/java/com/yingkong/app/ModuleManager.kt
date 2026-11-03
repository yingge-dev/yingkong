package com.yingkong.app

import android.content.Context
import java.io.File
import org.json.JSONObject

data class ModuleState(
    val root: Boolean = false,
    val installed: Boolean = false,
    val pending: Boolean = false,
    val running: Boolean = false,
    val version: String? = null,
    val cpu: Float? = null,
    val temp: Float? = null,
    val battery: Float? = null,
    val power: Float? = null
)

object ModuleManager {
    private const val ID = "yingkong"
    private const val PATH = "/data/adb/modules/$ID"
    private const val UPDATE_PATH = "/data/adb/modules_update/$ID"

    fun state(): ModuleState {
        val out = RootShell.runRoot(
            """
            if [ "$(id -u)" = "0" ]; then echo ROOT=YES; fi
            if [ -f $PATH/module.prop ]; then echo STATE=INSTALLED; fi
            if [ -f $UPDATE_PATH/module.prop ]; then echo STATE=PENDING; fi
            if [ -f /data/adb/yingkong/pid ]; then if kill -0 $(cat /data/adb/yingkong/pid) 2>/dev/null; then echo STATE=RUNNING; fi; fi
            if [ -f $UPDATE_PATH/module.prop ]; then echo VERSION=$(grep '^version=' $UPDATE_PATH/module.prop | cut -d= -f2); elif [ -f $PATH/module.prop ]; then echo VERSION=$(grep '^version=' $PATH/module.prop | cut -d= -f2); fi
            if [ -f /data/adb/yingkong/status ]; then echo STATUS=$(cat /data/adb/yingkong/status); fi
            """.trimIndent(),
            10
        ) ?: return ModuleState()
        val lines = out.lineSequence().map { it.trim() }.toList()
        var cpu: Float? = null
        var temp: Float? = null
        var battery: Float? = null
        var power: Float? = null
        lines.firstOrNull { it.startsWith("STATUS=") }?.let { line ->
            try {
                val json = JSONObject(line.substringAfter("STATUS="))
                cpu = json.optDouble("cpu", Double.NaN).takeIf { !it.isNaN() }?.toFloat()
                temp = json.optDouble("temp", Double.NaN).takeIf { !it.isNaN() }?.toFloat()
                battery = json.optDouble("battery", Double.NaN).takeIf { !it.isNaN() }?.toFloat()
                power = json.optDouble("power", Double.NaN).takeIf { !it.isNaN() }?.toFloat()
            } catch (_: Exception) {
            }
        }
        return ModuleState(
            root = lines.contains("ROOT=YES"),
            installed = lines.contains("STATE=INSTALLED"),
            pending = lines.contains("STATE=PENDING"),
            running = lines.contains("STATE=RUNNING"),
            version = lines.firstOrNull { it.startsWith("VERSION=") }?.substringAfter('='),
            cpu = cpu,
            temp = temp,
            battery = battery,
            power = power
        )
    }

    fun install(context: Context): String {
        val zip = File(context.cacheDir, "yingkong-module.zip")
        context.assets.open("yingkong-module.zip").use { input ->
            zip.outputStream().use { output -> input.copyTo(output) }
        }
        val path = zip.absolutePath
        val cmd = """
            if [ -x /data/adb/ksu/bin/ksud ]; then
              /data/adb/ksu/bin/ksud module install '$path'
            elif command -v ksud >/dev/null 2>&1; then
              ksud module install '$path'
            elif command -v magisk >/dev/null 2>&1; then
              magisk --install-module '$path'
            else
              echo NO_INSTALLER
              exit 1
            fi
            chmod 755 $PATH/bin/yingkong-daemon $UPDATE_PATH/bin/yingkong-daemon 2>/dev/null
            if [ -f $PATH/service.sh ]; then sh $PATH/service.sh; fi
            if [ -f $UPDATE_PATH/service.sh ]; then sh $UPDATE_PATH/service.sh; fi
            echo INSTALL_DONE
        """.trimIndent()
        return RootShell.runRoot(cmd, 60) ?: "root 无响应"
    }

    fun uninstall(): String {
        val cmd = """
            if [ -f /data/adb/yingkong/pid ]; then kill -9 $(cat /data/adb/yingkong/pid) 2>/dev/null; fi
            rm -rf $PATH $UPDATE_PATH /data/adb/yingkong
            echo UNINSTALL_DONE
        """.trimIndent()
        return RootShell.runRoot(cmd, 20) ?: "root 无响应"
    }
}
