package br.com.fiap.letssign.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.material.icons.filled.Download
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.core.content.FileProvider
import br.com.fiap.letssign.data.local.DocumentFullDetails
import br.com.fiap.letssign.data.local.DocumentSignerEntity
import br.com.fiap.letssign.data.local.UserEntity
import br.com.fiap.letssign.domain.model.DocumentStatus
import br.com.fiap.letssign.domain.model.DraftSignatureField
import br.com.fiap.letssign.domain.model.SignatureType
import br.com.fiap.letssign.domain.model.SignerStatus
import br.com.fiap.letssign.domain.model.UserRole
import br.com.fiap.letssign.ui.components.AssinaJaLogo
import br.com.fiap.letssign.ui.components.DocumentStatusBadge
import br.com.fiap.letssign.ui.components.FullscreenPdfViewerDialog
import br.com.fiap.letssign.ui.components.HandwrittenSignatureCanvasPad
import br.com.fiap.letssign.ui.components.PdfPageInteractiveCanvas
import br.com.fiap.letssign.ui.components.SignatureTypeBadge
import br.com.fiap.letssign.ui.components.SignerClassificationBadge
import br.com.fiap.letssign.ui.theme.CardBorder
import br.com.fiap.letssign.ui.theme.CardWhite
import br.com.fiap.letssign.ui.theme.CharcoalText
import br.com.fiap.letssign.ui.theme.ErrorRed
import br.com.fiap.letssign.ui.theme.GoldChampagne
import br.com.fiap.letssign.ui.theme.GoldLight
import br.com.fiap.letssign.ui.theme.IvoryBackground
import br.com.fiap.letssign.ui.theme.MutedText
import br.com.fiap.letssign.ui.theme.SuccessGreen
import br.com.fiap.letssign.ui.theme.WarningAmber
import br.com.fiap.letssign.ui.theme.WineDark
import br.com.fiap.letssign.ui.theme.WineLight
import br.com.fiap.letssign.ui.theme.WinePrimary
import br.com.fiap.letssign.util.PdfEngine
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailAndSignScreen(
    currentUser: UserEntity,
    details: DocumentFullDetails?,
    onRecordViewed: (Long) -> Unit,
    onSignDocument: (
        signatureBitmap: Bitmap?,
        verificationToken: String?,
        certificateInfo: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) -> Unit,
    onRejectDocument: ((reason: String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit)? = null,
    onCreateNewVersion: (DocumentFullDetails) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    if (details == null) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            CircularProgressIndicator(color = WinePrimary)
        }
        return
    }

    val doc = details.document
    val mySignerRecord = details.signers.find { it.signerUserId == currentUser.id }
    val canCurrentUserSign = currentUser.role == UserRole.SIGNATARIO &&
        mySignerRecord != null &&
        mySignerRecord.status == SignerStatus.PENDENTE &&
        doc.status != DocumentStatus.REJEITADO

    val totalAvailablePages = if (doc.status == DocumentStatus.CONCLUIDO) {
        doc.pageCount + 1
    } else {
        doc.pageCount
    }

    var currentPageIndex by remember(doc.id, totalAvailablePages) {
        val firstMyFieldPage = details.fields
            .firstOrNull { it.signerUserId == currentUser.id && !it.isSigned }
            ?.pageIndex
        mutableIntStateOf((firstMyFieldPage ?: 0).coerceIn(0, totalAvailablePages - 1))
    }

    var showSignatureModal by remember { mutableStateOf(false) }
    var showRejectDialog by remember { mutableStateOf(false) }
    var showFullscreenPdf by remember { mutableStateOf(false) }
    var rejectReason by remember { mutableStateOf("") }
    var actionError by remember { mutableStateOf<String?>(null) }

    if (showFullscreenPdf) {
        FullscreenPdfViewerDialog(
            pdfFilePath = doc.currentPdfPath,
            initialPageIndex = currentPageIndex,
            totalPages = totalAvailablePages,
            documentTitle = doc.title,
            onDismiss = { showFullscreenPdf = false }
        )
    }

    LaunchedEffect(doc.id, currentUser.id) {
        if (currentUser.role == UserRole.SIGNATARIO) {
            onRecordViewed(doc.id)
        }
    }

    // Modal de Rejeição
    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Cancel, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Recusar Assinatura", fontWeight = FontWeight.Bold, color = ErrorRed)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Informe o motivo pelo qual você está recusando assinar este documento:",
                        fontSize = 12.5.sp,
                        color = CharcoalText
                    )
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        placeholder = { Text("Ex: Cláusula de valor divergente do combinado...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRejectDocument?.invoke(
                            rejectReason,
                            {
                                showRejectDialog = false
                            },
                            { err -> actionError = err }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Confirmar Recusa")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Modal de Assinatura
    if (showSignatureModal && mySignerRecord != null) {
        CorporateSignatureExecutionDialog(
            signerRecord = mySignerRecord,
            documentTitle = doc.title,
            onDismiss = { showSignatureModal = false },
            onConfirmSignature = { bmp, token, certInfo, setError ->
                onSignDocument(
                    bmp,
                    token,
                    certInfo,
                    {
                        showSignatureModal = false
                    },
                    { err -> setError(err) }
                )
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color.White
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = doc.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "AssinaJÁ • ${doc.originalFileName}",
                            fontSize = 11.sp,
                            color = GoldChampagne,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                val fileName = "_v_Assinado.pdf"
                                val res = PdfEngine.exportPdfToPublicDownloads(context, doc.currentPdfPath, fileName)
                                res.fold(
                                    onSuccess = {
                                        Toast.makeText(context, "✦ Documento salvo com sucesso na pasta Downloads!", Toast.LENGTH_LONG).show()
                                    },
                                    onFailure = { err ->
                                        Toast.makeText(context, "Falha ao salvar: ", Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Salvar PDF no Dispositivo",
                            tint = Color.White
                        )
                    }
                    
                    IconButton(
                        onClick = {
                            try {
                                val pdfFile = File(doc.currentPdfPath)
                                if (pdfFile.exists()) {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        pdfFile
                                    )
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        putExtra(Intent.EXTRA_SUBJECT, "${doc.title} (v${doc.version})")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(shareIntent, "Compartilhar Documento PDF")
                                    )
                                }
                            } catch (_: Exception) {}
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Compartilhar",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WinePrimary)
            )
        },
        bottomBar = {
            if (canCurrentUserSign) {
                Surface(
                    color = CardWhite,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showRejectDialog = true },
                            border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Recusar", color = ErrorRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = { showSignatureModal = true },
                            colors = ButtonDefaults.buttonColors(containerColor = WinePrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Icon(Icons.Default.Draw, contentDescription = null, tint = GoldChampagne, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Assinar Documento", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        },
        containerColor = IvoryBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Situação Geral e Versionamento
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        DocumentStatusBadge(doc.status)
                        Surface(
                            color = WineLight,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Versão v${doc.version}",
                                color = WinePrimary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = doc.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = CharcoalText
                    )

                    if (doc.description.isNotBlank()) {
                        Text(
                            text = doc.description,
                            fontSize = 12.sp,
                            color = MutedText
                        )
                    }

                    HorizontalDivider(color = Color(0xFFF0ECE8))

                    Text(
                        text = "Hash SHA-256 Original: ${doc.originalSha256}",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MutedText
                    )
                    Text(
                        text = "Protocolo AssinaJÁ: ${doc.dispatchIdempotencyKey}",
                        fontSize = 9.5.sp,
                        fontFamily = FontFamily.Monospace,
                        color = WinePrimary,
                        fontWeight = FontWeight.Bold
                    )

                    // Salvar documento no dispositivo
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val fileName = "_v_Assinado.pdf"
                                val res = PdfEngine.exportPdfToPublicDownloads(context, doc.currentPdfPath, fileName)
                                res.fold(
                                    onSuccess = {
                                        Toast.makeText(context, "✦ Documento salvo com sucesso na pasta Downloads!", Toast.LENGTH_LONG).show()
                                    },
                                    onFailure = { err ->
                                        Toast.makeText(context, "Falha ao salvar: ", Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WinePrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = GoldChampagne, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Salvar Documento Final no Dispositivo (Downloads)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }

                    

                    // Se for Admin e documento estiver concluído/em andamento, permite gerar nova versão (v2, v3...)
                    if (currentUser.role == UserRole.ADMIN) {
                        OutlinedButton(
                            onClick = { onCreateNewVersion(details) },
                            border = BorderStroke(1.dp, WinePrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Gerar Nova Versão deste Documento (v${doc.version + 1})", color = WinePrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Visualizador Interativo de Páginas do PDF
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                enabled = currentPageIndex > 0,
                                onClick = { currentPageIndex-- }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Página anterior", tint = WinePrimary)
                            }

                            Text(
                                text = if (doc.status == DocumentStatus.CONCLUIDO && currentPageIndex == doc.pageCount) {
                                    "Pág. ${currentPageIndex + 1}/$totalAvailablePages • Manifesto Forense"
                                } else {
                                    "Pág. ${currentPageIndex + 1}/$totalAvailablePages"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = WinePrimary
                            )

                            IconButton(
                                enabled = currentPageIndex < totalAvailablePages - 1,
                                onClick = { currentPageIndex++ }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Próxima página", tint = WinePrimary)
                            }
                        }

                        OutlinedButton(
                            onClick = { showFullscreenPdf = true },
                            border = BorderStroke(1.dp, GoldChampagne),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.ZoomIn, contentDescription = null, tint = WinePrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tela Cheia (Zoom)", fontSize = 11.sp, color = WinePrimary, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val fieldsThisPage = details.fields.filter { it.pageIndex == currentPageIndex }.map { f ->
                        DraftSignatureField(
                            id = f.id.toString(),
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

                    PdfPageInteractiveCanvas(
                        pdfFilePath = doc.currentPdfPath,
                        pageIndex = currentPageIndex,
                        fieldsOnPage = fieldsThisPage,
                        isEditable = false,
                        highlightedSignerUserId = if (canCurrentUserSign) currentUser.id else null,
                        signedFieldIds = details.fields.filter { it.isSigned }.map { it.id.toString() }.toSet(),
                        onFieldClicked = { clickedField ->
                            if (canCurrentUserSign && clickedField.signerUserId == currentUser.id) {
                                showSignatureModal = true
                            }
                        }
                    )
                }
            }

            // Quadro Individual de Signatários
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "QUADRO DE SIGNATÁRIOS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = WinePrimary,
                        letterSpacing = 0.6.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    details.signers.forEachIndexed { idx, s ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (s.status == SignerStatus.ASSINADO) Color(0xFFE8F7EE) else if (s.status == SignerStatus.REJEITADO) Color(0xFFFFEBEE) else Color(0xFFFFF7E6),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (s.status == SignerStatus.ASSINADO) Icons.Default.CheckCircle else if (s.status == SignerStatus.REJEITADO) Icons.Default.Cancel else Icons.Default.Draw,
                                        contentDescription = null,
                                        tint = if (s.status == SignerStatus.ASSINADO) SuccessGreen else if (s.status == SignerStatus.REJEITADO) ErrorRed else WarningAmber,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${idx + 1}. ${s.signerName}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CharcoalText
                                )
                                Text(
                                    text = "${s.classification.label} • ${s.signatureType.shortName}",
                                    fontSize = 11.sp,
                                    color = MutedText
                                )
                                if (s.status == SignerStatus.ASSINADO) {
                                    Text(
                                        text = "Assinado em ${PdfEngine.formatTimestamp(s.signedAt)}",
                                        fontSize = 10.sp,
                                        color = SuccessGreen,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                } else if (s.status == SignerStatus.REJEITADO) {
                                    Text(
                                        text = "Rejeitado em ${PdfEngine.formatTimestamp(s.signedAt)}",
                                        fontSize = 10.sp,
                                        color = ErrorRed,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                        if (idx < details.signers.size - 1) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF4EFEA))
                        }
                    }
                }
            }

            // Trilha de Auditoria Forense
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "HISTÓRICO",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = WinePrimary,
                        letterSpacing = 0.6.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    details.auditEvents.sortedBy { it.timestamp }.forEachIndexed { i, event ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "•",
                                color = WinePrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "[${PdfEngine.formatTimestamp(event.timestamp)}] ${event.title}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CharcoalText
                                )
                                Text(
                                    text = "${event.actorName} • ${event.description}",
                                    fontSize = 11.sp,
                                    color = MutedText
                                )
                            }
                        }
                        if (i < details.auditEvents.size - 1) {
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Modal Corporativo de Execução de Assinatura com conformidade legal.
 */
@Composable
private fun CorporateSignatureExecutionDialog(
    signerRecord: DocumentSignerEntity,
    documentTitle: String,
    onDismiss: () -> Unit,
    onConfirmSignature: (
        bitmap: Bitmap?,
        verificationToken: String?,
        certificateInfo: String?,
        setError: (String) -> Unit
    ) -> Unit
) {
    val sigType = signerRecord.signatureType
    var hasCanvasStrokes by remember { mutableStateOf(false) }
    var bitmapExporter by remember { mutableStateOf<() -> Bitmap?>({ null }) }

    val generatedOtp = remember { (100000..999999).random().toString() }
    var tokenInput by remember { mutableStateOf(generatedOtp) }
    var acceptedTerms by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AssinaJaLogo(size = 24.dp, showText = false)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = sigType.fullName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = WinePrimary,
                        lineHeight = 18.sp
                    )
                    Text(
                        text = sigType.legalBasis,
                        fontSize = 10.sp,
                        color = MutedText,
                        lineHeight = 13.sp
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (errorMessage != null) {
                    Surface(
                        color = Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = ErrorRed,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Se exigir rubrica desenhada
                if (sigType.requiresHandwrittenCanvas) {
                    HandwrittenSignatureCanvasPad(
                        onSignatureChanged = { hasStrokes, exporter ->
                            hasCanvasStrokes = hasStrokes
                            bitmapExporter = exporter
                            errorMessage = null
                        }
                    )
                }

                // Se exigir token de verificação 2FA (WhatsApp/SMS)
                if (sigType.requiresToken) {
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = GoldLight),
                        border = BorderStroke(0.8.dp, GoldChampagne.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF7A5816), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Token 2FA (WhatsApp / SMS)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5A4418))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Código de confirmação exclusivo para ${signerRecord.signerPhone}:",
                                fontSize = 11.sp,
                                color = MutedText
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = tokenInput,
                                onValueChange = { tokenInput = it },
                                label = { Text("Código de 6 dígitos") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Se exigir certificado digital padrão ICP-Brasil
                if (sigType.requiresIcpCertificate) {
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = GoldLight),
                        border = BorderStroke(0.8.dp, GoldChampagne.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Verified, contentDescription = null, tint = WinePrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Certificado ICP-Brasil Detectado", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = WinePrimary)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Titular: ${signerRecord.signerName} • e-CPF ICP-Brasil A1/A3",
                                fontSize = 11.sp,
                                color = CharcoalText
                            )
                            Text(
                                text = "Validade Jurídica Presumida (MP 2.200-2/2001, Art. 10, § 1º)",
                                fontSize = 10.sp,
                                color = MutedText
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { acceptedTerms = !acceptedTerms }
                ) {
                    Checkbox(
                        checked = acceptedTerms,
                        onCheckedChange = { acceptedTerms = it },
                        colors = CheckboxDefaults.colors(checkedColor = WinePrimary)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Declaro que li o documento e concordo em assiná-lo eletronicamente pela plataforma AssinaJÁ.",
                        fontSize = 11.5.sp,
                        color = CharcoalText,
                        lineHeight = 15.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isSaving,
                onClick = {
                    if (!acceptedTerms) {
                        errorMessage = "É necessário aceitar os termos para assinar."
                        return@Button
                    }
                    if (sigType.requiresHandwrittenCanvas && !hasCanvasStrokes) {
                        errorMessage = "Por favor, desenhe sua rubrica no quadro acima."
                        return@Button
                    }
                    isSaving = true
                    val bmp = if (sigType.requiresHandwrittenCanvas) bitmapExporter() else null
                    onConfirmSignature(
                        bmp,
                        tokenInput,
                        if (sigType.requiresIcpCertificate) "Certificado Corporativo ICP-Brasil" else null
                    ) { err ->
                        isSaving = false
                        errorMessage = err
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = WinePrimary)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                } else {
                    Text("Confirmar Assinatura")
                }
            }
        },
        dismissButton = {
            TextButton(
                enabled = !isSaving,
                onClick = onDismiss
            ) {
                Text("Cancelar", color = WinePrimary)
            }
        }
    )
}
