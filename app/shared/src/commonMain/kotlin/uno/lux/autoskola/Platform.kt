package uno.lux.autoskola

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
