package net.mamoe.mirai.utils

import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.message.MessageReceipt
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.Voice
import net.mamoe.mirai.message.data.sendTo
import net.mamoe.mirai.message.data.toUHexString
import net.mamoe.mirai.utils.internal.md5
import kotlin.jvm.JvmField
import kotlin.jvm.JvmSynthetic


class ExternalPtt internal constructor(
    @JvmField
    internal val data: ByteArray
) {
    internal val md5: ByteArray get() = this.data.md5()
    val formatName: String
    val codec: Int

    override fun toString(): String {
        return "ExternalPtt($md5.$formatName)"
    }

    companion object {
        const val defaultFormatName = "mirai"
    }

    init {
        formatName = data.copyOfRange(0, 10).toUHexString("").detectFormatName()
        codec = when (formatName) {
            "amr" -> 0
            "silk" -> 1
            else -> 1
        }
//        require(formatName != "mirai") { "Currently only support amr or silk v3 upload" }
    }

    private fun String.detectFormatName(): String = when {
        startsWith("2321414D52") -> "amr"
        startsWith("02232153494C4B5F5633") -> "silk"
        else -> defaultFormatName
    }
}

@JvmSynthetic
suspend fun <C : Contact> ExternalPtt.sendTo(contact: C): MessageReceipt<C> = when (contact) {
    is Group -> contact.uploadPtt(this).sendTo(contact)
    else -> error("unreachable")
}


@JvmSynthetic
suspend fun ExternalPtt.upload(contact: Contact): Voice = when (contact) {
    is Group -> contact.uploadPtt(this)
    else -> error("unreachable")
}

@JvmSynthetic
suspend inline fun <C : Contact> C.sendPtt(ptt: ExternalPtt): MessageReceipt<C> = ptt.sendTo(this)
