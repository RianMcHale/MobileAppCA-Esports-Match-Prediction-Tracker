package org.setu.esportstracker.matches

enum class MatchCategory(val apiPath: String) {
    UPCOMING("upcoming"),
    LIVE("running"),
    COMPLETED("past")
}