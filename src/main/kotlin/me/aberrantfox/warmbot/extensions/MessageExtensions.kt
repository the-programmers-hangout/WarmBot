package me.aberrantfox.warmbot.extensions

import me.aberrantfox.kjdautils.extensions.stdlib.sanitiseMentions
import net.dv8tion.jda.core.entities.Message

fun Message.fullContent() = contentRaw + "\n" + attachmentsString()

fun Message.attachmentsString(): String =
        if(attachments.isNotEmpty()) attachments.map { it.url }.reduce { a, b -> "$a\n $b" } else ""

fun Message.cleanContent() = this.fullContent().trimEnd().sanitiseMentions()