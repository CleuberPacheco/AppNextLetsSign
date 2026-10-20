package br.com.fiap.letssign.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.fiap.letssign.data.local.UserEntity
import br.com.fiap.letssign.domain.model.SignatureType
import br.com.fiap.letssign.domain.model.SignerClassification
import br.com.fiap.letssign.presentation.DocumentWizardUiState
import br.com.fiap.letssign.ui.components.PdfPageInteractiveCanvas
import br.com.fiap.letssign.ui.components.SignatureTypeBadge
import br.com.fiap.letssign.ui.components.SignerClassificationBadge
import br.com.fiap.letssign.ui.components.SignerFormDialog
import br.com.fiap.letssign.ui.theme.ErrorRed
import br.com.fiap.letssign.ui.theme.IcpBlue
import br.com.fiap.letssign.ui.theme.NavyPrimary
import br.com.fiap.letssign.ui.theme.SuccessGreen
import br.com.fiap.letssign.ui.theme.TealAccent
import br.com.fiap.letssign.ui.theme.WarningAmber
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateDocumentWizardScreen(
    wizardState: DocumentWizardUiState,
    availableSigners: List<UserEntity>,
    onUpdateTitle: (String) -> Unit,
    onUpdateDescription: (String) -> Unit,
    onUpdateInvitationMessage: (String) -> Unit,
    onImportPdfUri: (android.net.Uri) -> Unit,
    onGenerateSamplePdf: () -> Unit,
    onSimulateCorruptedPdf: () -> Unit,
    onAddSignerToDraft: (UserEntity, SignerClassification, SignatureType) -> Unit,
    onRemoveSignerFromDraft: (Long) -> Unit,
    onReplaceSignerInDraft: (oldUserId: Long, newUser: UserEntity) -> Unit,
    onUpdateSignerClassification: (Long, SignerClassification) -> Unit,
    onUpdateSignerSignatureType: (Long, SignatureType) -> Unit,
    onSaveNewSigner: (
        editingId: Long?,
        fullName: String,
        email: String,
        phone: String,
        password: String,
        legalDoc: String,
        onSuccess: (UserEntity) -> Unit,
        onError: (String) -> Unit
    ) -> Unit,
    onSetActiveSignerForPlacement: (Long) -> Unit,
    onSetPageIndex: (Int) -> Unit,
    onAddFieldToPage: () -> Unit,
    onMoveField: (String, Float, Float) -> Unit,
    onResizeField: (String, Float, Float) -> Unit,
    onRemoveField: (String) -> Unit,
    onSetStep: (Int) -> Unit,
    onToggleSimulateFailure: (Boolean) -> Unit,
    onDispatchDocument: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onImportPdfUri(uri)
        }
    }

    var showCreateSignerDialog by remember { mutableStateOf(false) }
    var signerIdToReplace by remember { mutableStateOf<Long?>(null) }

    if (showCreateSignerDialog) {
        SignerFormDialog(
            initialSigner = null,
            onDismiss = { showCreateSignerDialog = false },
            onConfirm = { editingId, fullName, email, phone, password, legalDoc, setError ->
                onSaveNewSigner(
                    editingId,
                    fullName,
                    email,
                    phone,
                    password,
                    legalDoc,
                    { createdUser ->
                        showCreateSignerDialog = false
                        onAddSignerToDraft(
                            createdUser,
                            SignerClassification.CONTRATANTE,
                            SignatureType.AVANCADA
                        )
                    },
                    { err -> setError(err) }
                )
            }
        )
    }

    // Modal para substituir um signatário já vinculado ao documento por outro cadastrado
    if (signerIdToReplace != null) {
        val candidates = availableSigners.filter { candidate ->
            wizardState.selectedSigners.none { it.userId == candidate.id }
        }
        AlertDialog(
            onDismissRequest = { signerIdToReplace = null },
            title = { Text("Substituir Signatário", fontWeight = FontWeight.Bold) },
            text = {
                if (candidates.isEmpty()) {
                    Text(
                        "Todos os signatários cadastrados já estão selecionados neste documento. Cadastre um novo signatário primeiro para realizar a substituição.",
                        fontSize = 13.sp
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Selecione o novo signatário que assumirá o papel e os campos de assinatura deste participante:",
                            fontSize = 12.5.sp
                        )
                        candidates.forEach { candidate ->
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F6FC)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onReplaceSignerInDraft(signerIdToReplace!!, candidate)
                                        signerIdToReplace = null
                                    }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(candidate.fullName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(candidate.email, fontSize = 12.sp, color = Color(0xFF475569))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { signerIdToReplace = null }) {
                    Text("Fechar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (wizardState.version > 1) {
                                "Nova Versão (v${wizardState.version}) • Fluxo de Assinatura"
                            } else {
                                "Novo Fluxo de Documento p/ Assinatura"
                            },
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Etapa ${wizardState.currentStep} de 4",
                            fontSize = 11.5.sp,
                            color = Color(0xFFB8C9E8)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (wizardState.currentStep > 1) {
                                onSetStep(wizardState.currentStep - 1)
                            } else {
                                onBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyPrimary)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            WizardStepIndicator(
                currentStep = wizardState.currentStep,
                onSelectStep = { step ->
                    // Só permite pular para etapas cujos pré-requisitos já foram cumpridos
                    if (step < wizardState.currentStep) {
                        onSetStep(step)
                    }
                }
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (wizardState.currentStep) {
                    1 -> Step1UploadPdfContent(
                        wizardState = wizardState,
                        onUpdateTitle = onUpdateTitle,
                        onUpdateDescription = onUpdateDescription,
                        onPickPdfFromDevice = {
                            pdfPickerLauncher.launch(arrayOf("application/pdf"))
                        },
                        onGenerateSamplePdf = onGenerateSamplePdf,
                        onSimulateCorruptedPdf = onSimulateCorruptedPdf,
                        onNextStep = { onSetStep(2) }
                    )

                    2 -> Step2SelectSignersContent(
                        wizardState = wizardState,
                        availableSigners = availableSigners,
                        onOpenCreateSignerModal = { showCreateSignerDialog = true },
                        onAddSigner = onAddSignerToDraft,
                        onRemoveSigner = onRemoveSignerFromDraft,
                        onRequestReplaceSigner = { oldId -> signerIdToReplace = oldId },
                        onUpdateClassification = onUpdateSignerClassification,
                        onUpdateSignatureType = onUpdateSignerSignatureType,
                        onPreviousStep = { onSetStep(1) },
                        onNextStep = { onSetStep(3) }
                    )

                    3 -> Step3PositionSignatureFieldsContent(
                        wizardState = wizardState,
                        onSetActiveSigner = onSetActiveSignerForPlacement,
                        onSetPageIndex = onSetPageIndex,
                        onAddField = onAddFieldToPage,
                        onMoveField = onMoveField,
                        onResizeField = onResizeField,
                        onRemoveField = onRemoveField,
                        onPreviousStep = { onSetStep(2) },
                        onNextStep = { onSetStep(4) }
                    )

                    4 -> Step4ReviewAndDispatchContent(
                        wizardState = wizardState,
                        onUpdateInvitationMessage = onUpdateInvitationMessage,
                        onSetPageIndex = onSetPageIndex,
                        onToggleSimulateFailure = onToggleSimulateFailure,
                        onPreviousStep = { onSetStep(3) },
                        onDispatch = onDispatchDocument
                    )
                }
            }
        }
    }
}

@Composable
private fun WizardStepIndicator(
    currentStep: Int,
    onSelectStep: (Int) -> Unit
) {
    val steps = listOf(
        1 to "1. PDF & Título",
        2 to "2. Signatários",
        3 to "3. Posição PDF",
        4 to "4. Revisão"
    )
    Surface(
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            steps.forEach { (stepNum, label) ->
                val isCurrent = currentStep == stepNum
                val isCompleted = currentStep > stepNum
                val bgColor = when {
                    isCurrent -> NavyPrimary
                    isCompleted -> SuccessGreen
                    else -> Color(0xFFE2E8F0)
                }
                val textColor = when {
                    isCurrent || isCompleted -> Color.White
                    else -> Color(0xFF64748B)
                }

                Surface(
                    color = bgColor,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .clickable(enabled = isCompleted) { onSelectStep(stepNum) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        if (isCompleted) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                        }
                        Text(
                            text = label,
                            color = textColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Step1UploadPdfContent(
    wizardState: DocumentWizardUiState,
    onUpdateTitle: (String) -> Unit,
    onUpdateDescription: (String) -> Unit,
    onPickPdfFromDevice: () -> Unit,
    onGenerateSamplePdf: () -> Unit,
    onSimulateCorruptedPdf: () -> Unit,
    onNextStep: () -> Unit
) {
    val canAdvance = wizardState.title.trim().isNotEmpty() &&
        wizardState.internalPdfPath.isNotBlank() &&
        wizardState.pageCount > 0 &&
        wizardState.pdfValidationError == null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFDCE3F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "1. Identificação e Importação do Documento PDF",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )

                OutlinedTextField(
                    value = wizardState.title,
                    onValueChange = onUpdateTitle,
                    label = { Text("Título do Documento *") },
                    placeholder = { Text("Ex: Contrato de Prestação de Serviços Advocatícios") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = wizardState.description,
                    onValueChange = onUpdateDescription,
                    label = { Text("Referência Jurídica / Processo (Opcional)") },
                    placeholder = { Text("Ex: Autos nº 1002345-88.2026.8.26.0100") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(color = Color(0xFFE2E8F0))

                Text(
                    text = "Arquivo PDF do Documento *",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onPickPdfFromDevice,
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Selecionar PDF no Celular", fontSize = 12.5.sp)
                    }
                }

                // Botões auxiliares para facilitar testes rápidos no Emulador (Gerar Minuta PDF ou Testar Arquivo Corrompido)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onGenerateSamplePdf,
                        border = BorderStroke(1.dp, TealAccent),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = TealAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Gerar Minuta PDF Jurídica (2 pág.)",
                            color = TealAccent,
                            fontSize = 11.5.sp
                        )
                    }

                    OutlinedButton(
                        onClick = onSimulateCorruptedPdf,
                        border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = null,
                            tint = ErrorRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Testar PDF Inválido",
                            color = ErrorRed,
                            fontSize = 11.sp
                        )
                    }
                }

                if (wizardState.isProcessingPdf) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Validando integridade e páginas do arquivo PDF...", fontSize = 12.5.sp)
                    }
                }

                // Alerta de PDF inválido, corrompido ou que não pôde ser processado (Requisito Obrigatório Item 3)
                if (wizardState.pdfValidationError != null) {
                    Surface(
                        color = Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = ErrorRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Não foi possível processar o arquivo PDF",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = ErrorRed
                                )
                                Text(
                                    text = wizardState.pdfValidationError,
                                    fontSize = 12.sp,
                                    color = Color(0xFF7F1D1D)
                                )
                            }
                        }
                    }
                }

                // Card de PDF validado com sucesso
                if (wizardState.internalPdfPath.isNotBlank() && wizardState.pageCount > 0) {
                    Surface(
                        color = Color(0xFFE8F8EE),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "PDF Íntegro e Validado com Sucesso",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = SuccessGreen
                                )
                            }
                            Text(
                                text = "Arquivo: ${wizardState.fileName} • ${wizardState.pageCount} página(s)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NavyPrimary
                            )
                            Text(
                                text = "SHA-256: ${wizardState.sha256Hash}",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF334155),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        Button(
            onClick = onNextStep,
            enabled = canAdvance,
            colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Avançar: Selecionar e Classificar Signatários", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Step2SelectSignersContent(
    wizardState: DocumentWizardUiState,
    availableSigners: List<UserEntity>,
    onOpenCreateSignerModal: () -> Unit,
    onAddSigner: (UserEntity, SignerClassification, SignatureType) -> Unit,
    onRemoveSigner: (Long) -> Unit,
    onRequestReplaceSigner: (Long) -> Unit,
    onUpdateClassification: (Long, SignerClassification) -> Unit,
    onUpdateSignatureType: (Long, SignatureType) -> Unit,
    onPreviousStep: () -> Unit,
    onNextStep: () -> Unit
) {
    val unselectedSigners = availableSigners.filter { candidate ->
        wizardState.selectedSigners.none { it.userId == candidate.id }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFDCE3F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "2. Seleção e Classificação dos Signatários",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                        Text(
                            text = "Mínimo de 1 signatário obrigatório. Classifique como Contratante, Contratado ou Testemunha e defina o tipo de assinatura.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(onClick = onOpenCreateSignerModal) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Novo", fontSize = 12.sp)
                    }
                }

                // Lista de signatários cadastrados disponíveis para adicionar
                if (unselectedSigners.isNotEmpty()) {
                    Text(
                        text = "Toque para adicionar um signatário cadastrado ao documento:",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NavyPrimary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        unselectedSigners.forEach { user ->
                            Surface(
                                color = Color(0xFFF0F4FA),
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, NavyPrimary.copy(alpha = 0.3f)),
                                modifier = Modifier.clickable {
                                    onAddSigner(
                                        user,
                                        SignerClassification.CONTRATANTE,
                                        SignatureType.AVANCADA
                                    )
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = NavyPrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${user.fullName} (${user.email})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = NavyPrimary
                                    )
                                }
                            }
                        }
                    }
                } else if (availableSigners.isEmpty()) {
                    Surface(
                        color = Color(0xFFFFF8E1),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Nenhum signatário cadastrado ainda.",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF92400E)
                            )
                            Button(
                                onClick = onOpenCreateSignerModal,
                                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cadastrar Signatário Agora")
                            }
                        }
                    }
                }
            }
        }

        // Signatários selecionados para o documento atual
        if (wizardState.selectedSigners.isEmpty()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Atenção: O documento deve possuir no mínimo 1 Signatário obrigatório para prosseguir.",
                    color = ErrorRed,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(14.dp)
                )
            }
        } else {
            wizardState.selectedSigners.forEachIndexed { index, signer ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFCFD8E6)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${index + 1}. ${signer.fullName}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NavyPrimary
                                )
                                Text(
                                    text = "${signer.email} • ${signer.phone}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row {
                                IconButton(
                                    onClick = { onRequestReplaceSigner(signer.userId) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SwapHoriz,
                                        contentDescription = "Substituir Signatário",
                                        tint = IcpBlue
                                    )
                                }
                                IconButton(
                                    onClick = { onRemoveSigner(signer.userId) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remover Signatário",
                                        tint = ErrorRed
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFFEEF2F6))

                        // Classificação Jurídica: Contratante, Contratado ou Testemunha
                        Text(
                            text = "Classificação no Instrumento Jurídico:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SignerClassification.entries.forEach { role ->
                                val selected = signer.classification == role
                                FilterChip(
                                    selected = selected,
                                    onClick = { onUpdateClassification(signer.userId, role) },
                                    label = { Text(role.label, fontSize = 11.5.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NavyPrimary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        // Nuances de Tipo de Assinatura Digital (Simples, Avançada, Qualificada ICP-Brasil)
                        Text(
                            text = "Modalidade de Assinatura Exigida (Lei 14.063/2020):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SignatureType.entries.forEach { sigType ->
                                val isSelected = signer.signatureType == sigType
                                Surface(
                                    color = if (isSelected) Color(0xFFEEF5FF) else Color(0xFFF8FAFC),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) IcpBlue else Color(0xFFDCE3F0)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onUpdateSignatureType(signer.userId, sigType) }
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = sigType.fullName,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) IcpBlue else NavyPrimary
                                            )
                                            Text(
                                                text = sigType.legalBasis,
                                                fontSize = 10.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                        Text(
                                            text = sigType.description,
                                            fontSize = 11.sp,
                                            color = Color(0xFF475569)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = onPreviousStep,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("Voltar")
            }
            Button(
                onClick = onNextStep,
                enabled = wizardState.selectedSigners.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                modifier = Modifier
                    .weight(1.6f)
                    .height(48.dp)
            ) {
                Text("Posicionar Assinaturas no PDF", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun Step3PositionSignatureFieldsContent(
    wizardState: DocumentWizardUiState,
    onSetActiveSigner: (Long) -> Unit,
    onSetPageIndex: (Int) -> Unit,
    onAddField: () -> Unit,
    onMoveField: (String, Float, Float) -> Unit,
    onResizeField: (String, Float, Float) -> Unit,
    onRemoveField: (String) -> Unit,
    onPreviousStep: () -> Unit,
    onNextStep: () -> Unit
) {
    val currentPageFields = wizardState.signatureFields.filter {
        it.pageIndex == wizardState.currentPageIndex
    }
    val signersWithoutFields = wizardState.selectedSigners.filter { s ->
        wizardState.signatureFields.none { it.signerUserId == s.userId }
    }
    val canAdvance = wizardState.signatureFields.isNotEmpty() && signersWithoutFields.isEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Barra de seleção de Signatário + Botão Adicionar Campo
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFDCE3F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "3. Posicionamento dos Campos de Assinatura (Drag & Drop)",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )
                Text(
                    text = "Selecione o signatário abaixo, toque em '+ Adicionar Campo' e arraste a caixa sobre a página do PDF. As coordenadas relativas (0..1) são salvas automaticamente.",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    wizardState.selectedSigners.forEach { signer ->
                        val isActive = wizardState.activeSignerIdForPlacement == signer.userId
                        val fieldsCount = wizardState.signatureFields.count { it.signerUserId == signer.userId }
                        FilterChip(
                            selected = isActive,
                            onClick = { onSetActiveSigner(signer.userId) },
                            label = {
                                Text(
                                    "${signer.fullName} • ${signer.classification.label} ($fieldsCount)",
                                    fontSize = 11.5.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NavyPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Controles de paginação do PDF
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = { onSetPageIndex(wizardState.currentPageIndex - 1) },
                            enabled = wizardState.currentPageIndex > 0,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("◀ Pág. Ant.", fontSize = 11.5.sp)
                        }
                        Text(
                            text = "Pág. ${wizardState.currentPageIndex + 1}/${wizardState.pageCount}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = NavyPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )
                        OutlinedButton(
                            onClick = { onSetPageIndex(wizardState.currentPageIndex + 1) },
                            enabled = wizardState.currentPageIndex < wizardState.pageCount - 1,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Próx. Pág. ▶", fontSize = 11.5.sp)
                        }
                    }

                    Button(
                        onClick = onAddField,
                        colors = ButtonDefaults.buttonColors(containerColor = TealAccent)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Adicionar Campo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Visualização da Página do PDF com Campos Arrastáveis (Drag & Drop)
        PdfPageInteractiveCanvas(
            pdfFilePath = wizardState.internalPdfPath,
            pageIndex = wizardState.currentPageIndex,
            fieldsOnPage = currentPageFields,
            isEditable = true,
            onFieldMoved = onMoveField,
            onFieldRemoved = onRemoveField
        )

        // Lista de ajuste fino de dimensões e posições da página atual
        if (currentPageFields.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFDCE3F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Campos na Página ${wizardState.currentPageIndex + 1} (Coordenadas Relativas Preservadas):",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )
                    currentPageFields.forEach { field ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${field.signerName} (${field.classification.label})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NavyPrimary
                                )
                                Text(
                                    text = "Posição: X=${(field.xRatio * 100).roundToInt()}%, Y=${(field.yRatio * 100).roundToInt()}% • Dimensão: L=${(field.widthRatio * 100).roundToInt()}%, A=${(field.heightRatio * 100).roundToInt()}%",
                                    fontSize = 10.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF475569)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(
                                    onClick = {
                                        val nextW = if (field.widthRatio < 0.45f) 0.48f else 0.34f
                                        val nextH = if (field.heightRatio < 0.13f) 0.14f else 0.10f
                                        onResizeField(field.id, nextW, nextH)
                                    }
                                ) {
                                    Text("Tam.", fontSize = 11.sp)
                                }
                                IconButton(
                                    onClick = { onRemoveField(field.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Excluir campo",
                                        tint = ErrorRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (signersWithoutFields.isNotEmpty()) {
            Surface(
                color = Color(0xFFFFF8E1),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Adicione pelo menos 1 campo de assinatura para: ${signersWithoutFields.joinToString(", ") { it.fullName }}.",
                    color = Color(0xFF92400E),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = onPreviousStep,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("Voltar")
            }
            Button(
                onClick = onNextStep,
                enabled = canAdvance,
                colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                modifier = Modifier
                    .weight(1.6f)
                    .height(48.dp)
            ) {
                Text("Revisar e Disparar", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun Step4ReviewAndDispatchContent(
    wizardState: DocumentWizardUiState,
    onUpdateInvitationMessage: (String) -> Unit,
    onSetPageIndex: (Int) -> Unit,
    onToggleSimulateFailure: (Boolean) -> Unit,
    onPreviousStep: () -> Unit,
    onDispatch: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Resumo do Documento Final
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFDCE3F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "5. Revisão Final antes do Disparo",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Text(
                    text = "Documento: ${wizardState.title} (Versão v${wizardState.version})",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )
                if (wizardState.description.isNotBlank()) {
                    Text(
                        text = "Referência: ${wizardState.description}",
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "Arquivo: ${wizardState.fileName} (${wizardState.pageCount} páginas)",
                    fontSize = 12.sp,
                    color = Color(0xFF475569)
                )
                Text(
                    text = "Hash SHA-256: ${wizardState.sha256Hash}",
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF334155)
                )
                Text(
                    text = "Chave de Idempotência (Anti-Duplicação): ${wizardState.dispatchIdempotencyKey}",
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TealAccent
                )
            }
        }

        // 2. Lista de Signatários e Campos mapeados por página
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFDCE3F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Signatários, Classificação e Campos de Assinatura (${wizardState.signatureFields.size} campos)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )
                wizardState.selectedSigners.forEach { signer ->
                    val signerFields = wizardState.signatureFields.filter { it.signerUserId == signer.userId }
                    val pagesSummary = signerFields
                        .groupBy { it.pageIndex + 1 }
                        .entries
                        .joinToString(", ") { (page, list) -> "Pág. $page (${list.size}x)" }

                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = signer.fullName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = NavyPrimary
                                )
                                SignerClassificationBadge(signer.classification)
                            }
                            Text(
                                text = "${signer.email} • ${signer.phone}",
                                fontSize = 11.5.sp,
                                color = Color(0xFF475569)
                            )
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                SignatureTypeBadge(signer.signatureType)
                                Text(
                                    text = "Campos: $pagesSummary",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NavyPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Prévia visual do Documento Final antes do envio
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFDCE3F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Prévia do Documento Final (Pág. ${wizardState.currentPageIndex + 1}/${wizardState.pageCount})",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )
                    Row {
                        TextButton(
                            onClick = { onSetPageIndex(wizardState.currentPageIndex - 1) },
                            enabled = wizardState.currentPageIndex > 0
                        ) {
                            Text("◀ Ant.")
                        }
                        TextButton(
                            onClick = { onSetPageIndex(wizardState.currentPageIndex + 1) },
                            enabled = wizardState.currentPageIndex < wizardState.pageCount - 1
                        ) {
                            Text("Próx. ▶")
                        }
                    }
                }

                PdfPageInteractiveCanvas(
                    pdfFilePath = wizardState.internalPdfPath,
                    pageIndex = wizardState.currentPageIndex,
                    fieldsOnPage = wizardState.signatureFields.filter { it.pageIndex == wizardState.currentPageIndex },
                    isEditable = false
                )
            }
        }

        // 4. Mensagem que acompanhará o convite
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFDCE3F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Mensagem que Acompanhará o Convite",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )
                OutlinedTextField(
                    value = wizardState.invitationMessage,
                    onValueChange = onUpdateInvitationMessage,
                    label = { Text("Mensagem aos signatários") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )

                // Opção para demonstrar tratamento de erro e retry sem duplicar solicitações (Item 5)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleSimulateFailure(!wizardState.simulateFailureOnNextAttempt) }
                ) {
                    Checkbox(
                        checked = wizardState.simulateFailureOnNextAttempt,
                        onCheckedChange = onToggleSimulateFailure
                    )
                    Text(
                        text = "Simular falha de rede na 1ª tentativa (para demonstrar recuperação/retry sem duplicar solicitações)",
                        fontSize = 11.5.sp,
                        color = Color(0xFF475569)
                    )
                }
            }
        }

        // Exibição de falha e botão de Nova Tentativa (Retry Idempotente)
        if (wizardState.dispatchError != null) {
            Surface(
                color = Color(0xFFFFEBEE),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, ErrorRed),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = ErrorRed
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Não foi possível concluir o disparo",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = ErrorRed
                        )
                    }
                    Text(
                        text = wizardState.dispatchError,
                        fontSize = 12.sp,
                        color = Color(0xFF7F1D1D)
                    )
                    Button(
                        onClick = onDispatch,
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tentar Novamente (Sem Duplicar Solicitações)")
                    }
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = onPreviousStep,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            ) {
                Text("Voltar")
            }
            Button(
                onClick = onDispatch,
                enabled = !wizardState.isDispatching,
                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                modifier = Modifier
                    .weight(1.8f)
                    .height(50.dp)
            ) {
                if (wizardState.isDispatching) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Confirmar e Disparar Solicitações", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
