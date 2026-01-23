package de.readeckapp.io.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import de.readeckapp.io.db.model.BookmarkEntity
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import de.readeckapp.io.db.model.ArticleContentEntity
import de.readeckapp.io.db.model.BookmarkWithArticleContent
import de.readeckapp.io.db.model.BookmarkListItemEntity
import de.readeckapp.io.db.model.RemoteBookmarkIdEntity
import de.readeckapp.io.db.model.BookmarkCountsEntity

@Dao
interface BookmarkDao {
    @Query("SELECT * from bookmarks")
    suspend fun getBookmarks(): List<BookmarkEntity>

    @Query("SELECT * FROM bookmarks ORDER BY created DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * from bookmarks WHERE type = 'picture'")
    fun getPictures(): Flow<List<BookmarkEntity>>

    @Query("SELECT * from bookmarks WHERE type = 'video'")
    fun getVideos(): Flow<List<BookmarkEntity>>

    @Query("SELECT * from bookmarks WHERE type = 'article'")
    fun getArticles(): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmarks(bookmarks: List<BookmarkEntity>)

    @Transaction
    suspend fun insertBookmarkWithArticleContent(bookmarkWithArticleContent: BookmarkWithArticleContent) {
        with(bookmarkWithArticleContent) {
            insertBookmark(bookmark)
            articleContent?.run { insertArticleContent(this) }
        }
    }

    @Transaction
    suspend fun insertBookmarksWithArticleContent(bookmarks: List<BookmarkWithArticleContent>) {
        bookmarks.forEach {
            insertBookmarkWithArticleContent(it)
        }
    }

    @Insert(onConflict = REPLACE)
    suspend fun insertBookmark(bookmarkEntity: BookmarkEntity)

    @Query("SELECT * FROM bookmarks ORDER BY updated DESC LIMIT 1")
    suspend fun getLastUpdatedBookmark(): BookmarkEntity?

    @Query("SELECT * FROM bookmarks WHERE id = :id")
    suspend fun getBookmarkById(id: String): BookmarkEntity

    @Query("SELECT * FROM bookmarks WHERE id = :id")
    fun observeBookmark(id: String): Flow<BookmarkEntity?>

    @Query("DELETE FROM bookmarks")
    suspend fun deleteAllBookmarks()

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmark(id: String)

    @RawQuery(observedEntities = [BookmarkEntity::class])
    fun getBookmarksByFiltersDynamic(query: SupportSQLiteQuery): Flow<List<BookmarkEntity>>

    @RawQuery(observedEntities = [BookmarkEntity::class])
    fun getBookmarkListItemsByFiltersDynamic(query: SupportSQLiteQuery): Flow<List<BookmarkListItemEntity>>

    fun getBookmarksByFilters(
        type: BookmarkEntity.Type? = null,
        isUnread: Boolean? = null,
        isArchived: Boolean? = null,
        isFavorite: Boolean? = null,
        state: BookmarkEntity.State? = null
    ): Flow<List<BookmarkEntity>> {
        val args = mutableListOf<Any>()
        val sqlQuery = buildString {
            append("SELECT * FROM bookmarks WHERE 1=1")

            state?.let {
                append(" AND state = ?")
                args.add(it.value)
            }

            type?.let {
                append(" AND type = ?")
                args.add(it.value)
            }

            if (isUnread == true) {
                append(" AND readProgress < 100")
            } else if (isUnread == false) {
                append(" AND readProgress = 100")
            }

            isArchived?.let {
                append(" AND isArchived = ?")
                args.add(it)
            }

            isFavorite?.let {
                append(" AND isMarked = ?")
                args.add(it)
            }
            append(" ORDER BY created DESC")
        }.let { SimpleSQLiteQuery(it, args.toTypedArray()) }
        Timber.d("query=${sqlQuery.sql}")
        return getBookmarksByFiltersDynamic(sqlQuery)
    }

    @Query("SELECT content FROM article_content WHERE bookmarkId = :bookmarkId")
    suspend fun getArticleContent(bookmarkId: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticleContent(articleContent: ArticleContentEntity): Unit

    @Query("DELETE FROM article_content WHERE bookmarkId = :bookmarkId")
    suspend fun deleteArticleContent(bookmarkId: String)

    @Query("SELECT * FROM bookmarks")
    suspend fun getAllBookmarksWithContent(): List<BookmarkWithArticleContent>

    @Query("SELECT * FROM bookmarks WHERE id = :id")
    fun observeBookmarkWithArticleContent(id: String): Flow<BookmarkWithArticleContent?>

    fun getBookmarkListItemsByFilters(
        type: BookmarkEntity.Type? = null,
        isUnread: Boolean? = null,
        isArchived: Boolean? = null,
        isFavorite: Boolean? = null,
        state: BookmarkEntity.State? = null,
        label: String? = null
    ): Flow<List<BookmarkListItemEntity>> {
        val args = mutableListOf<Any>()
        val sqlQuery = buildString {
            append("""SELECT
            id,
            url,
            title,
            siteName,
            isMarked,
            isArchived,
            readProgress,
            icon_src AS iconSrc,
            image_src AS imageSrc,
            labels,
            thumbnail_src AS thumbnailSrc,
            type
            """)

            append(" FROM bookmarks WHERE 1=1")

            state?.let {
                append(" AND state = ?")
                args.add(it.value)
            }

            type?.let {
                append(" AND type = ?")
                args.add(it.value)
            }

            if (isUnread == true) {
                append(" AND readProgress < 100")
            } else if (isUnread == false) {
                append(" AND readProgress = 100")
            }

            isArchived?.let {
                append(" AND isArchived = ?")
                args.add(it)
            }

            isFavorite?.let {
                append(" AND isMarked = ?")
                args.add(it)
            }

            label?.let {
                // Exact label match: wrap labels with commas and search for exact label
                // This prevents "ai" from matching "email" or "sailing"
                append(" AND (',' || labels || ',') LIKE ?")
                args.add("%,$it,%")
            }

            append(" ORDER BY created DESC")
        }.let { SimpleSQLiteQuery(it, args.toTypedArray()) }
        Timber.d("query=${sqlQuery.sql}")
        return getBookmarkListItemsByFiltersDynamic(sqlQuery)
    }

    fun searchBookmarkListItems(
        searchQuery: String,
        type: BookmarkEntity.Type? = null,
        isUnread: Boolean? = null,
        isArchived: Boolean? = null,
        isFavorite: Boolean? = null,
        state: BookmarkEntity.State? = null,
        hasArticleContent: Boolean? = null,
        requiresArticle: Boolean = false
    ): Flow<List<BookmarkListItemEntity>> {
        val args = mutableListOf<Any>()
        val sqlQuery = buildString {
            append("""SELECT
            b.id,
            b.url,
            b.title,
            b.siteName,
            b.isMarked,
            b.isArchived,
            b.readProgress,
            b.icon_src AS iconSrc,
            b.image_src AS imageSrc,
            b.labels,
            b.thumbnail_src AS thumbnailSrc,
            b.type
            """)

            append(" FROM bookmarks b")

            // LEFT JOIN with article_content to check for content existence
            append(" LEFT JOIN article_content ac ON b.id = ac.bookmarkId")

            append(" WHERE 1=1")

            // Add search condition for title and labels - only if searchQuery is not blank
            if (searchQuery.isNotBlank()) {
                append(" AND (b.title LIKE ? COLLATE NOCASE OR b.labels LIKE ? COLLATE NOCASE OR b.siteName LIKE ? COLLATE NOCASE)")
                val searchPattern = "%$searchQuery%"
                args.add(searchPattern)
                args.add(searchPattern)
                args.add(searchPattern)
            }

            state?.let {
                append(" AND b.state = ?")
                args.add(it.value)
            }

            type?.let {
                append(" AND b.type = ?")
                args.add(it.value)
            }

            if (isUnread == true) {
                append(" AND b.readProgress < 100")
            } else if (isUnread == false) {
                append(" AND b.readProgress = 100")
            }

            isArchived?.let {
                append(" AND b.isArchived = ?")
                args.add(it)
            }

            isFavorite?.let {
                append(" AND b.isMarked = ?")
                args.add(it)
            }

            // Handle article content filtering
            when {
                // Special case for is:empty - bookmarks that should have content but don't
                requiresArticle -> {
                    append(" AND b.hasArticle = 1 AND (ac.content IS NULL OR ac.content = '')")
                }
                hasArticleContent == true -> {
                    append(" AND ac.content IS NOT NULL AND ac.content != ''")
                }
                hasArticleContent == false -> {
                    append(" AND (ac.content IS NULL OR ac.content = '')")
                }
            }

            append(" ORDER BY b.created DESC")
        }.let { SimpleSQLiteQuery(it, args.toTypedArray()) }
        Timber.d("searchQuery=${sqlQuery.sql}")
        return getBookmarkListItemsByFiltersDynamic(sqlQuery)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRemoteBookmarkIds(ids: List<RemoteBookmarkIdEntity>)

    @Query("DELETE FROM remote_bookmark_ids")
    suspend fun clearRemoteBookmarkIds()

    @Query("SELECT id FROM remote_bookmark_ids")
    suspend fun getAllRemoteBookmarkIds(): List<String>

    @Query(
        """
            DELETE FROM bookmarks
            WHERE NOT EXISTS (SELECT 1 FROM remote_bookmark_ids WHERE bookmarks.id = remote_bookmark_ids.id)
        """
    )
    suspend fun removeDeletedBookmars(): Int

    @Query(
        """
        SELECT
            (SELECT COUNT(*) FROM bookmarks WHERE readProgress < 100 AND state = 0) AS unread_count,
            (SELECT COUNT(*) FROM bookmarks WHERE isArchived = 1 AND state = 0) AS archived_count,
            (SELECT COUNT(*) FROM bookmarks WHERE isMarked = 1 AND state = 0) AS favorite_count,
            (SELECT COUNT(*) FROM bookmarks WHERE type = 'article' AND state = 0) AS article_count,
            (SELECT COUNT(*) FROM bookmarks WHERE type = 'video' AND state = 0) AS video_count,
            (SELECT COUNT(*) FROM bookmarks WHERE type = 'photo' AND state = 0) AS picture_count,
            (SELECT COUNT(*) FROM bookmarks WHERE state = 0) AS total_count
        FROM bookmarks
        LIMIT 1
        """
    )
    fun observeAllBookmarkCounts(): Flow<BookmarkCountsEntity?>

    @Query(
        """
        SELECT labels FROM bookmarks WHERE state = 0 AND labels != '' AND labels IS NOT NULL
        """
    )
    suspend fun getAllLabels(): List<String>

    @Query(
        """
        SELECT labels FROM bookmarks WHERE state = 0 AND labels != '' AND labels IS NOT NULL
        """
    )
    fun observeAllLabels(): Flow<List<String>>
}
