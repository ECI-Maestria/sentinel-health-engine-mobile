package com.example.tesisv3.network

/**
 * Single source of truth for all backend base URLs.
 * Change the domain here and it propagates everywhere.
 */
object ApiConstants {
    private const val BASE_DOMAIN = "yellowmeadow-4dfba13a.centralus.azurecontainerapps.io"

    const val USER_SERVICE      = "https://user-service.$BASE_DOMAIN"
    const val ANALYTICS_SERVICE = "https://analytics-service.$BASE_DOMAIN"
    const val CALENDAR_SERVICE  = "https://calendar-service.$BASE_DOMAIN"
}
