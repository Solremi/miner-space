package fr.solremi.minerspace.shared

@JvmInline
value class GameId private constructor(val value: String) {
    companion object {
        private val format = Regex("^[a-z0-9]+(?:_[a-z0-9]+)*$")

        fun of(value: String): GameId {
            require(format.matches(value)) {
                "An identifier must use lowercase ASCII words separated by underscores: $value"
            }
            return GameId(value)
        }
    }

    override fun toString(): String = value
}
