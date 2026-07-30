package fr.solremi.minerspace.shared

sealed interface GameResult<out T> {
    data class Success<T>(val value: T) : GameResult<T>
    data class Failure(
        val code: String,
        val message: String,
        val cause: Throwable? = null,
    ) : GameResult<Nothing>
}
