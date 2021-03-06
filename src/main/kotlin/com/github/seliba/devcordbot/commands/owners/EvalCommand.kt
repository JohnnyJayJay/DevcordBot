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

package com.github.seliba.devcordbot.commands.owners

import com.github.seliba.devcordbot.command.AbstractCommand
import com.github.seliba.devcordbot.command.CommandCategory
import com.github.seliba.devcordbot.command.context.Context
import com.github.seliba.devcordbot.command.permission.Permission
import com.github.seliba.devcordbot.constants.Embeds
import com.github.seliba.devcordbot.constants.Emotes
import com.github.seliba.devcordbot.dsl.editMessage
import com.github.seliba.devcordbot.util.HastebinUtil
import com.github.seliba.devcordbot.util.limit
import com.github.seliba.devcordbot.util.stringify
import net.dv8tion.jda.api.entities.MessageEmbed
import javax.script.ScriptEngineManager
import javax.script.ScriptException

/**
 * Eval command for bot owners.
 */
class EvalCommand : AbstractCommand() {
    override val aliases: List<String> = listOf("ev")
    override val displayName: String = "eval"
    override val description: String = "Führt Kotlin Code über den Bot aus"
    override val usage: String = "<code>"
    override val permission: Permission = Permission.BOT_OWNER
    override val category: CommandCategory = CommandCategory.BOT_OWNER

    @Suppress("KDocMissingDocumentation")
    override fun execute(context: Context) {
        context.respond(
            Embeds.loading(
                "Code wird kompiliert und ausgeführt",
                "Bitte warte einen Augenblick während dein Script kompiliert und ausgeführt wird"
            )
        ).flatMap {
            val scriptEngine = ScriptEngineManager().getEngineByName("kotlin")
            val script = context.args.join()
            //language=kotlin
            scriptEngine.eval(
                """
                import com.github.seliba.devcordbot.*
                import com.github.seliba.devcordbot.database.*
                import com.github.seliba.devcordbot.command.*
                import com.github.seliba.devcordbot.command.permission.Permission as BotPermission
                import com.github.seliba.devcordbot.command.context.*
                import org.jetbrains.exposed.sql.transactions.*
                import okhttp3.*
                import net.dv8tion.jda.api.*
                import net.dv8tion.jda.api.entities.*
            """.trimIndent()
            )
            scriptEngine.put("context", context)
            val result = try {
                val evaluation = scriptEngine.eval(script)?.toString() ?: "null"
                if (evaluation.length > MessageEmbed.TEXT_MAX_LENGTH - "Ergebniss: ``````".length) {
                    val result = Embeds.info(
                        "Zu langes Ergebniss!",
                        "Ergebniss: ${Emotes.LOADING}"
                    )
                    HastebinUtil.postErrorToHastebin(evaluation, context.bot.httpClient).thenAccept { hasteUrl ->
                        it.editMessage(result.apply {
                            @Suppress("ReplaceNotNullAssertionWithElvisReturn") // Description is set above
                            description = description!!.replace(Emotes.LOADING.toRegex(), hasteUrl)
                        }).queue()
                    }
                    result
                } else {
                    Embeds.info("Erfolgreich ausgeführt!", "Ergebniss: ```$evaluation```")
                }
            } catch (e: ScriptException) {
                val result = Embeds.error(
                    "Fehler!",
                    "Es ist folgender Fehler aufgetreten: ```${e.message?.limit(1024)}``` Detailierter Fehler: ${Emotes.LOADING}"
                )
                HastebinUtil.postErrorToHastebin(e.stringify(), context.bot.httpClient).thenAccept { hasteUrl ->
                    it.editMessage(result.apply {
                        @Suppress("ReplaceNotNullAssertionWithElvisReturn") // Description is set above
                        description = description!!.replace(Emotes.LOADING.toRegex(), hasteUrl)
                    }).queue()
                }
                result
            }
            it.editMessage(result)
        }.queue()
    }
}
