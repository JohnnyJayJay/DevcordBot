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
import com.github.seliba.devcordbot.command.perrmission.Permission
import com.github.seliba.devcordbot.constants.Colors
import com.github.seliba.devcordbot.constants.Embeds
import com.github.seliba.devcordbot.database.*
import com.github.seliba.devcordbot.dsl.embed
import com.github.seliba.devcordbot.menu.Paginator
import net.dv8tion.jda.api.utils.MarkdownSanitizer
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Tag command.
 */
class TagCommand : AbstractCommand() {
    private val reservedNames: List<String>
    override val aliases: List<String> = listOf("tag", "tags", "t")
    override val displayName: String = "Tag"
    override val description: String = ""
    override val usage: String = "<tagname>"
    override val permission: Permission = Permission.ANY
    override val category: CommandCategory = CommandCategory.GENERAL

    init {
        registerCommands(CreateCommand())
        registerCommands(AliasCommand())
        registerCommands(EditCommand())
        registerCommands(InfoCommand())
        registerCommands(DeleteCommand())
        registerCommands(ListCommand())
        registerCommands(FromCommand())
        registerCommands(SearchCommand())
        registerCommands(RawCommand())
        reservedNames = registeredCommands.flatMap { it.aliases }
    }

    override fun execute(context: Context) {
        val args = context.args
        if (args.isEmpty()) {
            return context.sendHelp().queue()
        }
        val tagName = context.args.join()
        val tag = transaction { checkNotTagExists(tagName, context) } ?: return
        context.respond(tag.content).queue()
        transaction {
            tag.usages++
        }
    }

    private inner class CreateCommand : AbstractSubCommand(this) {
        override val aliases: List<String> = listOf("create", "c", "add", "a")
        override val displayName: String = "Add"
        override val description: String = "Erstellt einen neuen Tag"
        override val usage: String = "<name> \n <text>"

        override fun execute(context: Context) {
            val (name, content) = parseTag(context) ?: return
            if (transaction { checkTagExists(name, context) }) return
            val tag = transaction {
                Tag.new(name) {
                    this.content = content
                    author = context.author.idLong
                }
            }
            context.respond(
                Embeds.success(
                    "Erfolgreich erstellt!",
                    "Der Tag mit dem Namen `${tag.name}` wurde erfolgreich erstellt."
                )
            ).queue()
        }

    }

    private inner class AliasCommand : AbstractSubCommand(this) {
        override val aliases: List<String> = listOf("alias")
        override val displayName: String = "Add"
        override val description: String = "Erstellt einen neuen Tag Alias"
        override val usage: String = "<alias> <tag>"

        override fun execute(context: Context) {
            val args = context.args
            val aliasName = args.requiredArgument(0, context) ?: return
            val tagName = args.subList(1, args.size).joinToString(" ")
            val tag = transaction { checkNotTagExists(tagName, context) } ?: return
            if (transaction { checkTagExists(aliasName, context) }) return
            val (newAliasName, newTagName) = transaction {
                val alias = TagAlias.new(aliasName) {
                    this.tag = tag
                }
                alias.name to alias.tag.name
            }
            context.respond(
                Embeds.success(
                    "Alias erfolgreich erstellt",
                    "Es wurde erfolgreich ein Alias mit dem Namen $newAliasName für den Tag $newTagName erstellt"
                )
            ).queue()
        }
    }

    private inner class EditCommand : AbstractSubCommand(this) {
        override val aliases: List<String> = listOf("edit", "e")
        override val displayName: String = "Edit"
        override val description: String = "Editiert einen existierenden Tag"
        override val usage: String = "<tagname> \n <newcontent>"

        override fun execute(context: Context) {
            val (name, content) = parseTag(context) ?: return
            val tag = transaction { checkNotTagExists(name, context) } ?: return
            if (checkPermission(tag, context)) return
            transaction {
                tag.content = content
            }
            context.respond(
                Embeds.success(
                    "Tag erfolgreich bearbeitet!",
                    "Du hast den Tag mit dem Namen `${tag.name}` erfolgreich bearbeitet."
                )
            ).queue()
        }

    }

    private inner class InfoCommand : AbstractSubCommand(this) {

        override val aliases: List<String> = listOf("info", "i")
        override val displayName: String = "Info"
        override val description: String = "Zeigt Informationen über einen Tag an"
        override val usage: String = "<tag>"
        override fun execute(context: Context) {
            val args = context.args
            if (args.isEmpty()) {
                return context.sendHelp().queue()
            }
            val name = args.join()
            val tag = transaction { checkNotTagExists(name, context) } ?: return
            val rank = transaction {
                Tags.select { (Tags.usages greaterEq tag.usages) and (Tags.createdAt greaterEq tag.createdAt) }.count()
            }
            val author = context.jda.getUserById(tag.author)
            context.respond(
                embed {
                    color = Colors.BLUE
                    val creator = author?.name ?: "Mysteriöse Person"
                    author {
                        this.name = creator
                        iconUrl = author?.avatarUrl
                    }
                    addField("Erstellt von", creator, inline = true)
                    addField("Benutzungen", tag.usages.toString(), inline = true)
                    addField("Rang", rank.toString(), inline = true)
                    addField(
                        "Aliase",
                        TagAlias.find { TagAliases.tag eq tag.name }.joinToString(
                            prefix = "`",
                            separator = "`, `",
                            postfix = "`"
                        ),
                        inline = true
                    )
                }
            ).queue()
        }

    }

    private inner class DeleteCommand : AbstractSubCommand(this) {

        override val aliases: List<String> = listOf("delete", "del", "d", "remove", "rem", "r")
        override val displayName: String = "delete"
        override val description: String = "Löscht einen Tag"
        override val usage: String = "<tag>"
        override fun execute(context: Context) {
            val tag = transaction { checkNotTagExists(name, context) } ?: return
            if (checkPermission(tag, context)) return

            transaction {
                TagAliases.deleteWhere { TagAliases.tag.eq(tag.name) }
                tag.delete()
            }

            context.respond(
                Embeds.success(
                    "Tag erfolgreich gelöscht!",
                    "Du hast den Tag mit dem Namen ${tag.name} erfolgreich gelöscht."
                )
            ).queue()
        }

    }

    private inner class ListCommand : AbstractSubCommand(this) {
        override val aliases: List<String> = listOf("list", "all")
        override val displayName: String = "List"
        override val description: String = "Gibt eine Liste aller Tags aus"
        override val usage: String = ""

        override fun execute(context: Context) {
            val tags = transaction { Tag.all().orderBy(Tags.usages to SortOrder.DESC).map(Tag::name) }
            if (tags.isEmpty()) {
                return context.respond(Embeds.error("Keine Tags gefunden!", "Es gibt keine Tags.")).queue()
            }
            Paginator(tags, context.author, context.channel, "Tags")
        }
    }

    private inner class FromCommand : AbstractSubCommand(this) {
        override val aliases: List<String> = listOf("from", "by")
        override val displayName: String = "from"
        override val description: String = "Gibt eine Liste aller Tags eines bestimmten Benutzers aus"
        override val usage: String = "<@user>"

        override fun execute(context: Context) {
            val user = context.args.user(0, context = context) ?: context.author
            val tags = transaction { Tag.find { Tags.author eq user.idLong }.map(Tag::name) }
            if (tags.isEmpty()) {
                return context.respond(Embeds.error("Keine Tags gefunden!", "Es gibt keine Tags von diesem User."))
                    .queue()
            }
            Paginator(tags, context.author, context.channel, "Tags von ${user.name}")
        }
    }

    private inner class SearchCommand : AbstractSubCommand(this) {
        override val aliases: List<String> = listOf("search", "find")
        override val displayName: String = "search"
        override val description: String = "Gibt die ersten 25 Tags mit dem angegebenen Namen"
        override val usage: String = "<name>"

        override fun execute(context: Context) {
            val name = context.args.join()
            val tags = transaction {
                Tag.find { Tags.name similar name }.orderBy(similarity(Tags.name, name) to SortOrder.DESC).limit(25)
                    .map(Tag::name)
            }
            if (tags.isEmpty()) {
                return context.respond(Embeds.error("Keine Tags gefunden!", "Es gibt keine Tags von diesem Namen."))
                    .queue()
            }
            Paginator(tags, context.author, context.channel, "Suche für $name")
        }
    }

    private inner class RawCommand : AbstractSubCommand(this) {
        override val aliases: List<String> = listOf("raw")
        override val displayName: String = "Raw"
        override val description: String = "Zeigt dir einen Tag ohne Markdown an"
        override val usage: String = "<tagname>"

        override fun execute(context: Context) {
            val tagName = context.args.join()
            val tag = transaction { checkNotTagExists(tagName, context) } ?: return
            val content = MarkdownSanitizer.sanitize(tag.content, MarkdownSanitizer.SanitizationStrategy.ESCAPE)
            context.respond(content).queue()
        }
    }

    private fun Tag.Companion.findByName(name: String) = Tag.findById(name) ?: TagAlias.findById(name)?.tag

    private fun checkPermission(
        tag: Tag,
        context: Context
    ): Boolean {
        if (tag.author != context.author.idLong && !context.hasModerator()) {
            context.respond(
                Embeds.error(
                    "Keine Berechtigung!",
                    "Nur Teammitglieder können nicht selbst-erstelle Tags bearbeiten."
                )
            ).queue()
            return true
        }
        return false
    }

    private fun parseTag(context: Context): Pair<String, String>? {
        val args = context.args.split("\n")
        val (name, content) = if (args.size == 1) {
            context.args.first() to context.args.subList(1, context.args.size).joinToString(" ")
        } else {
            args.first().trim() to args.subList(1, args.size).joinToString("\n")
        }
        if (name.isBlank() or content.isBlank()) {
            context.sendHelp().queue()
            return null
        }
        if (checkNameLength(name, context) or checkReservedName(name, context)) return null

        return name to content
    }

    private fun checkTagExists(name: String, context: Context): Boolean {
        val tag = Tag.findByName(name)
        if (tag != null) {
            context.respond(
                Embeds.error(
                    "Tag existiert bereits!", "Dieser Tag existiert bereits."
                )
            ).queue()
            return true
        }
        return false

    }

    private fun checkNotTagExists(name: String, context: Context): Tag? {
        val foundTag = Tag.findByName(name)
        return if (foundTag != null) foundTag else {
            val similarTag =
                Tag.find { Tags.name similar name }.orderBy(similarity(Tags.name, name) to SortOrder.DESC).firstOrNull()
            val similarTagHint = if (similarTag != null) " Meintest du vielleicht `${similarTag.name}`?" else null
            return context.respond(
                Embeds.error(
                    "Tag nicht gefunden!",
                    "Es wurde kein Tag mit dem Namen $name gefunden.$similarTagHint"
                )
            ).queue().run { null }
        }
    }

    private fun
            checkNameLength(name: String, context: Context): Boolean {
        if (name.length > Tag.NAME_MAX_LENGTH) {
            context.respond(
                Embeds.error(
                    "Zu langer Name!",
                    "Der Name darf maximal ${Tag.NAME_MAX_LENGTH} Zeichen lang sein."
                )
            ).queue()
            return true
        }
        return false
    }

    private fun checkReservedName(name: String, context: Context): Boolean {
        if (name in reservedNames) {
            context.respond(
                Embeds.error(
                    "Reservierter Name!",
                    "Die folgenden Namen sind reserviert `${reservedNames.joinToString()}`."
                )
            ).queue()
            return true
        }
        return false
    }
}
