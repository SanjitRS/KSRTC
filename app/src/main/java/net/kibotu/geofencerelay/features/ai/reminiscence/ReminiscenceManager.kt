package net.kibotu.geofencerelay.features.ai.reminiscence

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class MemoryCard(
    val id: String,
    val title: String,
    val category: String,          // Theme
    val question: String,
    val cueText: String,
    val iconEmoji: String,
    val options: List<String>,
    val correctIndex: Int,
    val isCustom: Boolean = false
)

object ReminiscenceManager {

    val defaultThemes = listOf(
        Pair("Family & Grandchildren", "👨‍👩‍👧‍👦"),
        Pair("Hometown & Childhood", "🏡"),
        Pair("Traditional Food & Recipes", "🍛"),
        Pair("Festivals & Celebrations", "🌸"),
        Pair("Favorite Songs & Hobbies", "🎵"),
        Pair("Daily Life & Pets", "🐕")
    )

    // Default memory cards removed: Vault starts empty and is populated from first by family members!
    private val defaultCards = emptyList<MemoryCard>()

    private val json = Json { ignoreUnknownKeys = true }

    fun loadAllCards(context: Context): List<MemoryCard> {
        val prefs = context.getSharedPreferences("reminiscence_prefs", Context.MODE_PRIVATE)
        val customCardsJson = prefs.getString("custom_cards", null)
        val customCards: List<MemoryCard> = if (!customCardsJson.isNullOrBlank()) {
            try {
                json.decodeFromString<List<MemoryCard>>(customCardsJson)
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
        return customCards
    }

    fun saveCustomCard(context: Context, card: MemoryCard) {
        val prefs = context.getSharedPreferences("reminiscence_prefs", Context.MODE_PRIVATE)
        val existing = loadAllCards(context).toMutableList()
        existing.add(card)
        val serialized = json.encodeToString(existing)
        prefs.edit().putString("custom_cards", serialized).apply()
    }

    fun clearAllCards(context: Context) {
        val prefs = context.getSharedPreferences("reminiscence_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove("custom_cards").apply()
    }
}
