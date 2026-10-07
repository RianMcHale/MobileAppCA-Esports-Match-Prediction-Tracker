package org.setu.esportstracker.matches

interface MatchStore {
    fun findAll(): List<EsportsMatch>
    fun findOne(id: Long): EsportsMatch?
}