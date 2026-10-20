package br.com.fiap.letssign.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

data class RemoteSignerDto(
    val id: Long?,
    val fullName: String,
    val email: String,
    val phone: String,
    val legalDocument: String?
)

data class RemoteDispatchDocumentRequest(
    val idempotencyKey: String,
    val title: String,
    val originalSha256: String,
    val invitationMessage: String,
    val version: Int,
    val parentDocumentId: Long?,
    val signers: List<RemoteDocumentSignerDto>,
    val fields: List<RemoteSignatureFieldDto>
)

data class RemoteDocumentSignerDto(
    val signerUserId: Long,
    val classification: String,
    val signatureType: String,
    val isMandatory: Boolean
)

data class RemoteSignatureFieldDto(
    val signerUserId: Long,
    val pageIndex: Int,
    val xRatio: Float,
    val yRatio: Float,
    val widthRatio: Float,
    val heightRatio: Float
)

data class RemoteDocumentResponse(
    val id: Long,
    val status: String,
    val currentSha256: String
)

/**
 * Interface Retrofit preparada para integração futura via API REST (Sprint 2+),
 * mantendo os Repositórios desacoplados da fonte de dados local (Room).
 */
interface LetsSignApiService {

    @GET("api/v1/signers")
    suspend fun getSigners(): Response<List<RemoteSignerDto>>

    @POST("api/v1/signers")
    suspend fun createSigner(@Body signer: RemoteSignerDto): Response<RemoteSignerDto>

    @POST("api/v1/documents/dispatch")
    suspend fun dispatchDocument(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: RemoteDispatchDocumentRequest
    ): Response<RemoteDocumentResponse>

    @GET("api/v1/documents/{id}")
    suspend fun getDocumentStatus(@Path("id") documentId: Long): Response<RemoteDocumentResponse>
}
