package com.donatienthorez.ugandai

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.ugandai.ugandai.data.api.TokenResponse
import com.ugandai.ugandai.data.api.authorizationValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ApiContractHardeningTest {
    @Test
    fun tokenResponseParsesCanonicalAuthContract() {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val token = moshi.adapter(TokenResponse::class.java)
            .fromJson("""{"access_token":"jwt-value","token_type":"bearer"}""")
        assertEquals("jwt-value", token?.access_token)
        assertEquals("bearer", token?.token_type)
    }

    @Test
    fun bearerHeaderRejectsMissingOrBlankTokens() {
        assertEquals("Bearer jwt-value", authorizationValue("jwt-value"))
        assertNull(authorizationValue(""))
        assertNull(authorizationValue(null))
    }
}
