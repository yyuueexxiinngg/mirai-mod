package net.mamoe.mirai.message

import kotlinx.coroutines.Dispatchers
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.Voice
import net.mamoe.mirai.utils.*
import net.mamoe.mirai.utils.internal.DeferredReusableInput
import net.mamoe.mirai.utils.internal.asReusableInput
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URL
import kotlin.jvm.Throws


@Throws(OverFileSizeMaxException::class)
suspend inline fun <C : Contact> C.sendPtt(pttUrl: URL): MessageReceipt<C> = pttUrl.sendAsPttTo(this)

@Throws(OverFileSizeMaxException::class)
suspend inline fun <C : Contact> C.sendPtt(pttData: ByteArray): MessageReceipt<C> = pttData.sendAsPttTo(this)

@Throws(OverFileSizeMaxException::class)
suspend inline fun <C : Contact> C.sendPtt(pttFile: File): MessageReceipt<C> = pttFile.sendAsPttTo(this)

@Throws(OverFileSizeMaxException::class)
suspend inline fun Contact.uploadPtt(pttUrl: URL): Voice = pttUrl.uploadAsPtt(this)

@Throws(OverFileSizeMaxException::class)
suspend inline fun Contact.uploadPtt(pttData: ByteArray): Voice = pttData.uploadAsPtt(this)

@Throws(OverFileSizeMaxException::class)
suspend inline fun Contact.uploadPtt(pttFile: File): Voice = pttFile.uploadAsPtt(this)

@Throws(OverFileSizeMaxException::class)
suspend fun <C : Contact> URL.sendAsPttTo(contact: C): MessageReceipt<C> =
    toExternalPtt().sendTo(contact)

@Throws(OverFileSizeMaxException::class)
suspend fun <C : Contact> ByteArray.sendAsPttTo(contact: C): MessageReceipt<C> =
    toExternalPtt().sendTo(contact)

@Throws(OverFileSizeMaxException::class)
suspend fun <C : Contact> File.sendAsPttTo(contact: C): MessageReceipt<C> {
    require(this.exists() && this.canRead())
    return toExternalPtt().sendTo(contact)
}

@Throws(OverFileSizeMaxException::class)
suspend fun <C : Contact> URL.uploadAsPtt(contact: C): Voice =
    toExternalPtt().upload(contact)

@Throws(OverFileSizeMaxException::class)
suspend fun <C : Contact> ByteArray.uploadAsPtt(contact: C): Voice =
    toExternalPtt().upload(contact)

@Throws(OverFileSizeMaxException::class)
suspend fun <C : Contact> File.uploadAsPtt(contact: C): Voice =
    toExternalPtt().upload(contact)

fun URL.toExternalPtt(): ExternalPtt {
    val out = ByteArrayOutputStream()
    this.openConnection().getInputStream().use { it.copyTo(out) }
    return ExternalPtt(out.toByteArray())
}

fun ByteArray.toExternalPtt(): ExternalPtt = ExternalPtt(this)

fun File.toExternalPtt(): ExternalPtt {
    require(this.isFile) { "File must be a file" }
    require(this.exists()) { "File must exist" }
    require(this.canRead()) { "File must can be read" }
    return ExternalPtt(this.readBytes())
}

