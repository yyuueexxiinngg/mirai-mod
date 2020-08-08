/*
 * Copyright 2020 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/master/LICENSE
 */

@file:JvmMultifileClass
@file:JvmName("BotEventsKt")
@file:Suppress("unused", "INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "RESULT_CLASS_IN_RETURN_TYPE")

package net.mamoe.mirai.event.events

import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.event.AbstractEvent
import net.mamoe.mirai.event.CancellableEvent
import net.mamoe.mirai.event.events.ImageUploadEvent.Failed
import net.mamoe.mirai.event.events.ImageUploadEvent.Succeed
import net.mamoe.mirai.message.MessageReceipt
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.Message
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.MessageSource
import net.mamoe.mirai.qqandroid.network.Packet
import net.mamoe.mirai.utils.ExternalImage
import net.mamoe.mirai.utils.PlannedRemoval
import net.mamoe.mirai.utils.SinceMirai
import kotlin.internal.InlineOnly
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSynthetic


// region MessagePreSendEvent

/**
 * 在发送消息前广播的事件. 可被 [取消][CancellableEvent.cancel].
 *
 * 此事件总是在 [MessagePostSendEvent] 之前广播.
 *
 * 当 [MessagePreSendEvent] 被 [取消][CancellableEvent.cancel] 后:
 * - [MessagePostSendEvent] 不会广播
 * - 消息不会发送.
 * - [Contact.sendMessage] 会抛出异常 [EventCancelledException]
 *
 * @see Contact.sendMessage 发送消息. 为广播这个事件的唯一途径
 */
@SinceMirai("1.1.0")
sealed class MessagePreSendEvent : BotEvent, BotActiveEvent, AbstractEvent(), CancellableEvent {
    /** 发信目标. */
    abstract val target: Contact
    final override val bot: Bot get() = target.bot

    /** 待发送的消息. 修改后将会同时应用于发送. */
    abstract var message: Message
}

/**
 * 在发送群消息前广播的事件.
 * @see MessagePreSendEvent
 */
@SinceMirai("1.1.0")
data class GroupMessagePreSendEvent internal constructor(
    /** 发信目标. */
    override val target: Group,
    /** 待发送的消息. 修改后将会同时应用于发送. */
    override var message: Message
) : MessagePreSendEvent()

/**
 * 在发送好友或群临时会话消息前广播的事件.
 * @see MessagePreSendEvent
 */
@SinceMirai("1.1.0")
sealed class UserMessagePreSendEvent : MessagePreSendEvent() {
    /** 发信目标. */
    abstract override val target: User
}

/**
 * 在发送好友消息前广播的事件.
 * @see MessagePreSendEvent
 */
@SinceMirai("1.1.0")
data class FriendMessagePreSendEvent internal constructor(
    /** 发信目标. */
    override val target: Friend,
    /** 待发送的消息. 修改后将会同时应用于发送. */
    override var message: Message
) : UserMessagePreSendEvent()

/**
 * 在发送群临时会话消息前广播的事件.
 * @see MessagePreSendEvent
 */
@SinceMirai("1.1.0")
data class TempMessagePreSendEvent internal constructor(
    /** 发信目标. */
    override val target: Member,
    /** 待发送的消息. 修改后将会同时应用于发送. */
    override var message: Message
) : UserMessagePreSendEvent() {
    val group get() = target.group
}


// endregion

// region MessagePostSendEvent

/**
 * 在发送消息后广播的事件, 总是在 [MessagePreSendEvent] 之后广播.
 *
 * 只要 [MessagePreSendEvent] 未被 [取消][CancellableEvent.cancel], [MessagePostSendEvent] 就一定会被广播, 并携带 [发送时产生的异常][MessagePostSendEvent.exception] (如果有).
 *
 * 在此事件广播前, 消息一定已经发送成功, 或产生一个异常.
 *
 * @see Contact.sendMessage 发送消息. 为广播这个事件的唯一途径
 * @see MessagePreSendEvent
 */
@SinceMirai("1.1.0")
sealed class MessagePostSendEvent<C : Contact> : BotEvent, BotActiveEvent, AbstractEvent() {
    /** 发信目标. */
    abstract val target: C
    final override val bot: Bot get() = target.bot

    /** 待发送的消息. 此为 [MessagePreSendEvent.message] 的最终值. */
    abstract val message: MessageChain

    /**
     * 发送消息时抛出的异常. `null` 表示消息成功发送.
     * @see result
     */
    abstract val exception: Throwable?

    /**
     * 发送消息成功时的回执. `null` 表示消息发送失败.
     * @see result
     */
    abstract val receipt: MessageReceipt<C>?
}

/**
 * 获取指代这条已经发送的消息的 [MessageSource]. 若消息发送失败, 返回 `null`
 * @see MessagePostSendEvent.sourceResult
 */
@get:JvmSynthetic
@SinceMirai("1.1.0")
inline val MessagePostSendEvent<*>.source: MessageSource?
    get() = receipt?.source

/**
 * 获取指代这条已经发送的消息的 [MessageSource], 并包装为 [kotlin.Result]
 * @see MessagePostSendEvent.result
 */
@get:JvmSynthetic
@SinceMirai("1.1.0")
inline val MessagePostSendEvent<*>.sourceResult: Result<MessageSource>
    get() = result.map { it.source }

/**
 * 在此消息发送成功时返回 `true`.
 * @see MessagePostSendEvent.exception
 * @see MessagePostSendEvent.result
 */
@get:JvmSynthetic
@SinceMirai("1.1.0")
inline val MessagePostSendEvent<*>.isSuccess: Boolean
    get() = exception == null

/**
 * 在此消息发送失败时返回 `true`.
 * @see MessagePostSendEvent.exception
 * @see MessagePostSendEvent.result
 */
@get:JvmSynthetic
@SinceMirai("1.1.0")
inline val MessagePostSendEvent<*>.isFailure: Boolean
    get() = exception != null

/**
 * 将 [MessagePostSendEvent.exception] 与 [MessagePostSendEvent.receipt] 表示为 [Result]
 */
@InlineOnly
@SinceMirai("1.1.0")
inline val <C : Contact> MessagePostSendEvent<C>.result: Result<MessageReceipt<C>>
    get() = exception.let { exception -> if (exception != null) Result.failure(exception) else Result.success(receipt!!) }

/**
 * 在群消息发送后广播的事件.
 * @see MessagePostSendEvent
 */
@SinceMirai("1.1.0")
data class GroupMessagePostSendEvent internal constructor(
    /** 发信目标. */
    override val target: Group,
    /** 待发送的消息. 此为 [MessagePreSendEvent.message] 的最终值. */
    override val message: MessageChain,
    /**
     * 发送消息时抛出的异常. `null` 表示消息成功发送.
     * @see result
     */
    override val exception: Throwable?,
    /**
     * 发送消息成功时的回执. `null` 表示消息发送失败.
     * @see result
     */
    override val receipt: MessageReceipt<Group>?
) : MessagePostSendEvent<Group>()

/**
 * 在好友或群临时会话消息发送后广播的事件.
 * @see MessagePostSendEvent
 */
@SinceMirai("1.1.0")
sealed class UserMessagePostSendEvent<C : User> : MessagePostSendEvent<C>()

/**
 * 在好友消息发送后广播的事件.
 * @see MessagePostSendEvent
 */
@SinceMirai("1.1.0")
data class FriendMessagePostSendEvent internal constructor(
    /** 发信目标. */
    override val target: Friend,
    /** 待发送的消息. 此为 [MessagePreSendEvent.message] 的最终值. */
    override val message: MessageChain,
    /**
     * 发送消息时抛出的异常. `null` 表示消息成功发送.
     * @see result
     */
    override val exception: Throwable?,
    /**
     * 发送消息成功时的回执. `null` 表示消息发送失败.
     * @see result
     */
    override val receipt: MessageReceipt<Friend>?
) : UserMessagePostSendEvent<Friend>()

/**
 * 在群临时会话消息发送后广播的事件.
 * @see MessagePostSendEvent
 */
@SinceMirai("1.1.0")
data class TempMessagePostSendEvent internal constructor(
    /** 发信目标. */
    override val target: Member,
    /** 待发送的消息. 此为 [MessagePreSendEvent.message] 的最终值. */
    override val message: MessageChain,
    /**
     * 发送消息时抛出的异常. `null` 表示消息成功发送.
     * @see result
     */
    override val exception: Throwable?,
    /**
     * 发送消息成功时的回执. `null` 表示消息发送失败.
     * @see result
     */
    override val receipt: MessageReceipt<Member>?
) : UserMessagePostSendEvent<Member>() {
    val group get() = target.group
}

// endregion

/**
 * 消息撤回事件. 可是任意消息被任意人撤回.
 *
 * @see Contact.recall 撤回消息. 为广播这个事件的唯一途径
 */
sealed class MessageRecallEvent : BotEvent, AbstractEvent() {
    /**
     * 消息原发送人
     */
    abstract val authorId: Long

    /**
     * 消息 id.
     * @see MessageSource.id
     */
    abstract val messageId: Int

    /**
     * 消息内部 id.
     * @see MessageSource.id
     */
    abstract val messageInternalId: Int

    /**
     * 原发送时间
     */
    abstract val messageTime: Int // seconds

    /**
     * 好友消息撤回事件, 暂不支持.
     */
    data class FriendRecall internal constructor(
        override val bot: Bot,
        override val messageId: Int,
        override val messageInternalId: Int,
        override val messageTime: Int,
        /**
         * 撤回操作人, 好友的 [User.id]
         */
        val operator: Long
    ) : MessageRecallEvent(), Packet {
        override val authorId: Long
            get() = bot.id
    }

    /**
     * 群消息撤回事件.
     */
    data class GroupRecall internal constructor(
        override val bot: Bot,
        override val authorId: Long,
        override val messageId: Int,
        override val messageInternalId: Int,
        override val messageTime: Int,
        /**
         * 操作人. 为 null 时则为 [Bot] 操作.
         */
        override val operator: Member?,
        override val group: Group
    ) : MessageRecallEvent(), GroupOperableEvent, Packet
}

val MessageRecallEvent.GroupRecall.author: Member
    get() = if (authorId == bot.id) group.botAsMember else group[authorId]

val MessageRecallEvent.FriendRecall.isByBot: Boolean get() = this.operator == bot.id
// val MessageRecallEvent.GroupRecall.isByBot: Boolean get() = (this as GroupOperableEvent).isByBot
// no need

val MessageRecallEvent.isByBot: Boolean
    get() = when (this) {
        is MessageRecallEvent.FriendRecall -> this.isByBot
        is MessageRecallEvent.GroupRecall -> (this as GroupOperableEvent).isByBot
    }


/**
 * 图片上传前. 可以阻止上传.
 *
 * 此事件总是在 [ImageUploadEvent] 之前广播.
 * 若此事件被取消, [ImageUploadEvent] 不会广播.
 *
 * @see Contact.uploadImage 上传图片. 为广播这个事件的唯一途径
 */
data class BeforeImageUploadEvent internal constructor(
    val target: Contact,
    val source: ExternalImage
) : BotEvent, BotActiveEvent, AbstractEvent(), CancellableEvent {
    override val bot: Bot
        get() = target.bot
}

/**
 * 图片上传完成.
 *
 * 此事件总是在 [BeforeImageUploadEvent] 之后广播.
 * 若 [BeforeImageUploadEvent] 被取消, 此事件不会广播.
 *
 * @see Contact.uploadImage 上传图片. 为广播这个事件的唯一途径
 *
 * @see Succeed
 * @see Failed
 */
sealed class ImageUploadEvent : BotEvent, BotActiveEvent, AbstractEvent() {
    abstract val target: Contact
    abstract val source: ExternalImage
    override val bot: Bot
        get() = target.bot

    data class Succeed internal constructor(
        override val target: Contact,
        override val source: ExternalImage,
        val image: Image
    ) : ImageUploadEvent()

    data class Failed internal constructor(
        override val target: Contact,
        override val source: ExternalImage,
        val errno: Int,
        val message: String
    ) : ImageUploadEvent()
}


// region deprecated

/**
 * 主动发送消息
 *
 * @see Contact.sendMessage 发送消息. 为广播这个事件的唯一途径
 */
@Suppress("DEPRECATION")
@PlannedRemoval("1.3.0") // arise deprecation level to ERROR in 1.2.0.
@Deprecated(
    message = """
        以 MessagePreSendEvent 和 MessagePostSendEvent 替换.
    """,
    replaceWith = ReplaceWith("MessagePreSendEvent", "net.mamoe.mirai.event.events.MessagePreSendEvent"),
    level = DeprecationLevel.WARNING
)
sealed class MessageSendEvent : BotEvent, BotActiveEvent, AbstractEvent() {
    abstract val target: Contact
    final override val bot: Bot
        get() = target.bot

    @Deprecated(
        message = """
        以 GroupMessagePreSendEvent 和 GroupMessagePostSendEvent 替换.
    """,
        replaceWith = ReplaceWith("GroupMessagePreSendEvent", "net.mamoe.mirai.event.events.GroupMessagePreSendEvent"),
        level = DeprecationLevel.WARNING
    )
    data class GroupMessageSendEvent internal constructor(
        override val target: Group,
        var message: MessageChain
    ) : MessageSendEvent(), CancellableEvent

    @Deprecated(
        message = """
        以 FriendMessagePreSendEvent 和 FriendMessagePostSendEvent 替换.
    """,
        replaceWith = ReplaceWith(
            "FriendMessagePreSendEvent",
            "net.mamoe.mirai.event.events.FriendMessagePreSendEvent"
        ),
        level = DeprecationLevel.WARNING
    )
    data class FriendMessageSendEvent internal constructor(
        override val target: Friend,
        var message: MessageChain
    ) : MessageSendEvent(), CancellableEvent

    @Deprecated(
        message = """
        以 TempMessagePreSendEvent 和 TempMessagePostSendEvent 替换.
    """,
        replaceWith = ReplaceWith("TempMessagePreSendEvent", "net.mamoe.mirai.event.events.TempMessagePreSendEvent"),
        level = DeprecationLevel.WARNING
    )
    data class TempMessageSendEvent internal constructor(
        override val target: Member,
        var message: MessageChain
    ) : MessageSendEvent(), CancellableEvent
}
// endregion