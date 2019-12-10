package com.example.demo.app.directory

import java.util.concurrent.atomic.AtomicBoolean

class Mutex(initialValue: Boolean) {

    private val mutex = AtomicBoolean(initialValue)

    fun runWithLock(f: () -> Unit) {
        while (!mutex.compareAndSet(true, false)) { Thread.sleep(5) }
        try {
            f.invoke()
        } finally {
            mutex.set(true)
        }
    }

}