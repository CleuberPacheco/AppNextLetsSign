package br.com.fiap.letssign.data.repository

import android.content.Context
import android.graphics.Bitmap
import br.com.fiap.letssign.data.local.AuditEventEntity
import br.com.fiap.letssign.data.local.DocumentEntity
import br.com.fiap.letssign.data.local.DocumentFullDetails
import br.com.fiap.letssign.data.local.DocumentSignerEntity
import br.com.fiap.letssign.data.local.LetsSignDao
import br.com.fiap.letssign.data.local.SignatureFieldEntity
import br.com.fiap.letssign.data.local.UserEntity
import br.com.fiap.letssign.data.remote.LetsSignApiService
import br.com.fiap.letssign.domain.model.AuditEventType
import br.com.fiap.letssign.domain.model.DocumentStatus
import br.com.fiap.letssign.domain.model.DraftSignatureField
import br.com.fiap.letssign.domain.model.DraftSignerSelection
import br.com.fiap.letssign.domain.model.SignerStatus
import br.com.fiap.letssign.domain.model.UserRole
import br.com.fiap.letssign.util.PdfEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * =================================================================================================
 * Contrato de Repositório do AssinaJÁ — Plataforma Corporativa de Assinatura e Gestão de Documentos
 * =================================================================================================
 *
 * GUIA DO DESENVOLVEDOR (Visão de Arquitetura & Troubleshooting):
 *
 * 1. POR QUE ESTA INTERFACE EXISTE?
 *    Esta interface isola a camada de apresentação (ViewModels e Telas Compose) dos detalhes de como
 *    e onde os dados são salvos. Ela permite que a UI nunca converse diretamente com o Room SQLite
 *    ou com chamadas de rede HTTP.
 *
 * 2. COMO MIGRAR OU INTEGRAR COM BACKEND / REST APIS NO FUTURO:
 *    - Atualmente, usamos `LocalLetsSignRepositoryImpl`, que persiste localmente via Room SQLite.
 *    - Para conectar a um servidor REST no futuro:
 *        a) Crie uma classe `RemoteLetsSignRepositoryImpl` ou altere a implementação atual para
 *           usar o padrão "NetworkBoundResource" (Cache-First).
 *        b) Salve os dados recebidos via API diretamente no banco Room, e deixe que a UI continue
 *           apenas observando os `Flow` do Room. Assim o app mantém funcionamento offline perfeito!
 *
 * 3. TROUBLESHOOTING FREQUENTE:
 *    - Erro de Foreign Key ao disparar documento: certifique-se de que os IDs dos signatários
 *      selecionados já existem na tabela `users` antes de vincular em `document_signers`.
 *    - Hash SHA-256 divergente: O hash é recalculado a cada nova rubrica gravada. Se o PDF mudar
 *      fora do fluxo, o hash não baterá com a cadeia de custódia.
 */
interface LetsSignRepository {
    suspend fun ensureDefaultAdminExists()
    suspend fun login(email: String, password: String): Result<UserEntity>
    fun observeSigners(): Flow<List<UserEntity>>
    fun observeAllUsers(): Flow<List<UserEntity>>
    suspend fun saveSigner(
        editingId: Long?,
        fullName: String,
        email: String,
        phone: String,
        password: String,
        legalDocument: String
    ): Result<UserEntity>
    suspend fun deleteSigner(signerUserId: Long): Result<Unit>

    fun observeAllDocumentsForAdmin(): Flow<List<DocumentFullDetails>>
    fun observeDocumentsForSigner(signerUserId: Long): Flow<List<DocumentFullDetails>>
    fun observeDocumentDetails(documentId: Long): Flow<DocumentFullDetails?>
    suspend fun getDocumentDetails(documentId: Long): DocumentFullDetails?
    fun observeAllAuditEvents(): Flow<List<AuditEventEntity>>

    suspend fun dispatchDocumentForSignature(
        idempotencyKey: String,
        title: String,
        description: String,
        originalFileName: String,
        originalPdfPath: String,
        pageCount: Int,
        originalSha256: String,
        invitationMessage: String,
        adminUser: UserEntity,
        signers: List<DraftSignerSelection>,
        fields: List<DraftSignatureField>,
        version: Int = 1,
        parentDocumentId: Long? = null,
        simulateTransientFailure: Boolean = false
    ): Result<Long>

    suspend fun recordDocumentViewedBySigner(
        documentId: Long,
        signerUser: UserEntity
    )

    suspend fun recordInviteShared(
        documentId: Long,
        channel: String,
        actor: UserEntity
    )

    suspend fun signDocument(
        context: Context,
        documentId: Long,
        signerUser: UserEntity,
        signatureBitmap: Bitmap?,
        verificationToken: String?,
        certificateInfo: String?
    ): Result<DocumentFullDetails>

    suspend fun rejectDocument(
        documentId: Long,
        signerUser: UserEntity,
        reason: String
    ): Result<DocumentFullDetails>
}

class LocalLetsSignRepositoryImpl(
    private val dao: LetsSignDao,
    @Suppress("unused") private val remoteApi: LetsSignApiService? = null
) : LetsSignRepository {

    /**
     * Inicializa os dados padrão do sistema de forma idempotente e segura.
     *
     * DICA PARA QUEM ESTÁ DANDO MANUTENÇÃO / TROUBLESHOOT:
     * - Cria o usuário Administrador oficial (admin@assinaja.com.br / admin123).
     * - Cadastra automaticamente os signatários de teste requisitados:
     *     1. John McClane   (john.mcclane@assinaja.com.br / 123)
     *     2. Hans Gruber    (hans.gruber@assinaja.com.br / 123)
     *     3. Holly Gennaro  (holly.gennaro@assinaja.com.br / 123)
     * - Caso o banco SQLite local já exista em um celular ou emulador, este método
     *   verifica individualmente por e-mail e só insere quem ainda não estiver cadastrado,
     *   evitando exceções de chave única duplicada (SQLiteConstraintException).
     */
    override suspend fun ensureDefaultAdminExists() = withContext(Dispatchers.IO) {
        val assinajaAdmin = dao.getUserByEmail("admin@assinaja.com.br")
        val legacyAdmin = dao.getUserByEmail("admin@letssign.com.br")

        if (assinajaAdmin == null && legacyAdmin == null) {
            dao.insertUser(
                UserEntity(
                    fullName = "Administrador AssinaJÁ",
                    email = "admin@assinaja.com.br",
                    phone = "(11) 98765-4321",
                    password = "admin123",
                    legalDocument = "00.000.000/0001-91",
                    role = UserRole.ADMIN
                )
            )
        } else if (legacyAdmin != null && assinajaAdmin == null) {
            dao.updateUser(
                legacyAdmin.copy(
                    fullName = "Administrador AssinaJÁ",
                    email = "admin@assinaja.com.br",
                    password = "admin123"
                )
            )
        }

        // Signatários de homologação pré-cadastrados (Nakatomi Plaza / AssinaJÁ)
        val defaultSigners = listOf(
            UserEntity(
                fullName = "John McClane",
                email = "john.mcclane@assinaja.com.br",
                phone = "(11) 99111-2233",
                password = "123",
                legalDocument = "111.222.333-44",
                role = UserRole.SIGNATARIO
            ),
            UserEntity(
                fullName = "Hans Gruber",
                email = "hans.gruber@assinaja.com.br",
                phone = "(11) 98888-7766",
                password = "123",
                legalDocument = "222.333.444-55",
                role = UserRole.SIGNATARIO
            ),
            UserEntity(
                fullName = "Holly Gennaro",
                email = "holly.gennaro@assinaja.com.br",
                phone = "(11) 97777-6655",
                password = "123",
                legalDocument = "333.444.555-66",
                role = UserRole.SIGNATARIO
            )
        )

        for (signer in defaultSigners) {
            val existing = dao.getUserByEmail(signer.email)
            if (existing == null) {
                dao.insertUser(signer)
            }
        }

        // Limpeza imediata de signatários removidos a pedido (Carlos Eduardo Mendes e Bruno Zamboni)
        dao.purgeLegacyFields()
        dao.purgeLegacyDocumentSigners()
        dao.purgeLegacyUsers()
        Unit
    }

    override suspend fun login(email: String, password: String): Result<UserEntity> =
        withContext(Dispatchers.IO) {
            val cleanEmail = email.trim()
            val cleanPassword = password.trim()
            if (cleanEmail.isBlank() || cleanPassword.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Preencha o e-mail e a senha para autenticar."))
            }

            // Se for credencial de admin padrão, assegura sincronização imediata
            if ((cleanEmail.equals("admin@assinaja.com.br", ignoreCase = true) ||
                 cleanEmail.equals("admin@letssign.com.br", ignoreCase = true)) &&
                cleanPassword == "admin123"
            ) {
                ensureDefaultAdminExists()
            }

            var user = dao.authenticate(cleanEmail, cleanPassword)

            // Compatibilidade bidirecional entre admin novo e anterior
            if (user == null && cleanPassword == "admin123") {
                if (cleanEmail.equals("admin@assinaja.com.br", ignoreCase = true)) {
                    user = dao.authenticate("admin@letssign.com.br", cleanPassword)
                } else if (cleanEmail.equals("admin@letssign.com.br", ignoreCase = true)) {
                    user = dao.authenticate("admin@assinaja.com.br", cleanPassword)
                }
            }

            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(IllegalArgumentException("Credenciais inválidas. Verifique o e-mail e a senha informados."))
            }
        }

    override fun observeSigners(): Flow<List<UserEntity>> =
        dao.observeUsersByRole(UserRole.SIGNATARIO)

    override fun observeAllUsers(): Flow<List<UserEntity>> =
        dao.observeAllUsers()

    override fun observeAllAuditEvents(): Flow<List<AuditEventEntity>> =
        dao.observeAllAuditEvents()

    override suspend fun saveSigner(
        editingId: Long?,
        fullName: String,
        email: String,
        phone: String,
        password: String,
        legalDocument: String
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        val cleanName = fullName.trim()
        val cleanEmail = email.trim().lowercase()
        val cleanPhone = phone.trim()
        val cleanPassword = password.trim()
        val cleanDoc = legalDocument.trim()

        if (cleanName.length < 3 || !cleanName.contains(" ")) {
            return@withContext Result.failure(
                IllegalArgumentException("Informe o nome completo do signatário (nome e sobrenome).")
            )
        }
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            return@withContext Result.failure(
                IllegalArgumentException("Informe um endereço de e-mail válido para credencial de autenticação.")
            )
        }
        if (cleanPhone.count { it.isDigit() } < 8) {
            return@withContext Result.failure(
                IllegalArgumentException("Informe um telefone válido com DDD (mínimo 8 dígitos).")
            )
        }
        if (cleanPassword.length < 4) {
            return@withContext Result.failure(
                IllegalArgumentException("A senha de autenticação deve possuir pelo menos 4 caracteres.")
            )
        }

        val existingByEmail = dao.getUserByEmail(cleanEmail)
        if (existingByEmail != null && existingByEmail.id != editingId) {
            return@withContext Result.failure(
                IllegalArgumentException("Já existe um signatário cadastrado com o e-mail '$cleanEmail'.")
            )
        }

        if (editingId == null || editingId == 0L) {
            val newEntity = UserEntity(
                fullName = cleanName,
                email = cleanEmail,
                phone = cleanPhone,
                password = cleanPassword,
                legalDocument = cleanDoc,
                role = UserRole.SIGNATARIO
            )
            val id = dao.insertUser(newEntity)
            Result.success(newEntity.copy(id = id))
        } else {
            val current = dao.getUserById(editingId)
                ?: return@withContext Result.failure(IllegalArgumentException("Signatário não encontrado para edição."))
            val updated = current.copy(
                fullName = cleanName,
                email = cleanEmail,
                phone = cleanPhone,
                password = cleanPassword,
                legalDocument = cleanDoc
            )
            dao.updateUser(updated)
            dao.syncPendingSignerInfo(editingId, cleanName, cleanEmail, cleanPhone)
            dao.syncPendingFieldSignerName(editingId, cleanName)
            Result.success(updated)
        }
    }

    override suspend fun deleteSigner(signerUserId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = dao.getUserById(signerUserId)
                ?: return@withContext Result.failure(IllegalArgumentException("Signatário não encontrado."))
            if (user.role == UserRole.ADMIN) {
                return@withContext Result.failure(IllegalStateException("Não é permitido excluir um Administrador."))
            }
            dao.deleteSignatureFieldsBySignerUserId(signerUserId)
            dao.deleteDocumentSignersBySignerUserId(signerUserId)
            dao.deleteUserById(signerUserId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeAllDocumentsForAdmin(): Flow<List<DocumentFullDetails>> =
        dao.observeAllDocumentsWithDetails()

    override fun observeDocumentsForSigner(signerUserId: Long): Flow<List<DocumentFullDetails>> =
        dao.observeDocumentsForSigner(signerUserId)

    override fun observeDocumentDetails(documentId: Long): Flow<DocumentFullDetails?> =
        dao.observeDocumentDetails(documentId)

    override suspend fun getDocumentDetails(documentId: Long): DocumentFullDetails? =
        withContext(Dispatchers.IO) {
            dao.getDocumentDetails(documentId)
        }

    override suspend fun dispatchDocumentForSignature(
        idempotencyKey: String,
        title: String,
        description: String,
        originalFileName: String,
        originalPdfPath: String,
        pageCount: Int,
        originalSha256: String,
        invitationMessage: String,
        adminUser: UserEntity,
        signers: List<DraftSignerSelection>,
        fields: List<DraftSignatureField>,
        version: Int,
        parentDocumentId: Long?,
        simulateTransientFailure: Boolean
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            // 1. Controle de Idempotência: evita duplicar solicitações em caso de nova tentativa após falha
            val alreadyDispatched = dao.getDocumentByIdempotencyKey(idempotencyKey)
            if (alreadyDispatched != null) {
                return@withContext Result.success(alreadyDispatched.id)
            }

            if (simulateTransientFailure) {
                return@withContext Result.failure(
                    IllegalStateException(
                        "Falha temporária na comunicação ao registrar o disparo (Simulação de Erro). " +
                            "Suas configurações estão seguras — toque em 'Tentar Novamente' para reenviar sem duplicar a solicitação."
                    )
                )
            }

            // 2. Validação das Regras de Negócio Obrigatórias
            val cleanTitle = title.trim()
            if (cleanTitle.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("O título do documento é obrigatório."))
            }
            if (originalPdfPath.isBlank() || pageCount <= 0) {
                return@withContext Result.failure(IllegalArgumentException("Selecione um arquivo PDF válido antes do envio."))
            }
            if (signers.isEmpty()) {
                return@withContext Result.failure(
                    IllegalArgumentException("Regra de Negócio: Um documento precisa ter pelo menos 1 signatário obrigatório para ser enviado.")
                )
            }
            if (fields.isEmpty()) {
                return@withContext Result.failure(
                    IllegalArgumentException("Adicione e posicione pelo menos 1 campo de assinatura nas páginas do PDF.")
                )
            }

            // Verifica integridade dos campos
            val signerIds = signers.map { it.userId }.toSet()
            for (field in fields) {
                if (field.signerUserId !in signerIds) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Cada campo de assinatura deve estar associado a um participante válido do documento.")
                    )
                }
                if (field.pageIndex !in 0 until pageCount) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Campo de assinatura posicionado em página inexistente do PDF.")
                    )
                }
            }
            val signersWithoutFields = signers.filter { s -> fields.none { it.signerUserId == s.userId } }
            if (signersWithoutFields.isNotEmpty()) {
                val names = signersWithoutFields.joinToString(", ") { it.fullName }
                return@withContext Result.failure(
                    IllegalArgumentException("Posicione pelo menos 1 campo de assinatura no PDF para: $names.")
                )
            }

            val now = System.currentTimeMillis()
            val documentEntity = DocumentEntity(
                title = cleanTitle,
                description = description.trim(),
                originalFileName = originalFileName,
                originalPdfPath = originalPdfPath,
                currentPdfPath = originalPdfPath,
                pageCount = pageCount,
                originalSha256 = originalSha256,
                currentSha256 = originalSha256,
                invitationMessage = invitationMessage.trim().ifBlank {
                    "Encaminhamos este documento para conferência e assinatura eletrônica/digital na plataforma AssinaJÁ."
                },
                status = DocumentStatus.ENVIADO,
                version = version,
                parentDocumentId = parentDocumentId,
                createdByAdminId = adminUser.id,
                dispatchIdempotencyKey = idempotencyKey,
                createdAt = now,
                sentAt = now
            )

            val docId = dao.insertDocument(documentEntity)

            val signerEntities = signers.map { s ->
                DocumentSignerEntity(
                    documentId = docId,
                    signerUserId = s.userId,
                    signerName = s.fullName,
                    signerEmail = s.email,
                    signerPhone = s.phone,
                    classification = s.classification,
                    signatureType = s.signatureType,
                    isMandatory = s.isMandatory,
                    status = SignerStatus.PENDENTE
                )
            }
            dao.insertDocumentSigners(signerEntities)

            val fieldEntities = fields.map { f ->
                val clamped = f.clamped()
                SignatureFieldEntity(
                    documentId = docId,
                    signerUserId = clamped.signerUserId,
                    signerName = clamped.signerName,
                    classification = clamped.classification,
                    signatureType = clamped.signatureType,
                    pageIndex = clamped.pageIndex,
                    xRatio = clamped.xRatio,
                    yRatio = clamped.yRatio,
                    widthRatio = clamped.widthRatio,
                    heightRatio = clamped.heightRatio,
                    fieldType = clamped.fieldType,
                    isSigned = false
                )
            }
            dao.insertSignatureFields(fieldEntities)

            val initialEvents = mutableListOf(
                AuditEventEntity(
                    documentId = docId,
                    eventType = if (version > 1) AuditEventType.NOVA_VERSAO_GERADA else AuditEventType.DOCUMENTO_CRIADO,
                    title = if (version > 1) "Nova Versão Criada (v$version)" else "Documento Criado e PDF Validado",
                    description = "Arquivo '$originalFileName' ($pageCount pág.) • SHA-256: ${originalSha256.take(16)}...",
                    actorName = adminUser.fullName,
                    actorEmail = adminUser.email,
                    timestamp = now - 2000L
                ),
                AuditEventEntity(
                    documentId = docId,
                    eventType = AuditEventType.SIGNATARIOS_CONFIGURADOS,
                    title = "${signers.size} Signatário(s) Qualificado(s)",
                    description = signers.joinToString("; ") {
                        "${it.fullName} (${it.classification.label} • ${it.signatureType.shortName})"
                    },
                    actorName = adminUser.fullName,
                    actorEmail = adminUser.email,
                    timestamp = now - 1000L
                ),
                AuditEventEntity(
                    documentId = docId,
                    eventType = AuditEventType.CAMPOS_POSICIONADOS,
                    title = "${fields.size} Campo(s) Posicionado(s) no Documento",
                    description = "Coordenadas relativas salvas nas páginas ${fields.map { it.pageIndex + 1 }.distinct().sorted().joinToString(", ")}.",
                    actorName = adminUser.fullName,
                    actorEmail = adminUser.email,
                    timestamp = now - 500L
                ),
                AuditEventEntity(
                    documentId = docId,
                    eventType = AuditEventType.DOCUMENTO_ENVIADO,
                    title = "Solicitações de Assinatura Disparadas",
                    description = "Protocolo ${idempotencyKey.take(12)} • Convites preparados para WhatsApp, SMS e E-mail.",
                    actorName = adminUser.fullName,
                    actorEmail = adminUser.email,
                    timestamp = now
                )
            )
            dao.insertAuditEvents(initialEvents)

            Result.success(docId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun recordDocumentViewedBySigner(
        documentId: Long,
        signerUser: UserEntity
    ) = withContext(Dispatchers.IO) {
        val details = dao.getDocumentDetails(documentId) ?: return@withContext
        val alreadyLoggedView = details.auditEvents.any {
            it.eventType == AuditEventType.DOCUMENTO_VISUALIZADO &&
                it.actorEmail.equals(signerUser.email, ignoreCase = true)
        }
        if (!alreadyLoggedView) {
            val ip = PdfEngine.getLocalIpAddress()
            dao.insertAuditEvent(
                AuditEventEntity(
                    documentId = documentId,
                    eventType = AuditEventType.DOCUMENTO_VISUALIZADO,
                    title = "Documento Aberto para Conferência",
                    description = "Visualização das páginas e campos pelo signatário • IP: $ip",
                    actorName = signerUser.fullName,
                    actorEmail = signerUser.email
                )
            )
        }
    }

    override suspend fun recordInviteShared(
        documentId: Long,
        channel: String,
        actor: UserEntity
    ): Unit = withContext(Dispatchers.IO) {
        dao.insertAuditEvent(
            AuditEventEntity(
                documentId = documentId,
                eventType = AuditEventType.LINK_COMPARTILHADO,
                title = "Convite Compartilhado via $channel",
                description = "Link de assinatura enviado com chave criptográfica pelo canal $channel.",
                actorName = actor.fullName,
                actorEmail = actor.email
            )
        )
        Unit
    }

    override suspend fun signDocument(
        context: Context,
        documentId: Long,
        signerUser: UserEntity,
        signatureBitmap: Bitmap?,
        verificationToken: String?,
        certificateInfo: String?
    ): Result<DocumentFullDetails> = withContext(Dispatchers.IO) {
        try {
            val details = dao.getDocumentDetails(documentId)
                ?: return@withContext Result.failure(IllegalArgumentException("Documento não encontrado."))

            val signerRecord = details.signers.find { it.signerUserId == signerUser.id }
                ?: return@withContext Result.failure(
                    IllegalAccessException("Você não está autorizado como signatário deste documento.")
                )

            if (signerRecord.status == SignerStatus.ASSINADO) {
                return@withContext Result.failure(
                    IllegalStateException("Você já realizou a assinatura deste documento.")
                )
            }

            val now = System.currentTimeMillis()
            val savedSignaturePath = if (signatureBitmap != null) {
                PdfEngine.saveSignatureBitmap(context, signatureBitmap)
            } else {
                null
            }

            val ipAddress = PdfEngine.getLocalIpAddress()
            val rawEvidence = "${details.document.id}|${details.document.originalSha256}|${signerUser.email}|$now|${signerRecord.signatureType.name}|$ipAddress"
            val sigHash = PdfEngine.calculateStringSha256(rawEvidence)

            val updatedSigner = signerRecord.copy(
                status = SignerStatus.ASSINADO,
                signedAt = now,
                signatureImagePath = savedSignaturePath,
                verificationToken = verificationToken?.takeIf { it.isNotBlank() },
                certificateInfo = certificateInfo?.takeIf { it.isNotBlank() },
                signatureHash = sigHash,
                ipAddress = ipAddress
            )
            dao.updateDocumentSigner(updatedSigner)

            val updatedFieldsForSigner = details.fields
                .filter { it.signerUserId == signerUser.id }
                .map { field ->
                    val summary = when (field.fieldType) {
                        br.com.fiap.letssign.domain.model.FieldType.CPF -> signerUser.legalDocument.ifBlank { "CPF: ${signerRecord.signerPhone}" }
                        br.com.fiap.letssign.domain.model.FieldType.DATA -> "Data: ${PdfEngine.formatTimestamp(now)}"
                        br.com.fiap.letssign.domain.model.FieldType.ASSINATURA,
                        br.com.fiap.letssign.domain.model.FieldType.RUBRICA -> sigHash
                    }
                    field.copy(
                        isSigned = true,
                        signedAt = now,
                        signatureImagePath = if (field.fieldType == br.com.fiap.letssign.domain.model.FieldType.ASSINATURA || field.fieldType == br.com.fiap.letssign.domain.model.FieldType.RUBRICA) savedSignaturePath else null,
                        stampSummary = summary
                    )
                }
            dao.updateSignatureFields(updatedFieldsForSigner)

            val signatureAuditEvent = AuditEventEntity(
                documentId = documentId,
                eventType = AuditEventType.ASSINATURA_REALIZADA,
                title = "Assinatura Efetuada (${signerRecord.classification.label})",
                description = "Modalidade: ${signerRecord.signatureType.fullName} • IP: $ipAddress • Hash: ${sigHash.take(16)}...",
                actorName = signerUser.fullName,
                actorEmail = signerUser.email,
                timestamp = now
            )
            dao.insertAuditEvent(signatureAuditEvent)

            val allUpdatedSigners = details.signers.map {
                if (it.id == updatedSigner.id) updatedSigner else it
            }
            val allUpdatedFields = details.fields.map { f ->
                updatedFieldsForSigner.find { it.id == f.id } ?: f
            }

            val allMandatoryCompleted = allUpdatedSigners
                .filter { it.isMandatory }
                .all { it.status == SignerStatus.ASSINADO }

            val newDocStatus = if (allMandatoryCompleted) {
                DocumentStatus.CONCLUIDO
            } else {
                DocumentStatus.PARCIALMENTE_ASSINADO
            }

            val allEvents = details.auditEvents + listOfNotNull(
                signatureAuditEvent,
                if (allMandatoryCompleted) {
                    AuditEventEntity(
                        documentId = documentId,
                        eventType = AuditEventType.DOCUMENTO_CONCLUIDO,
                        title = "Documento 100% Concluído e Selado",
                        description = "Todos os signatários finalizaram suas assinaturas. Manifesto e rodapés AssinaJÁ anexados.",
                        actorName = "Motor de Autenticação AssinaJÁ",
                        actorEmail = "sistema@assinaja.com.br",
                        timestamp = now + 200L
                    )
                } else null
            )

            if (allMandatoryCompleted) {
                dao.insertAuditEvent(allEvents.last())
            }

            // Gera o PDF atualizado com a estampa oficial AssinaJÁ no rodapé e selos de assinatura
            val (newPdfPath, newPdfSha256) = PdfEngine.generateUpdatedSignedPdf(
                context = context,
                document = details.document,
                allSigners = allUpdatedSigners,
                allFields = allUpdatedFields,
                auditEvents = allEvents,
                includeManifestPage = allMandatoryCompleted
            )

            val updatedDoc = details.document.copy(
                currentPdfPath = newPdfPath,
                currentSha256 = newPdfSha256,
                status = newDocStatus,
                completedAt = if (allMandatoryCompleted) now + 200L else null
            )
            dao.updateDocument(updatedDoc)

            val finalDetails = dao.getDocumentDetails(documentId)!!
            Result.success(finalDetails)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun rejectDocument(
        documentId: Long,
        signerUser: UserEntity,
        reason: String
    ): Result<DocumentFullDetails> = withContext(Dispatchers.IO) {
        try {
            val details = dao.getDocumentDetails(documentId)
                ?: return@withContext Result.failure(IllegalArgumentException("Documento não encontrado."))

            val signerRecord = details.signers.find { it.signerUserId == signerUser.id }
                ?: return@withContext Result.failure(
                    IllegalAccessException("Você não está autorizado como signatário deste documento.")
                )

            val now = System.currentTimeMillis()
            val ip = PdfEngine.getLocalIpAddress()
            val cleanReason = reason.trim().ifBlank { "Signatário optou por não assinar o documento." }

            val updatedSigner = signerRecord.copy(
                status = SignerStatus.REJEITADO,
                signedAt = now,
                ipAddress = ip
            )
            dao.updateDocumentSigner(updatedSigner)

            val rejectEvent = AuditEventEntity(
                documentId = documentId,
                eventType = AuditEventType.DOCUMENTO_REJEITADO,
                title = "Documento Rejeitado pelo Signatário",
                description = "Motivo: \"$cleanReason\" • IP: $ip",
                actorName = signerUser.fullName,
                actorEmail = signerUser.email,
                timestamp = now
            )
            dao.insertAuditEvent(rejectEvent)

            val updatedDoc = details.document.copy(
                status = DocumentStatus.REJEITADO
            )
            dao.updateDocument(updatedDoc)

            val finalDetails = dao.getDocumentDetails(documentId)!!
            Result.success(finalDetails)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
