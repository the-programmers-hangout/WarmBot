package me.aberrantfox.warmbot

import me.aberrantfox.kjdautils.api.startBot
import me.aberrantfox.warmbot.extensions.htmlString
import net.dv8tion.jda.core.entities.Game
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val token = args.first()

        if(token == "UNSET") {
        println("You must specify the token with the -e flag when running via docker.")
        System.exit(-1)
    }

    startBot(token) {

        configure {
            prefix = "!"
            globalPath = "me.aberrantfox.warmbot"
        }


        jda.presence.setPresence(Game.of(Game.GameType.DEFAULT, "DM to contact Staff"), true)
//
        jda.getTextChannelById("555984239343042573").htmlString()
         exitProcess(0)
    }
}