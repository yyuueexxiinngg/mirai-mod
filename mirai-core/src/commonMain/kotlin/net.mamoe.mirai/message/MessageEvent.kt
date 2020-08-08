/*
 * Copyright 2020 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/master/LICENSE
 */

@file:Suppress(
    "EXPERIMENTAL_UNSIGNED_LITERALS",
    "EXPERIMENTAL_API_USAGE",
    "unused",
    "DECLARATION_CANT_BE_INLINED", "UNCHECKED_CAST", "NOTHING_TO_INLINE"
)

@file:JvmMultifileClass
@file:JvmName("MessageEventKt")

package net.mamoe.mirai.message

import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.event.AbstractEvent
import net.mamoe.mirai.event.events.BotEvent
import net.mamoe.mirai.event.subscribe
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.qqandroid.network.Packet
import net.mamoe.mirai.utils.ExternalImage
import net.mamoe.mirai.utils.PlannedRemoval
import net.mamoe.mirai.utils.sendTo
import net.mamoe.mirai.utils.upload
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSynthetic

/**
 * 一个 (收到的) 消息事件.
 *
 * 它是一个 [BotEvent], 因此可以被 [监听][Bot.subscribe]
 *
 * 支持的消息类型:
 * - [群消息事件][GroupMessageEvent]
 * - [好友消息事件][FriendMessageEvent]
 * - [临时会话消息事件][TempMessageEvent]
 *
 * @see isContextIdenticalWith 判断语境是否相同
 */
@Suppress("DEPRECATION_ERROR")
abstract class MessageEvent : @PlannedRemoval("1.2.0") ContactMessage(),
    BotEvent, MessageEventExtensions<User, Contact> {

    /**
     * 与这个消息事件相关的 [Bot]
     */
    abstract override val bot: Bot

    /**
     * 消息事件主体.
     *
     * - 对于好友消息, 这个属性为 [Friend] 的实例, 与 [sender] 引用相同;
     * - 对于临时会话消息, 这个属性为 [Member] 的实例, 与 [sender] 引用相同;
     * - 对于群消息, 这个属性为 [Group] 的实例, 与 [GroupMessageEvent.group] 引用相同
     *
     * 在回复消息时, 可通过 [subject] 作为回复对象
     */
    abstract override val subject: Contact

    /**
     * 发送人.
     *
     * 在好友消息时为 [Friend] 的实例, 在群消息时为 [Member] 的实例
     */
    abstract override val sender: User

    /**
     * 发送人名称
     */
    abstract override val senderName: String

    /**
     * 消息内容.
     *
     * 第一个元素一定为 [MessageSource], 存储此消息的发送人, 发送时间, 收信人, 消息 id 等数据.
     * 随后的元素为拥有顺序的真实消息内容.
     */
    abstract override val message: MessageChain

    /** 消息发送时间 (由服务器提供, 可能与本地有时差) */
    abstract override val time: Int

    /**
     * 消息源. 来自 [message] 的第一个元素,
     */
    override val source: OnlineMessageSource.Incoming get() = message.source as OnlineMessageSource.Incoming
}

/** 消息事件的扩展函数 */
@Suppress("EXPOSED_SUPER_INTERFACE") // Functions are visible
interface MessageEventExtensions<out TSender : User, out TSubject : Contact> :
    MessageEventPlatformExtensions<TSender, TSubject> {

    // region 发送 Message

    /**
     * 给这个消息事件的主体发送消息
     * 对于好友消息事件, 这个方法将会给好友 ([subject]) 发送消息
     * 对于群消息事件, 这个方法将会给群 ([subject]) 发送消息
     */
    @JvmSynthetic
    suspend inline fun reply(message: Message): MessageReceipt<TSubject> =
        subject.sendMessage(message.asMessageChain()) as MessageReceipt<TSubject>

    @JvmSynthetic
    suspend inline fun reply(plain: String): MessageReceipt<TSubject> =
        subject.sendMessage(plain.toMessage().asMessageChain()) as MessageReceipt<TSubject>

    // endregion

    @JvmSynthetic
    suspend inline fun ExternalImage.upload(): Image = this.upload(subject)

    @JvmSynthetic
    suspend inline fun ExternalImage.send(): MessageReceipt<TSubject> = this.sendTo(subject)

    @JvmSynthetic
    suspend inline fun Image.send(): MessageReceipt<TSubject> = this.sendTo(subject)

    @JvmSynthetic
    suspend inline fun Message.send(): MessageReceipt<TSubject> = this.sendTo(subject)

    @JvmSynthetic
    suspend inline fun String.send(): MessageReceipt<TSubject> = this.toMessage().sendTo(subject)

    // region 引用回复
    /**
     * 给这个消息事件的主体发送引用回复消息
     * 对于好友消息事件, 这个方法将会给好友 ([subject]) 发送消息
     * 对于群消息事件, 这个方法将会给群 ([subject]) 发送消息
     */
    @JvmSynthetic
    suspend inline fun quoteReply(message: MessageChain): MessageReceipt<TSubject> =
        reply(this.message.quote() + message)

    @JvmSynthetic
    suspend inline fun quoteReply(message: Message): MessageReceipt<TSubject> = reply(this.message.quote() + message)

    @JvmSynthetic
    suspend inline fun quoteReply(plain: String): MessageReceipt<TSubject> = reply(this.message.quote() + plain)

    @JvmSynthetic
    inline fun At.isBot(): Boolean = target == bot.id


    /**
     * 获取图片下载链接
     * @return "http://gchat.qpic.cn/gchatpic_new/..."
     */
    @JvmSynthetic
    suspend inline fun Image.url(): String = this@url.queryUrl()
}

/** 一个消息事件在各平台的相关扩展. 请使用 [MessageEventExtensions] */
internal expect interface MessageEventPlatformExtensions<out TSender : User, out TSubject : Contact> {
    val subject: TSubject
    val sender: TSender
    val message: MessageChain
    val bot: Bot
}


/**
 * 已废弃, 请使用 [MessageEvent]
 */
@PlannedRemoval("1.2.0")
@Deprecated(
    message = "use MessageEvent",
    replaceWith = ReplaceWith("MessageEvent", "net.mamoe.mirai.message.MessageEvent"),
    level = DeprecationLevel.HIDDEN
)
abstract class MessagePacketBase<out TSender : User, out TSubject : Contact> : Packet, BotEvent, AbstractEvent() {
    abstract override val bot: Bot
    abstract val sender: User
    abstract val subject: Contact
    abstract val message: MessageChain
    abstract val time: Int
    abstract val source: OnlineMessageSource.Incoming
    abstract val senderName: String
}

@PlannedRemoval("1.2.0")
@Deprecated(
    message = "Ambiguous name. Use MessageEvent instead",
    replaceWith = ReplaceWith("MessageEvent", "net.mamoe.mirai.message.MessageEvent"),
    level = DeprecationLevel.HIDDEN
)
@Suppress("DEPRECATION_ERROR")
abstract class MessagePacket : MessagePacketBase<User, Contact>(),
    BotEvent, MessageEventExtensions<User, Contact> {
    abstract override val bot: Bot
    abstract override val sender: User
    abstract override val subject: Contact
    abstract override val message: MessageChain
    abstract override val time: Int
    abstract override val source: OnlineMessageSource.Incoming
    abstract override val senderName: String
}

@PlannedRemoval("1.2.0")
@Deprecated(
    message = "Ambiguous name. Use MessageEvent instead",
    replaceWith = ReplaceWith("MessageEvent", "net.mamoe.mirai.message.MessageEvent"),
    level = DeprecationLevel.HIDDEN
)
@Suppress("DEPRECATION_ERROR")
abstract class ContactMessage : MessagePacket(),
    BotEvent, MessageEventExtensions<User, Contact> {
    abstract override val bot: Bot
    abstract override val sender: User
    abstract override val subject: Contact
    abstract override val message: MessageChain
    abstract override val time: Int
    abstract override val source: OnlineMessageSource.Incoming
    abstract override val senderName: String
}

@PlannedRemoval("1.2.0")
@Deprecated(
    message = "Ambiguous name. Use FriendMessageEvent instead",
    replaceWith = ReplaceWith("FriendMessageEvent", "net.mamoe.mirai.message.FriendMessageEvent"),
    level = DeprecationLevel.HIDDEN
)
@Suppress("DEPRECATION_ERROR")
abstract class FriendMessage : MessageEvent() {
    abstract override val bot: Bot
    abstract override val sender: Friend
    abstract override val subject: Friend
    abstract override val message: MessageChain
    abstract override val time: Int
    abstract override val source: OnlineMessageSource.Incoming.FromFriend
    abstract override val senderName: String
}

@PlannedRemoval("1.2.0")
@Deprecated(
    message = "Ambiguous name. Use GroupMessageEvent instead",
    replaceWith = ReplaceWith("GroupMessageEvent", "net.mamoe.mirai.message.GroupMessageEvent"),
    level = DeprecationLevel.HIDDEN
)
@Suppress("DEPRECATION_ERROR")
abstract class GroupMessage : MessageEvent() {
    abstract val group: Group
    abstract override val bot: Bot
    abstract override val sender: Member
    abstract override val subject: Group
    abstract override val message: MessageChain
    abstract override val time: Int
    abstract override val source: OnlineMessageSource.Incoming.FromGroup
    abstract override val senderName: String
}

@PlannedRemoval("1.2.0")
@Deprecated(
    message = "Ambiguous name. Use TempMessageEvent instead",
    replaceWith = ReplaceWith("TempMessageEvent", "net.mamoe.mirai.message.TempMessageEvent"),
    level = DeprecationLevel.HIDDEN
)
abstract class TempMessage : MessageEvent() {
    abstract override val bot: Bot
    abstract override val sender: Member
    abstract override val subject: Member
    abstract override val message: MessageChain
    abstract override val time: Int
    abstract override val source: OnlineMessageSource.Incoming.FromTemp
    abstract val group: Group
    abstract override val senderName: String
}