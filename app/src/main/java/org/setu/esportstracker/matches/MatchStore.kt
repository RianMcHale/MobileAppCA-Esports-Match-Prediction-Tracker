package org.setu.esportstracker.matches

// lists the basic actions every match store must provide.
interface MatchStore {
    // gets every match currently held by the store.
    fun findAll(): List<EsportsMatch>

    // gets one match by id, or null if it does not exist.
    fun findOne(id: Long): EsportsMatch?
}