package br.com.fiap.letssign.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * =================================================================================================
 * DTOs (Data Transfer Objects) para Integração com Backend / APIs REST
 * =================================================================================================
 *
 * NOTA DE ARQUITETURA & TROUBLESHOOTING:
 * Estes DTOs modelam os dados trocados entre o app Android (AssinaJÁ) e qualquer backend corporativo.
 *
 * Dica para Desenvolvedores Futuros:
 * - O app segue o padrão Local-First (Offline First). O Room Database é a "Fonte Única da Verdade".
 * - Quando a API REST for conectada, o fluxo recomendado é:
 *     1. UI observa o Room via Kotlin Flow (`observe...`).
 *     2. ViewModel dispara ações que batem no Repositório.
 *     3. O Repositório envia a requisição HTTP (Retrofit) e grava a resposta diretamente no Room.
 *     4. O Room emite os novos dados atualizados automaticamente para a UI.
 * - Isso evita telas piscando, funciona 100% offline e sincroniza quando a conexão retornar.
 */

data class RemoteLoginRequest(
    val email: String,
    val passwordHash: String,
    val clientAppVersion: String = "2.0.0"
)

data class RemoteLoginResponse(
    val token: String,
    val refreshToken: String,
    val user: RemoteSignerDto
)

data class RemoteSignerDto(
    val id: Long?,
    val fullName: String,
    val email: String,
    val phone: String,
    val legalDocument: String?,
    val role: String = "SIGNATARIO"
)

data class RemoteDispatchDocumentRequest(
    val idempotencyKey: String,
    val title: String,
    val description: String = "",
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
    val heightRatio: Float,
    /** Tipo do campo: RUBRICA, CPF ou DATA */
    val fieldType: String = "RUBRICA"
)

data class RemoteDocumentResponse(
    val id: Long,
    val title: String,
    val status: String,
    val currentSha256: String,
    val totalSigners: Int,
    val completedSigners: Int
)

data class RemoteExecuteSignatureRequest(
    val documentId: Long,
    val signerUserId: Long,
    val signatureBase64: String?,
    val verificationToken: String?,
    val certificateInfo: String?,
    val clientIp: String,
    val userAgent: String
)

data class RemoteRejectDocumentRequest(
    val documentId: Long,
    val signerUserId: Long,
    val reason: String
)

/**
 * =================================================================================================
 * Interface Retrofit de Serviços REST do AssinaJÁ
 * =================================================================================================
 *
 * Caso queira plugar sua API REST real (Spring Boot, Node.js, FastAPI, etc.):
 * 1. Configure a `baseUrl` no seu Retrofit.Builder().
 * 2. Adicione um OkHttpClient com Interceptor para injetar o header "Authorization: Bearer <token>".
 * 3. Injete esta interface na classe `RemoteLetsSignRepositoryImpl` ou combine com `LocalLetsSignRepositoryImpl`.
 */
interface LetsSignApiService {

    @POST("api/v1/auth/login")
    suspend fun login(
        @Body request: RemoteLoginRequest
    ): Response<RemoteLoginResponse>

    @GET("api/v1/signers")
    suspend fun getSigners(): Response<List<RemoteSignerDto>>

    @POST("api/v1/signers")
    suspend fun createSigner(
        @Body signer: RemoteSignerDto
    ): Response<RemoteSignerDto>

    @POST("api/v1/documents/dispatch")
    suspend fun dispatchDocument(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: RemoteDispatchDocumentRequest
    ): Response<RemoteDocumentResponse>

    @GET("api/v1/documents/{id}")
    suspend fun getDocumentStatus(
        @Path("id") documentId: Long
    ): Response<RemoteDocumentResponse>

    @POST("api/v1/documents/{id}/sign")
    suspend fun signDocument(
        @Path("id") documentId: Long,
        @Body request: RemoteExecuteSignatureRequest
    ): Response<RemoteDocumentResponse>

    @POST("api/v1/documents/{id}/reject")
    suspend fun rejectDocument(
        @Path("id") documentId: Long,
        @Body request: RemoteRejectDocumentRequest
    ): Response<RemoteDocumentResponse>
}
