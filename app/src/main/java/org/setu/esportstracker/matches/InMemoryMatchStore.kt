package org.setu.esportstracker.matches

class InMemoryMatchStore : MatchStore {

    // Fixed examples from HLTV.org @ 4.54pm
    private val matches = listOf(
        // Sample 1: SAW vs Famalicão
        EsportsMatch(
            id = 2399028L,
            teamOneId = 10567L,
            teamOneName = "SAW",
            teamTwoId = 13364L,
            teamTwoName = "Famalicão",
            game = "CS2",
            competition = "Roman Imperium Cup IX",
            startTimeUtcMillis = 1_791_389_700_000L
        ),

        // Sample 2: RBLS vs NAVI Junior
        EsportsMatch(
            id = 2399029L,
            teamOneId = 12642L,
            teamOneName = "RBLS",
            teamTwoId = 10371L,
            teamTwoName = "NAVI Junior",
            game = "CS2",
            competition = "Roman Imperium Cup IX",
            startTimeUtcMillis = 1_791_389_700_000L
        ),

        // Sample 3: Spirit vs M80
        EsportsMatch(
            id = 2398749L,
            teamOneId = 7020L,
            teamOneName = "Spirit",
            teamTwoId = 12376L,
            teamTwoName = "M80",
            game = "CS2",
            competition = "ESL Pro League Season 24",
            startTimeUtcMillis = 1_791_392_400_000L
        )
    )

    override fun findAll(): List<EsportsMatch> = matches.toList()

    override fun findOne(id: Long): EsportsMatch? =
        matches.find { it.id == id }
}