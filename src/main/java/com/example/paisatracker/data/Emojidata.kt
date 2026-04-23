package com.example.paisatracker.data

/**
 * Comprehensive emoji dataset organized by semantic category.
 * Covers all Unicode emoji blocks supported on modern Android (API 24+).
 * Each [EmojiCategory] groups related emojis for smart suggestion and browsing.
 */

data class EmojiCategory(
    val label: String,
    val icon: String,
    val emojis: List<String>
)

val allEmojiCategories: List<EmojiCategory> = listOf(

    EmojiCategory(
        label = "Finance & Money",
        icon = "💰",
        emojis = listOf(
            "💰", "💵", "💴", "💶", "💷", "💸", "💳", "🏦", "🪙", "💱",
            "📈", "📉", "📊", "🧾", "🏧", "💹", "💲", "🤑", "💎", "🏆",
            "🎰", "📑", "🗂️", "🔐", "🔏", "📋", "🧮", "🪣", "📌", "📍"
        )
    ),

    EmojiCategory(
        label = "Food & Drink",
        icon = "🍔",
        emojis = listOf(
            "🍔", "🍕", "🌮", "🌯", "🥪", "🥙", "🧆", "🥚", "🍳", "🥘",
            "🍲", "🍜", "🍝", "🍛", "🍣", "🍱", "🥗", "🍱", "🍗", "🥩",
            "🥓", "🌭", "🍟", "🧇", "🥞", "🧈", "🍞", "🥐", "🥨", "🧀",
            "☕", "🍵", "🧃", "🥤", "🧋", "🍺", "🍻", "🥂", "🍷", "🍸",
            "🍹", "🧉", "🍦", "🍧", "🍨", "🍩", "🍪", "🎂", "🧁", "🍰",
            "🍫", "🍬", "🍭", "🍮", "🍯", "🫖", "🫗", "🫙", "🥛", "🍶"
        )
    ),

    EmojiCategory(
        label = "Shopping & Retail",
        icon = "🛒",
        emojis = listOf(
            "🛒", "🛍️", "🏪", "🏬", "🏷️", "🎁", "📦", "📫", "📬", "🗃️",
            "👜", "👛", "👝", "🎒", "🧳", "💼", "🪤", "🛋️", "🪑", "🖼️",
            "🪞", "🧴", "🧸", "🪆", "🎀", "🎊", "🎉", "🎈", "🪅", "🧩"
        )
    ),

    EmojiCategory(
        label = "Home & Living",
        icon = "🏠",
        emojis = listOf(
            "🏠", "🏡", "🏘️", "🏗️", "🏢", "🏣", "🏤", "🏥", "🏦", "🏨",
            "🔑", "🗝️", "🚪", "🪟", "🛏️", "🛁", "🚿", "🪥", "🧻", "🪠",
            "🧹", "🧺", "🧼", "🫧", "🪣", "🧽", "🪒", "🪤", "🔧", "🪛",
            "🔨", "⚒️", "🛠️", "🔩", "🪝", "🧲", "💡", "🔌", "🔋", "🪫",
            "🔦", "🕯️", "🪔", "🧯", "🛢️", "🪜", "🏚️", "🪞", "🛋️", "🪑"
        )
    ),

    EmojiCategory(
        label = "Transport & Travel",
        icon = "🚗",
        emojis = listOf(
            "🚗", "🚕", "🚙", "🚌", "🚎", "🏎️", "🚓", "🚑", "🚒", "🚐",
            "🛻", "🚚", "🚛", "🚜", "🛵", "🏍️", "🚲", "🛴", "🛺", "🚁",
            "✈️", "🛸", "🚀", "🛩️", "🚂", "🚆", "🚇", "🚊", "🚝", "🚞",
            "⛽", "🛞", "🪝", "⚓", "🛟", "🗺️", "🧭", "🗼", "🏔️", "⛺",
            "🏖️", "🏝️", "🌍", "🌎", "🌏", "🎫", "🎟️", "🛂", "🛃", "🛄"
        )
    ),

    EmojiCategory(
        label = "Health & Medical",
        icon = "💊",
        emojis = listOf(
            "💊", "💉", "🩺", "🩻", "🩹", "🩼", "🦯", "🩸", "🧬", "🔬",
            "🏥", "⚕️", "🫁", "🫀", "🧠", "🦷", "🦴", "👁️", "👂", "👃",
            "🏋️", "🧘", "🛌", "😷", "🤒", "🤕", "💪", "🦾", "🧪", "🌡️"
        )
    ),

    EmojiCategory(
        label = "Fashion & Clothing",
        icon = "👗",
        emojis = listOf(
            "👗", "👘", "🥻", "🩱", "👙", "👚", "👛", "👜", "👝", "🎒",
            "🧳", "👒", "🎩", "🧢", "⛑️", "👟", "👠", "👡", "👢", "🥾",
            "🩴", "🧦", "🧤", "🧣", "🧥", "👔", "👕", "👖", "🩲", "🩳",
            "💄", "💍", "💎", "👑", "💅", "🪮", "🪭", "🕶️", "🥽", "🧲"
        )
    ),

    EmojiCategory(
        label = "Tech & Electronics",
        icon = "📱",
        emojis = listOf(
            "📱", "💻", "🖥️", "🖨️", "⌨️", "🖱️", "🖲️", "💾", "💿", "📀",
            "📷", "📸", "📹", "🎥", "📽️", "🎞️", "📺", "📻", "🎙️", "🎚️",
            "🎛️", "🧭", "⏱️", "⏲️", "🕰️", "📡", "🔋", "🔌", "💡", "🔦",
            "🕹️", "🎮", "🤖", "🪄", "🧲", "🔭", "🔬", "📟", "📠", "☎️"
        )
    ),

    EmojiCategory(
        label = "Education & Work",
        icon = "📚",
        emojis = listOf(
            "📚", "📖", "📝", "✏️", "🖊️", "🖋️", "🖌️", "📐", "📏", "🗂️",
            "📁", "📂", "🗃️", "🗄️", "📋", "📌", "📍", "🗓️", "📅", "📆",
            "🎓", "🏫", "📰", "🗞️", "📃", "📄", "📑", "🔖", "🏷️", "🔗",
            "🧑‍💻", "👨‍🏫", "👩‍🔬", "👨‍🎨", "👩‍⚕️", "🧑‍🏭", "📊", "📈", "📉", "🗺️"
        )
    ),

    EmojiCategory(
        label = "Entertainment",
        icon = "🎬",
        emojis = listOf(
            "🎬", "🎭", "🎪", "🎨", "🎧", "🎤", "🎵", "🎶", "🎷", "🎸",
            "🥁", "🎹", "🎺", "🎻", "🪕", "🪘", "🪗", "🪈", "🎲", "♟️",
            "🎯", "🎳", "🎮", "🕹️", "🎰", "🃏", "🀄", "🎴", "🎫", "🎟️",
            "🏟️", "🎡", "🎢", "🎠", "🎭", "🎪", "🤹", "🎃", "🎄", "🎆"
        )
    ),

    EmojiCategory(
        label = "Sports & Fitness",
        icon = "⚽",
        emojis = listOf(
            "⚽", "🏀", "🏈", "⚾", "🥎", "🎾", "🏐", "🏉", "🥏", "🎱",
            "🏓", "🏸", "🏒", "🥍", "🏑", "🏏", "🪃", "⛳", "🏹", "🎣",
            "🤿", "🥊", "🥋", "🎽", "🛹", "🛼", "🛷", "🥌", "🏋️", "🤼",
            "🤸", "⛹️", "🤾", "🏌️", "🏇", "🧗", "🚵", "🚴", "🏊", "🤽"
        )
    ),

    EmojiCategory(
        label = "Nature & Animals",
        icon = "🌿",
        emojis = listOf(
            "🌿", "🌱", "🌲", "🌳", "🌴", "🎋", "🎍", "🍀", "🌾", "🌵",
            "🐾", "🐕", "🐈", "🐇", "🐹", "🐿️", "🦊", "🦁", "🐯", "🐮",
            "🐷", "🐸", "🐵", "🦋", "🐛", "🐝", "🌺", "🌻", "🌹", "🌷",
            "🌸", "💐", "🍁", "🍂", "🍃", "🌙", "⭐", "🌟", "✨", "☀️"
        )
    ),

    EmojiCategory(
        label = "Family & People",
        icon = "👶",
        emojis = listOf(
            "👶", "🧒", "👧", "🧑", "👦", "👩", "👨", "🧑‍🦱", "🧑‍🦰", "🧑‍🦳",
            "👴", "👵", "👫", "👬", "👭", "💑", "👪", "🏠", "🎠", "🍼",
            "🧸", "🪆", "🎀", "🎁", "🎈", "🎉", "🎊", "💝", "💖", "💗"
        )
    ),

    EmojiCategory(
        label = "Goals & Projects",
        icon = "🎯",
        emojis = listOf(
            "🎯", "🚀", "⚡", "🔥", "💡", "🌟", "🏆", "🥇", "🥈", "🥉",
            "🎖️", "🏅", "🎗️", "🏁", "🚩", "🎌", "⚑", "📣", "📢", "🔔",
            "🔕", "🔊", "🔉", "🔈", "🔇", "✅", "❎", "✔️", "❌", "⭕",
            "🔴", "🟠", "🟡", "🟢", "🔵", "🟣", "⚫", "⚪", "🟤", "🔶"
        )
    ),

    EmojiCategory(
        label = "Misc & Symbols",
        icon = "🔮",
        emojis = listOf(
            "🔮", "🪄", "🧿", "💠", "🔯", "🃏", "🎴", "🀄", "🧩", "🪬",
            "☯️", "☮️", "✝️", "🕌", "🛕", "⛩️", "🗿", "🗽", "🗼", "🏰",
            "🌈", "🌀", "💫", "🌊", "🌋", "🏔️", "❄️", "🌤️", "⛅", "🌧️",
            "⚡", "🌪️", "🌩️", "🔱", "⚜️", "🏵️", "🎖️", "🧧", "🎐", "🪩"
        )
    )
)

/**
 * Smart emoji suggestion engine.
 * Given a query string, returns the most relevant emojis scored by keyword proximity.
 */
object EmojiSuggestionEngine {

    private val keywordMap: Map<String, List<String>> = buildKeywordMap()

    private fun buildKeywordMap(): Map<String, List<String>> {
        return mapOf(
            // Finance
            "money" to listOf("💰", "💵", "💸", "🤑", "💳", "🏦", "🪙"),
            "budget" to listOf("📊", "📈", "💰", "🧾", "📋", "💵", "💳"),
            "salary" to listOf("💵", "💸", "💰", "🏦", "📑", "💳", "🤑"),
            "invest" to listOf("📈", "💹", "🏆", "💎", "🏦", "📊", "💰"),
            "saving" to listOf("🏦", "💰", "🪙", "💵", "📈", "🔐", "🏆"),
            "tax" to listOf("🧾", "📑", "📋", "💵", "🏛️", "📂", "💰"),
            "bill" to listOf("🧾", "📑", "💡", "📋", "💰", "🏦", "🏠"),
            "rent" to listOf("🏠", "🔑", "🏢", "🧾", "💰", "📑", "🏡"),
            "loan" to listOf("🏦", "💰", "📋", "🤝", "💵", "🧾", "🏠"),
            "insurance" to listOf("🛡️", "🏥", "💊", "🏦", "📑", "🔐", "🛟"),

            // Food
            "food" to listOf("🍔", "🍕", "🌮", "🍜", "🥗", "🍱", "🍳"),
            "restaurant" to listOf("🍽️", "🍔", "🍷", "☕", "🍕", "🌮", "🥩"),
            "coffee" to listOf("☕", "🫖", "🍵", "🧋", "🏪", "🤎", "🫗"),
            "grocery" to listOf("🛒", "🥦", "🍎", "🥚", "🥛", "🍞", "🥩"),
            "drink" to listOf("🥤", "🍺", "🍷", "🧃", "🧋", "🍵", "☕"),
            "snack" to listOf("🍫", "🍿", "🍪", "🍩", "🥨", "🧇", "🍰"),

            // Transport
            "car" to listOf("🚗", "⛽", "🛞", "🏎️", "🚙", "🔧", "🚕"),
            "fuel" to listOf("⛽", "🚗", "🛢️", "💸", "🏍️", "🚙", "🔋"),
            "travel" to listOf("✈️", "🌍", "🧳", "🗺️", "🏨", "🚀", "🎫"),
            "flight" to listOf("✈️", "🛩️", "🌍", "🎫", "🛂", "🧳", "🏖️"),
            "train" to listOf("🚆", "🚇", "🚊", "🎫", "🗺️", "🏔️", "🚝"),
            "bus" to listOf("🚌", "🎫", "🗺️", "🚊", "🚇", "🏙️", "🚐"),
            "bike" to listOf("🚲", "🛴", "🏍️", "🚵", "🏃", "⛽", "🛵"),
            "uber" to listOf("🚕", "🚗", "📱", "💳", "🗺️", "🏙️", "🚙"),

            // Home
            "home" to listOf("🏠", "🔑", "🛋️", "🪟", "🚪", "🏡", "🏘️"),
            "electric" to listOf("💡", "⚡", "🔌", "🔋", "🏠", "🧾", "📊"),
            "water" to listOf("🚿", "💧", "🪣", "🏠", "🧾", "🌊", "🫗"),
            "repair" to listOf("🔧", "🛠️", "🔨", "🪛", "🔩", "🏠", "⚒️"),
            "furniture" to listOf("🛋️", "🪑", "🛏️", "🪞", "🏠", "🛒", "🛍️"),
            "cleaning" to listOf("🧹", "🧺", "🧼", "🫧", "🧽", "🧴", "🪥"),

            // Health
            "health" to listOf("💊", "🩺", "🏥", "❤️", "🧬", "💉", "🩻"),
            "doctor" to listOf("🩺", "🏥", "💊", "🩻", "👨‍⚕️", "🔬", "💉"),
            "medicine" to listOf("💊", "💉", "🩹", "🏥", "🔬", "🧪", "🩺"),
            "gym" to listOf("🏋️", "💪", "🧘", "⚽", "🏃", "🥊", "🎽"),
            "yoga" to listOf("🧘", "🧘‍♀️", "🌿", "☮️", "🏃", "💪", "🌅"),

            // Tech
            "phone" to listOf("📱", "📲", "💻", "☎️", "📡", "🔋", "🎧"),
            "computer" to listOf("💻", "🖥️", "⌨️", "🖱️", "📱", "🖨️", "💾"),
            "internet" to listOf("📡", "🌐", "💻", "📱", "🔗", "🛜", "📶"),
            "software" to listOf("💻", "📱", "🖥️", "🧑‍💻", "🔧", "💡", "⚙️"),
            "gaming" to listOf("🎮", "🕹️", "🎯", "🎲", "👾", "🏆", "⚔️"),
            "subscription" to listOf("📺", "🎵", "📱", "💳", "🔔", "📋", "🔄"),

            // Education
            "school" to listOf("🎓", "📚", "🏫", "✏️", "📝", "🖊️", "📐"),
            "book" to listOf("📚", "📖", "📝", "🎓", "🏫", "📘", "📙"),
            "course" to listOf("🎓", "📚", "💻", "📝", "🏫", "🖊️", "📊"),
            "study" to listOf("📚", "✏️", "🎓", "📝", "🏫", "🖊️", "📖"),

            // Entertainment
            "movie" to listOf("🎬", "🍿", "🎞️", "📽️", "🎭", "🎟️", "📺"),
            "music" to listOf("🎵", "🎧", "🎤", "🎸", "🎷", "🎹", "🥁"),
            "concert" to listOf("🎤", "🎵", "🎸", "🏟️", "🎟️", "🎊", "🎧"),
            "game" to listOf("🎮", "🕹️", "🎯", "🎲", "🃏", "⚽", "🏆"),
            "party" to listOf("🎉", "🎊", "🎈", "🥂", "🍕", "🎁", "🎂"),
            "gift" to listOf("🎁", "🎀", "🎊", "🎈", "🛍️", "💝", "🎉"),

            // Shopping
            "shop" to listOf("🛒", "🛍️", "💳", "🏪", "💰", "🏷️", "🎁"),
            "clothes" to listOf("👗", "👕", "👖", "👟", "👚", "🧥", "👔"),
            "shoes" to listOf("👟", "👠", "👡", "👢", "🥾", "🩴", "🧦"),

            // Projects
            "project" to listOf("📁", "📂", "🗂️", "💼", "🎯", "📋", "🚀"),
            "work" to listOf("💼", "🏢", "📊", "💻", "📋", "🎯", "⚙️"),
            "personal" to listOf("🏠", "👤", "❤️", "🌟", "💫", "🌈", "✨"),
            "family" to listOf("👪", "🏠", "❤️", "👶", "🤝", "🌸", "💝"),
            "vacation" to listOf("✈️", "🏖️", "🧳", "🌍", "🗺️", "🏝️", "☀️"),
            "wedding" to listOf("💍", "💒", "💐", "🥂", "🎊", "👗", "🤵"),
            "birthday" to listOf("🎂", "🎁", "🎈", "🎉", "🥳", "🎊", "🍰"),
            "holiday" to listOf("🎄", "🎅", "🎁", "❄️", "⛄", "🌟", "🎊"),
        )
    }

    private val mostUsedEmojis = mutableListOf<String>()

    fun recordUsage(emoji: String) {
        mostUsedEmojis.remove(emoji)
        mostUsedEmojis.add(0, emoji)
        if (mostUsedEmojis.size > 15) {
            mostUsedEmojis.removeAt(mostUsedEmojis.size - 1)
        }
    }

    fun getPopularEmojis(): List<String> {
        return (mostUsedEmojis + listOf("💰", "🛒", "🏠", "🍕", "🚗", "💼", "💊", "🎓", "🎬", "🎁")).distinct().take(10)
    }

    /**
     * Returns ranked emoji suggestions for the given query.
     * Falls back to popular emojis if no query is provided.
     */
    fun suggest(query: String, maxResults: Int = 20): List<String> {
        if (query.isBlank()) return getPopularEmojis()

        val normalizedQuery = query.lowercase().trim()

        // Score each keyword based on match quality
        val scored = keywordMap
            .entries
            .mapNotNull { (keyword, emojis) ->
                val score = when {
                    keyword == normalizedQuery -> 100
                    normalizedQuery.startsWith(keyword) -> 80
                    keyword.startsWith(normalizedQuery) -> 70
                    normalizedQuery.contains(keyword) -> 60
                    keyword.contains(normalizedQuery) -> 50
                    normalizedQuery.split(" ").any { keyword.contains(it) && it.length > 2 } -> 30
                    else -> null
                }
                score?.let { it to emojis }
            }
            .sortedByDescending { it.first }

        if (scored.isEmpty()) return emptyList()

        // Deduplicate while preserving order
        val seen = mutableSetOf<String>()
        return scored
            .flatMap { it.second }
            .filter { seen.add(it) }
            .take(maxResults)
    }
}