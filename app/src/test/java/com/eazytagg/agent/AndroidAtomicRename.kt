package com.eazytagg.agent

import android.util.AtomicFile
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.RealObject
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Android rename replaces the destination. Windows File.renameTo does not;
 * emulate Android's filesystem semantics in the headless Windows test runner. */
@Implements(AtomicFile::class)
class AndroidAtomicRename {
    @RealObject lateinit var real: AtomicFile
    @Implementation fun finishWrite(stream: FileOutputStream?) {
        if (stream == null) return
        stream.fd.sync(); stream.close()
        Files.move(File(real.baseFile.path + ".new").toPath(), real.baseFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }
}
