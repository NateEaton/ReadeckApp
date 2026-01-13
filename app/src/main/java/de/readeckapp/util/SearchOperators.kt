package de.readeckapp.util

import de.readeckapp.domain.model.Bookmark

/**
 * Represents parsed search operators from a search query.
 * Supports Gmail-style search operators like "is:error", "has:content", etc.
 */
data class SearchOperators(
    val textQuery: String,
    val state: Bookmark.State? = null,
    val hasArticleContent: Boolean? = null
)

/**
 * Parses a search query string into operators and text query.
 *
 * Supported operators:
 * - is:error - Show bookmarks with ERROR state
 * - is:loaded - Show bookmarks with LOADED state
 * - is:loading - Show bookmarks with LOADING state
 * - is:empty - Show bookmarks that should have content (hasArticle=true) but don't
 * - has:content - Show bookmarks with article content
 * - has:no-content - Show bookmarks without article content
 *
 * Example: "is:error kubernetes" returns SearchOperators(textQuery="kubernetes", state=ERROR)
 */
fun parseSearchQuery(query: String): SearchOperators {
    val tokens = query.split("\\s+".toRegex())
    val textTokens = mutableListOf<String>()
    var state: Bookmark.State? = null
    var hasArticleContent: Boolean? = null

    for (token in tokens) {
        when {
            token.equals("is:error", ignoreCase = true) -> {
                state = Bookmark.State.ERROR
            }
            token.equals("is:loaded", ignoreCase = true) -> {
                state = Bookmark.State.LOADED
            }
            token.equals("is:loading", ignoreCase = true) -> {
                state = Bookmark.State.LOADING
            }
            token.equals("is:empty", ignoreCase = true) -> {
                // Empty means: hasArticle=true BUT no content exists
                // This is handled specially in the DAO query
                hasArticleContent = false
            }
            token.equals("has:content", ignoreCase = true) -> {
                hasArticleContent = true
            }
            token.equals("has:no-content", ignoreCase = true) -> {
                hasArticleContent = false
            }
            token.isNotBlank() -> {
                textTokens.add(token)
            }
        }
    }

    return SearchOperators(
        textQuery = textTokens.joinToString(" "),
        state = state,
        hasArticleContent = hasArticleContent
    )
}
