package me.aberrantfox.warmbot.commands

import me.aberrantfox.kjdautils.api.dsl.*
import me.aberrantfox.kjdautils.extensions.jda.*
import me.aberrantfox.kjdautils.internal.command.arguments.*
import me.aberrantfox.kjdautils.internal.logging.DefaultLogger
import me.aberrantfox.warmbot.arguments.MemberArg
import me.aberrantfox.warmbot.extensions.*
import me.aberrantfox.warmbot.messages.Locale
import me.aberrantfox.warmbot.services.*
import net.dv8tion.jda.core.entities.*
import java.awt.Color
import java.util.concurrent.ConcurrentHashMap

@CommandSet("ReportHelpers")
fun reportHelperCommands(configuration: Configuration, reportService: ReportService,
                         moderationService: ModerationService, loggingService: LoggingService) = commands {

    data class EmbedData(val color: Color, val topic: String, val openMessage: String, val initialMessage: String)

    fun openReport(event: CommandEvent, targetUser: User, guild: Guild, userEmbed: MessageEmbed, embedData: EmbedData, detain: Boolean = false) {
        val guildId = guild.id
        val reportCategory = configuration.getGuildConfig(guildId)!!.reportCategory.idToCategory()

        targetUser.openPrivateChannel().queue {
            it.sendMessage(userEmbed).queue({
                reportCategory.createTextChannel(targetUser.name).queue { channel ->
                    channel as TextChannel

                    val message = embedData.initialMessage

                    val initialMessage =
                        if (message.isNotEmpty()) {
                            targetUser.sendPrivateMessage(message, DefaultLogger())
                            message
                        } else {
                            Locale.messages.DEFAULT_INITIAL_MESSAGE
                        }

                    val reportEmbed = embed {
                        setColor(embedData.color)
                        setThumbnail(targetUser.avatarUrl)
                        addField(embedData.topic,
                            "${targetUser.descriptor()} :: ${targetUser.asMention}",
                            false)
                        addField(embedData.openMessage,
                            "${event.author.descriptor()} :: ${event.author.asMention}",
                            false)
                        addField("Initial Message", initialMessage, false)
                    }

                    channel.sendMessage(reportEmbed).queue()

                    val newReport = Report(targetUser.id, channel.id, guildId, ConcurrentHashMap())
                    reportService.addReport(newReport)

                    if (detain) newReport.detain()

                    event.respond("Success! Channel opened at: ${channel.asMention}")
                    loggingService.staffOpen(guild, channel.name, event.author)
                }
            },
            {
                event.respond("Unable to contact the target user. Direct messages are disabled or the bot is blocked.")
            })
        }
    }

    command("Open") {
        requiresGuild = true
        description = Locale.messages.OPEN_DESCRIPTION
        expect(arg(MemberArg), arg(SentenceArg("Initial Message"), optional = true))
        execute { event ->
            val targetMember = event.args.component1() as Member
            val message = event.args.component2() as String
            val guild = event.message.guild

            if (!hasValidState(event, guild, targetMember.user))
                return@execute

            val userEmbed = embed {
                setColor(Color.green)
                setThumbnail(guild.iconUrl)
                addField("You've received a message from the staff of ${guild.name}!", Locale.messages.BOT_DESCRIPTION, false)
            }

            val embedData = EmbedData(Color.green, "New Report Opened!", "This report was opened by", message)
            openReport(event, targetMember.user, guild, userEmbed, embedData, true)
        }
    }

    command("Detain") {
        requiresGuild = true
        description = Locale.messages.DETAIN_DESCRIPTION
        expect(arg(MemberArg), arg(SentenceArg("Initial Message"), optional = true))
        execute { event ->
            val targetMember = event.args.component1() as Member
            val message = event.args.component2() as String
            val guild = event.message.guild

            if (moderationService.hasStaffRole(targetMember))
                return@execute event.respond("You cannot detain another staff member.")

            targetMember.mute()

            if (targetMember.isDetained())
                return@execute event.respond("This member is already detained.")

            if (!hasValidState(event, guild, targetMember.user))
                return@execute

            val userEmbed = embed {
                setColor(Color.red)
                setThumbnail(guild.iconUrl)
                addField("You've have been detained by the staff of ${guild.name}!", Locale.messages.USER_DETAIN_MESSAGE, false)
            }

            val embedData = EmbedData(Color.red, "User Detained!", "This user was detained by", message)
            openReport(event, targetMember.user, guild, userEmbed, embedData, true)
        }
    }

    command("Release") {
        requiresGuild = true
        description = Locale.messages.RELEASE_DESCRIPTION
        expect(MemberArg)
        execute {
            val targetMember = it.args.component1() as Member

            if (!targetMember.isDetained())
                return@execute it.respond("This member is not detained.")

            targetMember.user.userToReport()?.release()
            it.respond("${targetMember.fullName()} has been released.")
        }
    }

    command("CloseAll") {
        requiresGuild = true
        description = Locale.messages.CLOSE_ALL_DESCRIPTION
        execute {
            val guild = it.guild!!
            val reportsFromGuild = reportService.getReportsFromGuild(guild.id)

            if (reportsFromGuild.isEmpty()) return@execute it.respond("There are no reports to close.")

            reportsFromGuild.forEach { report ->
                val channel = report.channelId.idToTextChannel()

                channel.delete().queue()
                loggingService.commandClose(guild, channel.name, it.author)
            }

            it.respond("${reportsFromGuild.size} report(s) closed successfully.")
        }
    }

    command("Info") {
        requiresGuild = true
        description = Locale.messages.INFO_DESCRIPTION
        expect(arg(TextChannelArg("Report Channel"), optional = true, default = { it.channel }),
            arg(ChoiceArg("Field", "user", "channel", "guild", "all"), optional = true, default = "all"))
        execute {
            val targetChannel = it.args.component1() as TextChannel
            val choice = it.args.component2() as String
            val error = "Command should be invoked in a report channel or target a report channel."

            if (!targetChannel.isReportChannel()) return@execute it.respond(error)

            val report = targetChannel.channelToReport()

            with(report) {
                val allData =
                    "User ID: $userId\n" +
                    "Channel ID: $channelId\n" +
                    "Guild ID: $guildId"

                it.respond(
                    when (choice) {
                        "user" -> userId
                        "channel" -> channelId
                        "guild" -> guildId
                        "all" -> allData
                        else -> "Invalid selection!"
                    }
                )
            }
        }
    }

    command("isReport") {
        requiresGuild = true
        description = Locale.messages.IS_REPORT_DESCRIPTION
        expect(arg(TextChannelArg("Channel"), optional = true, default = { it.channel }))
        execute {
            val channel = it.args.component1() as TextChannel
            val isReport = channel.isReportChannel()

            it.respond("${channel.asMention} ${if (isReport) "is" else "is not"} a valid report channel.")
        }
    }
}

private fun hasValidState(event: CommandEvent, currentGuild: Guild, targetUser: User): Boolean {
    if (!targetUser.hasReportChannel())
        return true

    val report = targetUser.userToReport() ?: return false
    val reportGuild = report.guildId.idToGuild()

    event.respond("The target user already has an open report " +
        if (reportGuild == currentGuild) {
            val channel = report.reportToChannel()?.asMention ?: "<Failed to retrieve channel>"
            "at $channel."
        } else {
            "in ${reportGuild.name}."
        }
    )

    return false
}