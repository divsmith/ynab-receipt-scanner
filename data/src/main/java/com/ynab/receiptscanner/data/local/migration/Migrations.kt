package com.ynab.receiptscanner.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Database migration definitions
 * Each migration handles schema changes between versions
 */
object Migrations {
    
    /**
     * Example migration from version 1 to 2
     * Currently empty as we're on version 1
     * Add migrations here as schema evolves
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Example: Add a new column to receipts table
            // database.execSQL("ALTER TABLE receipts ADD COLUMN notes TEXT")
        }
    }
    
    /**
     * Get all migrations for the database
     * Add new migrations to this array as they are created
     */
    fun getAllMigrations(): Array<Migration> {
        return arrayOf(
            // MIGRATION_1_2 // Uncomment when ready to use
        )
    }
}
