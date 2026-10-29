package com.example.lagallens.data.datasource.local.datastore

import android.content.Context
import android.util.AtomicFile
import com.example.lagallens.data.repository.FinanceSnapshot
import com.example.lagallens.data.repository.FinanceStorage
import com.google.gson.Gson
import com.google.gson.JsonParser
import java.io.File
import java.io.FileNotFoundException

/** No legacy fallback. AtomicFile restores the previous snapshot after interrupted writes. */
internal class AtomicFinanceStorage(private val directory: File) : FinanceStorage {
    constructor(context: Context) : this(File(context.applicationContext.filesDir, "demo_finance_v1"))
    private val gson = Gson()
    private fun file(userId: String) = AtomicFile(File(directory, AccountStorageName.forUser("finance", userId)))
    override fun read(userId: String): FinanceSnapshot? {
        val raw = try { file(userId).openRead().bufferedReader(Charsets.UTF_8).use { it.readText() } }
        catch (failure: FileNotFoundException) {
            val base = file(userId).baseFile
            if (base.exists() || File(base.path + ".bak").exists() || File(base.path + ".new").exists()) throw failure
            return null
        }
        return FinanceSnapshotCodec.decode(raw)
    }
    override fun write(userId: String, snapshot: FinanceSnapshot) {
        check(directory.isDirectory || directory.mkdirs()) { "Unable to create finance storage" }
        val target = file(userId)
        val stream = target.startWrite()
        try {
            val encoded = FinanceSnapshotCodec.encode(snapshot)
            FinanceSnapshotCodec.decode(encoded)
            stream.write(encoded.toByteArray(Charsets.UTF_8))
            stream.fd.sync()
            target.finishWrite(stream)
            check(read(userId) == snapshot) { "Finance commit could not be verified" }
        } catch (failure: Throwable) {
            target.failWrite(stream)
            throw failure
        }
    }
}
