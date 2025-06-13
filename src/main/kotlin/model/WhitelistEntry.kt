package net.eupixel.model

import java.time.Instant

data class WhitelistEntry(val ip: String, val ttl: Int, val timestamp: Instant)