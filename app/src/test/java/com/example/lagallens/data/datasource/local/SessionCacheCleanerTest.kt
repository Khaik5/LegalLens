package com.example.lagallens.data.datasource.local

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionCacheCleanerTest {
    @Test fun keepsFilesWrittenAfterTheSessionChange() {
        val cache = Files.createTempDirectory("session-cache").toFile()
        val scans = File(cache, "scan-session").apply { mkdirs() }
        val pdfs = File(cache, "draft-pdfs/fixture").apply { mkdirs() }
        val oldScan = File(scans, "old.jpg").apply { writeText("previous user"); setLastModified(1_000) }
        val oldPdf = File(pdfs, "old.pdf").apply { writeText("previous user"); setLastModified(1_000) }
        val newScan = File(scans, "new.jpg").apply { writeText("next user"); setLastModified(5_000) }
        val other = File(cache, "other.txt").apply { writeText("not a session file"); setLastModified(1_000) }

        SessionCacheCleaner(cache).clear(createdBefore = 2_000)

        assertFalse(oldScan.exists())
        assertFalse(oldPdf.exists())
        assertFalse(pdfs.exists())
        assertTrue(newScan.exists())
        assertTrue(other.exists())
        cache.deleteRecursively()
    }
}
