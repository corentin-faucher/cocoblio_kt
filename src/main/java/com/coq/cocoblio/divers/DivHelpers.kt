package com.coq.cocoblio.divers

import com.coq.cocoblio.BuildConfig
import java.lang.ref.WeakReference
import kotlin.math.min

/** Affichage d'erreur. */
fun printerror(message: String) {//, functionName: String = #methodName())
    val e = Exception().stackTrace
    System.err.print("❌ Error: $message (")
    if(e.size > 1) for(index in 1..min(3, e.size)) {
        System.err.print(" ${e[index].methodName} in ${e[index].fileName} ")
    }
    System.err.println(")")
}

fun printwarning(message: String) {//, functionName: String = #methodName())
    val e = Exception().stackTrace
    System.err.print("⚠️ Warn.: $message (")
    if(e.size > 1) for(index in 1..min(3, e.size)) {
        System.err.print(" ${e[index].methodName} in ${e[index].fileName} ")
    }
    System.err.println(")")
}
fun printdebug(message: String) {//, functionName: String = #methodName())
    if (!BuildConfig.DEBUG) {
        return
    }
    val e = Exception().stackTrace
    System.err.print("❗️ D️ebug: $message (")
    if(e.size > 1) for(index in 1..min(3, e.size)) {
        System.err.print(" ${e[index].methodName} in ${e[index].fileName} ")
    }
    System.err.println(")")
}

fun <K, T> MutableMap<K, WeakReference<T>>.strip() {
    forEach { (k, v) ->
        if(v.get() == null) {
            this.remove(k)
        }
    }
}

fun <T> MutableList<WeakReference<T> >.strip() {
    this.removeIf { it.get() == null }
}

