package br.com.fiap.letssign.presentation

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.fiap.letssign.data.local.AuditEventEntity
import br.com.fiap.letssign.data.local.DocumentFullDetails
import br.com.fiap.letssign.data.local.UserEntity
import br.com.fiap.letssign.data.repository.LetsSignRepository
import br.com.fiap.letssign.domain.model.DraftSignatureField
import br.com.fiap.letssign.domain.model.DraftSignerSelection
import br.com.fiap.letssign.domain.model.SignatureType
import br.com.fiap.letssign.domain.model.SignerClassification
import br.com.fiap.letssign.util.PdfEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class DocumentWizardUiState(
    val currentStep: Int = 1, // 1: Upload PDF, 2: Signatários, 3: Posicionamento no PDF, 4: Revisão e Disparo
    val title: String = "",
    val description: String = "",
    val fileName: String = "",
    val internalPdfPath: String = "",
    val pageCount: Int = 0,
    val sha256Hash: String = "",
    val fileSizeBytes: Long = 0L,
    val pdfValidationError: String? = null,
    val isProcessingPdf: Boolean = false,
    val selectedSigners: List<DraftSignerSelection> = emptyList(),
    val activeSignerIdForPlacement: Long? = null,
    val currentPageIndex: Int = 0,
    val signatureFields: List<DraftSignatureField> = emptyList(),
    val invitationMessage: String = "Prezado(a), encaminhamos este documento para conferência e assinatura eletrônica/digital na plataforma AssinaJÁ.",
    val version: Int = 1,
    val parentDocumentId: Long? = null,
    val dispatchIdempotencyKey: String = "AJ-${UUID.randomUUID().toString().uppercase().take(10)}",
    val isDispatching: Boolean = false,
    val dispatchError: String? = null,
    val simulateFailureOnNextAttempt: Boolean = false
)

class LetsSignViewModel(
    private val repository: LetsSignRepository
) : ViewModel() {

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _uiNotification = MutableStateFlow<String?>(null)
    val uiNotification: StateFlow<String?> = _uiNotification.asStateFlow()

    private val _wizardState = MutableStateFlow(DocumentWizardUiState())
    val wizardState: StateFlow<DocumentWizardUiState> = _wizardState.asStateFlow()

    val allSigners: StateFlow<List<UserEntity>> = repository.observeSigners()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUsers: StateFlow<List<UserEntity>> = repository.observeAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminDocuments: StateFlow<List<DocumentFullDetails>> = repository.observeAllDocumentsForAdmin()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAuditEvents: StateFlow<List<AuditEventEntity>> = repository.observeAllAuditEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val signerDocuments: StateFlow<List<DocumentFullDetails>> = _currentUser
        .flatMapLatest { user ->
            if (user != null) {
                repository.observeDocumentsForSigner(user.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.ensureDefaultAdminExists()
        }
    }

    fun clearNotification() {
        _uiNotification.value = null
    }

    fun clearAuthError() {
        _authError.value = null
    }

    fun login(email: String, password: String, onSuccess: (UserEntity) -> Unit) {
        viewModelScope.launch {
            _authError.value = null
            val result = repository.login(email, password)
            result.fold(
                onSuccess = { user ->
                    _currentUser.value = user
                    onSuccess(user)
                },
                onFailure = { err ->
                    _authError.value = err.message ?: "Falha na autenticação."
                }
            )
        }
    }

    fun logout() {
        _currentUser.value = null
        _authError.value = null
    }

    // ================== GESTÃO DE SIGNATÁRIOS (ADMIN) ==================
    fun saveSigner(
        editingId: Long?,
        fullName: String,
        email: String,
        phone: String,
        password: String,
        legalDocument: String,
        onSuccess: (UserEntity) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.saveSigner(
                editingId = editingId,
                fullName = fullName,
                email = email,
                phone = phone,
                password = password,
                legalDocument = legalDocument
            )
            result.fold(
                onSuccess = { saved ->
                    _uiNotification.value = if (editingId == null) {
                        "Signatário '${saved.fullName}' cadastrado com sucesso!"
                    } else {
                        "Dados de '${saved.fullName}' atualizados com sucesso!"
                    }
                    _wizardState.update { state ->
                        val updatedSigners = state.selectedSigners.map { s ->
                            if (s.userId == saved.id) {
                                s.copy(
                                    fullName = saved.fullName,
                                    email = saved.email,
                                    phone = saved.phone
                                )
                            } else s
                        }
                        val updatedFields = state.signatureFields.map { f ->
                            if (f.signerUserId == saved.id) {
                                f.copy(signerName = saved.fullName)
                            } else f
                        }
                        state.copy(
                            selectedSigners = updatedSigners,
                            signatureFields = updatedFields
                        )
                    }
                    onSuccess(saved)
                },
                onFailure = { err ->
                    onError(err.message ?: "Erro ao salvar signatário.")
                }
            )
        }
    }

    fun deleteSigner(
        userId: Long,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.deleteSigner(userId)
            result.fold(
                onSuccess = {
                    _uiNotification.value = "Signatário removido com sucesso!"
                    _wizardState.update { state ->
                        state.copy(
                            selectedSigners = state.selectedSigners.filterNot { it.userId == userId },
                            signatureFields = state.signatureFields.filterNot { it.signerUserId == userId }
                        )
                    }
                    onSuccess()
                },
                onFailure = { err ->
                    onError(err.message ?: "Erro ao remover signatário.")
                }
            )
        }
    }

    // ================== FLUXO WIZARD DE ENVIO DE DOCUMENTO ==================
    fun startNewDocumentWizard() {
        _wizardState.value = DocumentWizardUiState(
            dispatchIdempotencyKey = "AJ-${UUID.randomUUID().toString().uppercase().take(10)}"
        )
    }

    fun startNewDocumentWizardWithPdf(context: Context, uri: Uri) {
        _wizardState.value = DocumentWizardUiState(
            dispatchIdempotencyKey = "AJ-${UUID.randomUUID().toString().uppercase().take(10)}"
        )
        importPdfFromUri(context, uri)
    }

    fun startNewVersionFromDocument(existing: DocumentFullDetails, onReady: () -> Unit) {
        val doc = existing.document
        val draftSigners = existing.signers.map { s ->
            DraftSignerSelection(
                userId = s.signerUserId,
                fullName = s.signerName,
                email = s.signerEmail,
                phone = s.signerPhone,
                classification = s.classification,
                signatureType = s.signatureType,
                isMandatory = s.isMandatory
            )
        }
        val draftFields = existing.fields.map { f ->
            DraftSignatureField(
                id = UUID.randomUUID().toString(),
                signerUserId = f.signerUserId,
                signerName = f.signerName,
                classification = f.classification,
                signatureType = f.signatureType,
                pageIndex = f.pageIndex,
                xRatio = f.xRatio,
                yRatio = f.yRatio,
                widthRatio = f.widthRatio,
                heightRatio = f.heightRatio,
                fieldType = f.fieldType
            )
        }

        _wizardState.value = DocumentWizardUiState(
            currentStep = 1,
            title = doc.title,
            description = doc.description,
            fileName = doc.originalFileName,
            internalPdfPath = doc.originalPdfPath,
            pageCount = doc.pageCount,
            sha256Hash = doc.originalSha256,
            selectedSigners = draftSigners,
            activeSignerIdForPlacement = draftSigners.firstOrNull()?.userId,
            currentPageIndex = 0,
            signatureFields = draftFields,
            invitationMessage = doc.invitationMessage,
            version = doc.version + 1,
            parentDocumentId = doc.id,
            dispatchIdempotencyKey = "AJ-${UUID.randomUUID().toString().uppercase().take(10)}"
        )
        _uiNotification.value = "Criando Nova Versão (v${doc.version + 1}). O documento original v${doc.version} permanece preservado."
        onReady()
    }

    fun updateWizardTitle(title: String) {
        _wizardState.update { it.copy(title = title, dispatchError = null) }
    }

    fun updateWizardDescription(description: String) {
        _wizardState.update { it.copy(description = description) }
    }

    fun updateWizardInvitationMessage(message: String) {
        _wizardState.update { it.copy(invitationMessage = message, dispatchError = null) }
    }

    fun setWizardStep(step: Int) {
        _wizardState.update { it.copy(currentStep = step.coerceIn(1, 4), dispatchError = null) }
    }

    fun importPdfFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _wizardState.update {
                it.copy(isProcessingPdf = true, pdfValidationError = null, dispatchError = null)
            }
            val validation = PdfEngine.validateAndCopyPdfUri(context, uri)
            if (validation.isValid) {
                val suggestedTitle = _wizardState.value.title.ifBlank {
                    validation.fileName.removeSuffix(".pdf").replace("_", " ")
                }
                _wizardState.update { state ->
                    val validFields = state.signatureFields.filter { it.pageIndex < validation.pageCount }
                    state.copy(
                        isProcessingPdf = false,
                        title = suggestedTitle,
                        fileName = validation.fileName,
                        internalPdfPath = validation.internalFilePath,
                        pageCount = validation.pageCount,
                        sha256Hash = validation.sha256Hash,
                        fileSizeBytes = validation.fileSizeBytes,
                        pdfValidationError = null,
                        currentPageIndex = 0,
                        signatureFields = validFields
                    )
                }
            } else {
                _wizardState.update {
                    it.copy(
                        isProcessingPdf = false,
                        pdfValidationError = validation.errorMessage ?: "Arquivo PDF inválido ou corrompido."
                    )
                }
            }
        }
    }

    fun generateSampleLegalContractPdf(context: Context) {
        viewModelScope.launch {
            _wizardState.update {
                it.copy(isProcessingPdf = true, pdfValidationError = null, dispatchError = null)
            }
            val validation = PdfEngine.createSampleLegalContractPdf(context)
            if (validation.isValid) {
                _wizardState.update { state ->
                    state.copy(
                        isProcessingPdf = false,
                        title = state.title.ifBlank { "Contrato de Prestação de Serviços" },
                        description = state.description.ifBlank { "Instrumento Comercial e Operacional Corporativo" },
                        fileName = validation.fileName,
                        internalPdfPath = validation.internalFilePath,
                        pageCount = validation.pageCount,
                        sha256Hash = validation.sha256Hash,
                        fileSizeBytes = validation.fileSizeBytes,
                        pdfValidationError = null,
                        currentPageIndex = 0
                    )
                }
            } else {
                _wizardState.update {
                    it.copy(
                        isProcessingPdf = false,
                        pdfValidationError = validation.errorMessage
                    )
                }
            }
        }
    }

    fun simulateCorruptedPdfSelection() {
        _wizardState.update {
            it.copy(
                pdfValidationError = "Arquivo inválido ou corrompido: O cabeçalho %PDF não pôde ser processado pelo leitor. Por favor, selecione um arquivo íntegro."
            )
        }
    }

    fun addSignerToDraft(
        user: UserEntity,
        classification: SignerClassification = SignerClassification.CONTRATANTE,
        signatureType: SignatureType = SignatureType.AVANCADA
    ) {
        _wizardState.update { state ->
            if (state.selectedSigners.any { it.userId == user.id }) {
                return@update state
            }
            val newSelection = DraftSignerSelection(
                userId = user.id,
                fullName = user.fullName,
                email = user.email,
                phone = user.phone,
                classification = classification,
                signatureType = signatureType,
                isMandatory = true
            )
            val updatedList = state.selectedSigners + newSelection
            state.copy(
                selectedSigners = updatedList,
                activeSignerIdForPlacement = state.activeSignerIdForPlacement ?: user.id,
                dispatchError = null
            )
        }
    }

    fun removeSignerFromDraft(userId: Long) {
        _wizardState.update { state ->
            val updatedSigners = state.selectedSigners.filterNot { it.userId == userId }
            val updatedFields = state.signatureFields.filterNot { it.signerUserId == userId }
            val nextActive = if (state.activeSignerIdForPlacement == userId) {
                updatedSigners.firstOrNull()?.userId
            } else {
                state.activeSignerIdForPlacement
            }
            state.copy(
                selectedSigners = updatedSigners,
                signatureFields = updatedFields,
                activeSignerIdForPlacement = nextActive
            )
        }
    }

    fun replaceSignerInDraft(oldUserId: Long, newUser: UserEntity) {
        _wizardState.update { state ->
            if (state.selectedSigners.any { it.userId == newUser.id }) {
                return@update state
            }
            val updatedSigners = state.selectedSigners.map { s ->
                if (s.userId == oldUserId) {
                    s.copy(
                        userId = newUser.id,
                        fullName = newUser.fullName,
                        email = newUser.email,
                        phone = newUser.phone
                    )
                } else s
            }
            val updatedFields = state.signatureFields.map { f ->
                if (f.signerUserId == oldUserId) {
                    f.copy(
                        signerUserId = newUser.id,
                        signerName = newUser.fullName
                    )
                } else f
            }
            val nextActive = if (state.activeSignerIdForPlacement == oldUserId) {
                newUser.id
            } else {
                state.activeSignerIdForPlacement
            }
            state.copy(
                selectedSigners = updatedSigners,
                signatureFields = updatedFields,
                activeSignerIdForPlacement = nextActive
            )
        }
    }

    fun updateSignerClassification(userId: Long, classification: SignerClassification) {
        _wizardState.update { state ->
            val updatedSigners = state.selectedSigners.map { s ->
                if (s.userId == userId) s.copy(classification = classification) else s
            }
            val updatedFields = state.signatureFields.map { f ->
                if (f.signerUserId == userId) f.copy(classification = classification) else f
            }
            state.copy(selectedSigners = updatedSigners, signatureFields = updatedFields)
        }
    }

    fun updateSignerSignatureType(userId: Long, signatureType: SignatureType) {
        _wizardState.update { state ->
            val updatedSigners = state.selectedSigners.map { s ->
                if (s.userId == userId) s.copy(signatureType = signatureType) else s
            }
            val updatedFields = state.signatureFields.map { f ->
                if (f.signerUserId == userId) f.copy(signatureType = signatureType) else f
            }
            state.copy(selectedSigners = updatedSigners, signatureFields = updatedFields)
        }
    }

    fun setActiveSignerForPlacement(userId: Long) {
        _wizardState.update { it.copy(activeSignerIdForPlacement = userId) }
    }

    fun setWizardPageIndex(pageIndex: Int) {
        _wizardState.update { state ->
            val maxPage = (state.pageCount - 1).coerceAtLeast(0)
            state.copy(currentPageIndex = pageIndex.coerceIn(0, maxPage))
        }
    }

    fun addSignatureFieldToCurrentPage(fieldType: br.com.fiap.letssign.domain.model.FieldType = br.com.fiap.letssign.domain.model.FieldType.ASSINATURA) {
        _wizardState.update { state ->
            val activeSigner = state.selectedSigners.find { it.userId == state.activeSignerIdForPlacement }
                ?: state.selectedSigners.firstOrNull()
                ?: return@update state

            val countOnPage = state.signatureFields.count { it.pageIndex == state.currentPageIndex }
            val (defaultW, defaultH) = when (fieldType) {
                br.com.fiap.letssign.domain.model.FieldType.ASSINATURA -> 0.44f to 0.12f
                br.com.fiap.letssign.domain.model.FieldType.RUBRICA -> 0.38f to 0.10f
                br.com.fiap.letssign.domain.model.FieldType.CPF -> 0.32f to 0.08f
                br.com.fiap.letssign.domain.model.FieldType.DATA -> 0.28f to 0.07f
            }
            val offsetX = (0.08f + (countOnPage % 2) * 0.44f).coerceAtMost(1f - defaultW)
            val offsetY = (0.62f + (countOnPage / 2) * 0.10f).coerceAtMost(1f - defaultH)

            val newField = DraftSignatureField(
                id = UUID.randomUUID().toString(),
                signerUserId = activeSigner.userId,
                signerName = activeSigner.fullName,
                classification = activeSigner.classification,
                signatureType = activeSigner.signatureType,
                pageIndex = state.currentPageIndex,
                xRatio = offsetX,
                yRatio = offsetY,
                widthRatio = defaultW,
                heightRatio = defaultH,
                fieldType = fieldType
            ).clamped()

            state.copy(
                signatureFields = state.signatureFields + newField,
                dispatchError = null
            )
        }
    }

    fun updateSignatureFieldPosition(fieldId: String, newXRatio: Float, newYRatio: Float) {
        _wizardState.update { state ->
            val updated = state.signatureFields.map { field ->
                if (field.id == fieldId) {
                    field.copy(xRatio = newXRatio, yRatio = newYRatio).clamped()
                } else field
            }
            state.copy(signatureFields = updated)
        }
    }

    fun updateSignatureFieldDimensions(fieldId: String, newWidthRatio: Float, newHeightRatio: Float) {
        _wizardState.update { state ->
            val updated = state.signatureFields.map { field ->
                if (field.id == fieldId) {
                    field.copy(widthRatio = newWidthRatio, heightRatio = newHeightRatio).clamped()
                } else field
            }
            state.copy(signatureFields = updated)
        }
    }

    fun removeSignatureField(fieldId: String) {
        _wizardState.update { state ->
            state.copy(signatureFields = state.signatureFields.filterNot { it.id == fieldId })
        }
    }

    fun toggleSimulateFailureOnNextAttempt(enabled: Boolean) {
        _wizardState.update { it.copy(simulateFailureOnNextAttempt = enabled, dispatchError = null) }
    }

    fun dispatchDocument(onSuccess: (Long) -> Unit) {
        val admin = _currentUser.value ?: return
        val state = _wizardState.value
        viewModelScope.launch {
            _wizardState.update { it.copy(isDispatching = true, dispatchError = null) }
            val result = repository.dispatchDocumentForSignature(
                idempotencyKey = state.dispatchIdempotencyKey,
                title = state.title,
                description = state.description,
                originalFileName = state.fileName,
                originalPdfPath = state.internalPdfPath,
                pageCount = state.pageCount,
                originalSha256 = state.sha256Hash,
                invitationMessage = state.invitationMessage,
                adminUser = admin,
                signers = state.selectedSigners,
                fields = state.signatureFields,
                version = state.version,
                parentDocumentId = state.parentDocumentId,
                simulateTransientFailure = state.simulateFailureOnNextAttempt
            )
            result.fold(
                onSuccess = { docId ->
                    _wizardState.update { it.copy(isDispatching = false, dispatchError = null) }
                    _uiNotification.value = "Documento enviado com sucesso! Convites preparados no AssinaJÁ."
                    onSuccess(docId)
                },
                onFailure = { err ->
                    _wizardState.update {
                        it.copy(
                            isDispatching = false,
                            simulateFailureOnNextAttempt = false,
                            dispatchError = err.message ?: "Falha ao enviar o documento."
                        )
                    }
                }
            )
        }
    }

    // ================== ACOMPANHAMENTO, COMPARTILHAMENTO & ASSINATURA ==================
    fun observeDocumentDetails(documentId: Long) = repository.observeDocumentDetails(documentId)

    fun recordDocumentViewed(documentId: Long) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.recordDocumentViewedBySigner(documentId, user)
        }
    }

    fun recordInviteShared(documentId: Long, channel: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.recordInviteShared(documentId, channel, user)
            _uiNotification.value = "Link copiado e registrado para envio via $channel!"
        }
    }

    fun signDocumentAsCurrentSigner(
        context: Context,
        documentId: Long,
        signatureBitmap: Bitmap?,
        verificationToken: String?,
        certificateInfo: String?,
        onSuccess: (DocumentFullDetails) -> Unit,
        onError: (String) -> Unit
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val result = repository.signDocument(
                context = context,
                documentId = documentId,
                signerUser = user,
                signatureBitmap = signatureBitmap,
                verificationToken = verificationToken,
                certificateInfo = certificateInfo
            )
            result.fold(
                onSuccess = { updated ->
                    _uiNotification.value = "Assinatura registrada e carimbada no PDF oficial AssinaJÁ!"
                    onSuccess(updated)
                },
                onFailure = { err ->
                    onError(err.message ?: "Erro ao processar assinatura.")
                }
            )
        }
    }

    fun rejectDocumentAsCurrentSigner(
        documentId: Long,
        reason: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val result = repository.rejectDocument(documentId, user, reason)
            result.fold(
                onSuccess = {
                    _uiNotification.value = "Documento formalmente rejeitado."
                    onSuccess()
                },
                onFailure = { err ->
                    onError(err.message ?: "Erro ao registrar recusa.")
                }
            )
        }
    }

    fun exportDocumentToDownloads(
        context: Context,
        documentId: Long,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val details = repository.getDocumentDetails(documentId)
            if (details == null) {
                onError("Documento não encontrado para exportação.")
                return@launch
            }
            val doc = details.document
            val safeFileName = "${doc.title}_v${doc.version}_Assinado.pdf"
            val result = PdfEngine.exportPdfToPublicDownloads(context, doc.currentPdfPath, safeFileName)
            result.fold(
                onSuccess = { msg ->
                    _uiNotification.value = msg
                    onSuccess(msg)
                },
                onFailure = { err ->
                    val errorMsg = err.message ?: "Erro ao salvar PDF no dispositivo."
                    onError(errorMsg)
                }
            )
        }
    }

    class Factory(private val repository: LetsSignRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LetsSignViewModel(repository) as T
        }
    }
}
