package org.setu.esportstracker.matches

// connects each tab to the matching pandascore url path.
enum class MatchCategory(val apiPath: String) {
    UPCOMING("upcoming"), // matches that have not started.
    LIVE("running"), // matches being played now.
    COMPLETED("past") // matches that have already ended.
}