package fr.solremi.minerspace.shared

interface GameLogger {
    fun debug(tag: String, message: String)
    fun info(tag: String, message: String)
    fun warning(tag: String, message: String, cause: Throwable? = null)
    fun error(tag: String, message: String, cause: Throwable? = null)
}

object SilentGameLogger : GameLogger {
    override fun debug(tag: String, message: String) = Unit
    override fun info(tag: String, message: String) = Unit
    override fun warning(tag: String, message: String, cause: Throwable?) = Unit
    override fun error(tag: String, message: String, cause: Throwable?) = Unit
}
