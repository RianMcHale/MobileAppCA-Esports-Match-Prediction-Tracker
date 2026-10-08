package org.setu.esportstracker.matches

interface MatchStore {
    fun findAll(): List<EsportsMatch>
    fun findOne(id: Long): EsportsMatch? // finds one by match id - nulll if it doesnt exist
}