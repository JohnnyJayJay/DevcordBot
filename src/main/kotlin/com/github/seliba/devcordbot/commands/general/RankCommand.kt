/*
 * Copyright 2020 Daniel Scherf & Michael Rittmeister
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.github.seliba.devcordbot.commands.general

import com.github.seliba.devcordbot.command.AbstractCommand
import com.github.seliba.devcordbot.command.CommandCategory
import com.github.seliba.devcordbot.command.context.Context
import com.github.seliba.devcordbot.command.permission.Permission
import com.github.seliba.devcordbot.constants.Embeds
import com.github.seliba.devcordbot.database.DevCordUser
import com.github.seliba.devcordbot.util.XPUtil
import net.dv8tion.jda.api.entities.User
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Rank command.
 */
class RankCommand : AbstractCommand() {
    override val aliases: List<String> = listOf("rank", "r")
    override val displayName: String = "Rank"
    override val description: String = "Rank Command erklärtt sich halt hoffentlich von selbst du banane"
    override val usage: String = ""
    override val permission: Permission = Permission.ANY
    override val category: CommandCategory = CommandCategory.GENERAL

    override fun execute(context: Context) {
        val user = context.args.optionalUser(0, jda = context.jda)
            ?: return sendRankInformation(context.author, context)
        sendRankInformation(user, context)
    }

    private fun sendRankInformation(user: User, context: Context) {
        val entry =
            transaction { DevCordUser.findById(user.idLong) ?: DevCordUser.new(user.idLong) {} }
        val currentXP = entry.experience
        val nextLevelXP = XPUtil.getXpToLevelup(entry.level)
        context.respond(
            Embeds.info(
                "Rang von ${user.asTag}",
                ""
            ) {
                addField(
                    "Level",
                    entry.level.toString(),
                    true
                )
                addField(
                    "Nächstes Level",
                    "${currentXP}/${nextLevelXP}XP ${buildProgressBar(currentXP, nextLevelXP)}"
                )
            }
        ).queue()
    }

    private fun buildProgressBar(current: Long, next: Long): String? {
        val stringBuilder = StringBuilder()
        val barProgress = (current.toDouble() / next * 20).toInt()
        stringBuilder.append("█".repeat(barProgress))
            .append("▒".repeat(20 - barProgress))
        return stringBuilder.toString()
    }
}