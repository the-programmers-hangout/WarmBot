package me.aberrantfox.warmbot.listeners

import com.google.common.eventbus.Subscribe
import me.aberrantfox.warmbot.extensions.*
import me.aberrantfox.warmbot.services.*
import me.jakejmattson.kutils.api.extensions.jda.sendPrivateMessage
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent

class ResponseListener(private val configuration: Configuration) {
    @Subscribe
    fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        if (event.author.isBot) return

        with(event.message) {
            if (contentRaw.startsWith(configuration.prefix)) return

            val report = event.channel.channelToReport() ?: return
            val member = report.reportToMember() ?: return addFailReaction()

            val content = fullContent()

            if (content.trim().isEmpty())
                return

            member.user.sendPrivateMessage(content)
            report.queuedMessageId = id
        }
    }
}