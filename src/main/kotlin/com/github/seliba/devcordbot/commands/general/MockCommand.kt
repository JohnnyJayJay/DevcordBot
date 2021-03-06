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
import com.github.seliba.devcordbot.command.AbstractSubCommand
import com.github.seliba.devcordbot.command.CommandCategory
import com.github.seliba.devcordbot.command.context.Context
import com.github.seliba.devcordbot.command.permission.Permission
import com.github.seliba.devcordbot.constants.Embeds
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.requests.restaction.MessageAction
import kotlin.random.Random

/**
 * Mock command.
 */
class MockCommand : AbstractCommand() {
    override val aliases: List<String> = listOf("mock", "m")
    override val displayName: String = "mock"
    override val description: String = "Mockt den eingegebenen Text."
    override val usage: String = "<text>"
    override val permission: Permission = Permission.ANY
    override val category: CommandCategory = CommandCategory.GENERAL

    init {
        registerCommands(MockLastCommand())
    }

    override fun execute(context: Context) {
        val arguments = context.args

        if (arguments.isEmpty()) {
            return context.respond(Embeds.command(this)).queue()
        }

        context.respond(mock(arguments.join())).queue()
    }

    private inner class MockLastCommand : AbstractSubCommand(this) {
        override val aliases: List<String> = listOf("^", "^^")
        override val displayName: String = "^"
        override val description: String = "Mockt die Nachricht über dem Command."
        override val usage: String = ""

        override fun execute(context: Context) {
            if (!context.args.isEmpty()) {
                return context.respond(Embeds.command(this)).queue()
            }

            val paginator = context.channel.iterableHistory.limit(2).cache(false)

            paginator.flatMap(fun(it: MutableList<Message>): MessageAction? {
                if (it.size < 2) {

                    return context.respond(
                        Embeds.error(
                            "Keine Nachricht gefunden",
                            "Es konnte keine Nachricht in dem aktuellen Channel gefunden werden."
                        )
                    )
                }

                val message = it[1].contentRaw
                if (message.isEmpty()) return null

                return context.respond(mock(it[1].contentRaw))
            }).queue()
        }

    }

    /**
     * Mocks a text.
     */
    private fun mock(text: String): String = text.asIterable().joinToString("") { tweak(it).toString() }

    /**
     * Tweaks a given char.
     */
    private fun tweak(c: Char): Char = if (Random.nextBoolean()) c.toLowerCase() else c.toUpperCase()
}
