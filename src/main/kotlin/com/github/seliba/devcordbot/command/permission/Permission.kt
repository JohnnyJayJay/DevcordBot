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

package com.github.seliba.devcordbot.command.permission

/**
 * Enum for command permissions.
 */
enum class Permission {
    /**
     * Anyone can execute the command.
     */
    ANY,
    /**
     * Only moderators can execute the command.
     */
    MODERATOR,
    /**
     * Only administrators can execute the command.
     */
    ADMIN,
    /**
     * Commands only executable by bot owners.
     */
    BOT_OWNER
}
