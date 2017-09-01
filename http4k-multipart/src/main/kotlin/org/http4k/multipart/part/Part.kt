package org.http4k.multipart.part

import java.io.Closeable
import java.io.File
import java.nio.charset.Charset
import java.nio.file.FileSystemException

sealed class Part(fieldName: String?, formField: Boolean, contentType: String?, fileName: String?, headers: Map<String, String>, val length: Int) : PartMetaData(fieldName, formField, contentType, fileName, headers), Closeable {

    abstract val bytes: ByteArray

    class DiskBacked(part: PartMetaData, internal val theFile: File) : Part(part.fieldName, part.isFormField, part.contentType, part.fileName, part.headers, theFile.length().toInt()) {

        override val bytes
            get() = throw IllegalStateException("Cannot get bytes from a DiskBacked Part. Check with isInMemory()")

        override fun close() {
            if (!theFile.delete()) {
                throw FileSystemException("Failed to delete file")
            }
        }
    }

    class InMemory(original: PartMetaData,
                   override val bytes: ByteArray /* not immutable*/,
                   internal val encoding: Charset) : Part(original.fieldName, original.isFormField, original.contentType, original.fileName, original.headers, bytes.size) {

        override fun close() {
            // do nothing
        }
    }

}