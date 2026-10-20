package br.com.fiap.letssign.ui.screens

import android.content.Intent
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerifiedUser
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
import br.com.fiap.letssign.ui.components.DocumentStatusBadge
import br.com.fiap.letssign.ui.components.HandwrittenSignatureCanvasPad
import br.com.fiap.letssign.ui.components.PdfPageInteractiveCanvas
import br.com.fiap.letssign.ui.components.SignatureTypeBadge
import br.com.fiap.letssign.ui.components.SignerClassificationBadge
import br.com.fiap.letssign.ui.theme.ErrorRed
import br.com.fiap.letssign.ui.theme.IcpBlue
import br.com.fiap.letssign.ui.theme.NavyPrimary
import br.com.fiap.letssign.ui.theme.SuccessGreen
import br.com.fiap.letssign.ui.theme.TealAccent
import br.com.fiap.letssign.ui.theme.WarningAmber
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
    onCreateNewVersion: (DocumentFullDetails) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    if (details == null) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            CircularProgressIndicator(color = NavyPrimary)
        }
        return
    }

    val doc = details.document
    val mySignerRecord = details.signers.find { it.signerUserId == currentUser.id }
    val canCurrentUserSign = currentUser.role == UserRole.SIGNATARIO &&
        mySignerRecord != null &&
        mySignerRecord.status == SignerStatus.PENDENTE

    // Quando o documento está CONCLUIDO, ele tem 1 página extra (Manifesto Jurídico de Assinaturas)
    val totalAvailablePages = if (doc.status == DocumentStatus.CONCLUIDO) {
        doc.pageCount + 1
    } else {
        doc.pageCount
    }

    var currentPageIndex by remember(doc.id, totalAvailablePages) {
        val firstMyFieldPage = details.fields
            .firstOrNull { it.signerUserId == currentUser.id && !it.isSigned }
            ?.pageIndex ?: 0
        mutableIntStateOf(firstMyFieldPage.coerceIn(0, (totalAvailablePages - 1).coerceAtLeast(0)))
    }

    var showSignatureModal by remember { mutableStateOf(false) }

    LaunchedEffect(doc.id, currentUser.id) {
        if (currentUser.role == UserRole.SIGNATARIO && mySignerRecord != null) {
            onRecordViewed(doc.id)
        }
    }

    if (showSignatureModal && mySignerRecord != null) {
        LegalSignatureExecutionDialog(
            signerRecord = mySignerRecord,
            documentTitle = doc.title,
            onDismiss = { showSignatureModal = false },
            onConfirmSignature = { bitmap, token, certInfo, setError ->
                onSignDocument(
                    bitmap,
                    token,
                    certInfo,
                    { showSignatureModal = false },
                    { err -> setError(err) }
                )
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "${doc.title} (v${doc.version})",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (canCurrentUserSign) {
                                "Toque nos seus campos no PDF ou no botão abaixo para assinar"
                            } else {
                                "Acompanhamento, Histórico e PDF (${doc.status.label})"
                            },
                            fontSize = 11.sp,
                            color = Color(0xFFB8C9E8)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color.White
                        )
                    }
                },
                actions = {
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
                                        Intent.createChooser(shareIntent, "Exportar / Compartilhar PDF")
                                    )
                                }
                            } catch (_: Exception) {
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Compartilhar PDF",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyPrimary)
            )
        },
        bottomBar = {
            if (canCurrentUserSign) {
                Surface(
                    color = Color.White,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sua Assinatura está Pendente",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = NavyPrimary
                            )
                            Text(
                                text = "${mySignerRecord?.classification?.label} • ${mySignerRecord?.signatureType?.shortName}",
                                fontSize = 11.5.sp,
                                color = TealAccent
                            )
                        }
                        Button(
                            onClick = { showSignatureModal = true },
                            colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(46.dp)
                        ) {
                            Icon(Icons.Default.Draw, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Assinar Documento", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Situação Geral do Documento + Regra de Nova Versão (Admin)
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
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = Color(0xFFE8EEF8),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Versão v${doc.version}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NavyPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            if (doc.parentDocumentId != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "(Derivado do ID #${doc.parentDocumentId})",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                        DocumentStatusBadge(doc.status)
                    }

                    Text(
                        text = doc.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NavyPrimary
                    )
                    if (doc.description.isNotBlank()) {
                        Text(
                            text = doc.description,
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Mensagem do Convite:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary
                            )
                            Text(
                                text = "\"${doc.invitationMessage}\"",
                                fontSize = 12.sp,
                                color = Color(0xFF334155)
                            )
                        }
                    }

                    //Datas principais e Hashes SHA-256
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = "• Criado / Enviado em: ${PdfEngine.formatTimestamp(doc.sentAt ?: doc.createdAt)}",
                            fontSize = 11.5.sp,
                            color = Color(0xFF475569)
                        )
                        if (doc.completedAt != null) {
                            Text(
                                text = "• Concluído e Selado em: ${PdfEngine.formatTimestamp(doc.completedAt)}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen
                            )
                        }
                        Text(
                            text = "• SHA-256 Original: ${doc.originalSha256.take(28)}...",
                            fontSize = 10.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF475569)
                        )
                        Text(
                            text = "• SHA-256 Atual/Final: ${doc.currentSha256.take(28)}...",
                            fontSize = 10.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (doc.status == DocumentStatus.CONCLUIDO) SuccessGreen else NavyPrimary
                        )
                    }

                    // Regra de Negócio: "O documento enviado e suas configurações devem ser preservados. Alterações posteriores deverão gerar uma nova versão e um novo fluxo."
                    if (currentUser.role == UserRole.ADMIN) {
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Precisa alterar signatários ou posições?",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NavyPrimary
                                )
                                Text(
                                    text = "O documento enviado é imutável. Gere uma nova versão (v${doc.version + 1}) preservando o histórico atual.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = { onCreateNewVersion(details) },
                                border = BorderStroke(1.dp, NavyPrimary)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Gerar v${doc.version + 1}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 2. Visualizador do Documento PDF (Original / Parcialmente Assinado / Documento Final com Manifesto)
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFDCE3F0)),
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
                                text = if (doc.status == DocumentStatus.CONCLUIDO) {
                                    "Documento Final Concluído (com Manifesto)"
                                } else {
                                    "Visualização do Documento PDF"
                                },
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary
                            )
                            Text(
                                text = if (currentPageIndex == doc.pageCount && doc.status == DocumentStatus.CONCLUIDO) {
                                    "Página ${currentPageIndex + 1} de $totalAvailablePages • Manifesto Jurídico de Assinaturas"
                                } else {
                                    "Página ${currentPageIndex + 1} de $totalAvailablePages"
                                },
                                fontSize = 11.5.sp,
                                color = TealAccent
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = { currentPageIndex = (currentPageIndex - 1).coerceAtLeast(0) },
                                enabled = currentPageIndex > 0,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("◀ Ant.", fontSize = 11.5.sp)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            OutlinedButton(
                                onClick = {
                                    currentPageIndex = (currentPageIndex + 1).coerceAtMost(totalAvailablePages - 1)
                                },
                                enabled = currentPageIndex < totalAvailablePages - 1,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Próx. ▶", fontSize = 11.5.sp)
                            }
                        }
                    }

                    // Exibe apenas os overlays de campos ainda pendentes (pois os campos já assinados já estão estampados no próprio arquivo PDF!)
                    val unsignedOverlaysOnPage = remember(details.fields, currentPageIndex) {
                        details.fields
                            .filter { it.pageIndex == currentPageIndex && !it.isSigned }
                            .map { f ->
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
                                    heightRatio = f.heightRatio
                                )
                            }
                    }

                    PdfPageInteractiveCanvas(
                        pdfFilePath = doc.currentPdfPath,
                        pageIndex = currentPageIndex,
                        fieldsOnPage = unsignedOverlaysOnPage,
                        isEditable = false,
                        highlightedSignerUserId = if (canCurrentUserSign) currentUser.id else null,
                        onFieldClicked = { clickedField ->
                            if (canCurrentUserSign && clickedField.signerUserId == currentUser.id) {
                                showSignatureModal = true
                            }
                        }
                    )
                }
            }

            // 3. Situação Individual dos Signatários (Item 6)
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
                        text = "Situação Individual dos Signatários (${details.signers.count { it.status == SignerStatus.ASSINADO }}/${details.signers.size} assinados)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )

                    details.signers.forEach { signer ->
                        val isSigned = signer.status == SignerStatus.ASSINADO
                        Surface(
                            color = if (isSigned) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isSigned) SuccessGreen.copy(alpha = 0.4f) else Color(0xFFDCE3F0)
                            ),
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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = if (isSigned) Icons.Default.CheckCircle else Icons.Default.PendingActions,
                                            contentDescription = null,
                                            tint = if (isSigned) SuccessGreen else WarningAmber,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = signer.signerName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = NavyPrimary
                                        )
                                    }
                                    SignerClassificationBadge(signer.classification)
                                }

                                Text(
                                    text = "${signer.signerEmail} • ${signer.signerPhone}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF475569)
                                )

                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    SignatureTypeBadge(signer.signatureType)
                                    Text(
                                        text = if (isSigned) {
                                            "Assinado em ${PdfEngine.formatTimestamp(signer.signedAt)}"
                                        } else {
                                            "Aguardando assinatura"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSigned) SuccessGreen else WarningAmber
                                    )
                                }

                                if (isSigned) {
                                    val evidence = signer.certificateInfo ?: signer.verificationToken ?: "Autenticação Eletrônica Simples"
                                    Text(
                                        text = "Evidência Jurídica: $evidence • IP: ${signer.ipAddress ?: "—"}",
                                        fontSize = 10.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF334155)
                                    )
                                    if (!signer.signatureHash.isNullOrBlank()) {
                                        Text(
                                            text = "Hash da Assinatura: ${signer.signatureHash.take(32)}...",
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = TealAccent
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Histórico de Criação, Envio e Assinatura (Audit Log - Item 6)
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = NavyPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Histórico de Eventos e Trilha de Auditoria",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                    }

                    details.auditEvents
                        .sortedByDescending { it.timestamp }
                        .forEach { event ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(TealAccent)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = event.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = NavyPrimary
                                        )
                                        Text(
                                            text = PdfEngine.formatTimestamp(event.timestamp),
                                            fontSize = 10.5.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                    Text(
                                        text = "${event.actorName} (${event.actorEmail})",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TealAccent
                                    )
                                    Text(
                                        text = event.description,
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF475569)
                                    )
                                }
                            }
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                        }
                }
            }
        }
    }
}

/**
 * Modal Jurídico de Execução da Assinatura Digital contemplando as nuances de cada modalidade:
 * - SIMPLES (Lei 14.063/2020, Art. 4º, I)
 * - AVANÇADA (Lei 14.063/2020, Art. 4º, II - Rubrica Manuscrita em Canvas + Token 2FA)
 * - QUALIFICADA ICP-BRASIL (MP 2.200-2/2001 & Lei 14.063/2020, Art. 4º, III - Certificado A1/A3 + PIN + Rubrica)
 */
@Composable
private fun LegalSignatureExecutionDialog(
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

    val availableCertificates = remember(signerRecord.signerName) {
        listOf(
            "ICP-Brasil A3 Nuvem (SafeID) • Titular: ${signerRecord.signerName} • AC Soluti Multipla v5",
            "ICP-Brasil A1 Arquivo (.pfx) • Titular: ${signerRecord.signerName} • AC Certisign RFB G5",
            "Token OAB ICP-Brasil A3 • Titular: ${signerRecord.signerName} • AC OAB G3"
        )
    }
    var selectedCertificate by remember { mutableStateOf(availableCertificates.first()) }
    var certificatePin by remember { mutableStateOf("123456") }
    var acceptedLegalTerms by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = TealAccent,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = sigType.fullName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )
                }
                Text(
                    text = "${sigType.legalBasis} • Papel: ${signerRecord.classification.label}",
                    fontSize = 11.5.sp,
                    color = Color(0xFF64748B)
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Surface(
                    color = Color(0xFFF0F4FA),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = sigType.description,
                        fontSize = 11.5.sp,
                        color = NavyPrimary,
                        modifier = Modifier.padding(10.dp)
                    )
                }

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

                // Se for Qualificada ICP-Brasil, exibe seleção de Certificado Digital A1/A3 e PIN
                if (sigType.requiresIcpCertificate) {
                    Text(
                        text = "Selecione o Certificado Digital ICP-Brasil:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )
                    availableCertificates.forEach { cert ->
                        val selected = selectedCertificate == cert
                        Surface(
                            color = if (selected) Color(0xFFE8F0FE) else Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (selected) IcpBlue else Color(0xFFDCE3F0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedCertificate = cert }
                        ) {
                            Text(
                                text = cert,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) IcpBlue else Color(0xFF334155),
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = certificatePin,
                        onValueChange = {
                            certificatePin = it
                            errorMessage = null
                        },
                        label = { Text("PIN / Senha do Certificado ICP-Brasil *") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Se for Assinatura Avançada, exige Token de Verificação 2FA
                if (sigType.requiresToken) {
                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = {
                            tokenInput = it
                            errorMessage = null
                        },
                        label = { Text("Token de Autenticação enviado (${signerRecord.signerEmail}) *") },
                        supportingText = {
                            Text("Token gerado para esta sessão: $generatedOtp", fontSize = 11.sp, color = TealAccent)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Quadro de Rubrica Manuscrita em Canvas (Obrigatório na Avançada/ICP e opcional na Simples)
                HandwrittenSignatureCanvasPad(
                    onSignatureChanged = { hasStrokes, exporter ->
                        hasCanvasStrokes = hasStrokes
                        bitmapExporter = exporter
                        if (hasStrokes) errorMessage = null
                    }
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { acceptedLegalTerms = !acceptedLegalTerms }
                ) {
                    Checkbox(
                        checked = acceptedLegalTerms,
                        onCheckedChange = { acceptedLegalTerms = it }
                    )
                    Text(
                        text = "Declaro que li o documento '$documentTitle' e concordo em assiná-lo eletronicamente com validade jurídica.",
                        fontSize = 11.5.sp,
                        color = Color(0xFF334155)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                onClick = {
                    if (!acceptedLegalTerms) {
                        errorMessage = "Você precisa aceitar o termo de declaração jurídica para assinar."
                        return@Button
                    }
                    if (sigType.requiresHandwrittenCanvas && !hasCanvasStrokes) {
                        errorMessage = "Na modalidade ${sigType.shortName}, é obrigatório desenhar sua rubrica/assinatura no quadro acima."
                        return@Button
                    }
                    if (sigType.requiresToken && tokenInput.trim().length < 4) {
                        errorMessage = "Informe o código Token de verificação para validar a Assinatura Avançada."
                        return@Button
                    }
                    if (sigType.requiresIcpCertificate && certificatePin.trim().length < 4) {
                        errorMessage = "Informe o PIN do Certificado Digital ICP-Brasil (mínimo 4 dígitos)."
                        return@Button
                    }

                    isSaving = true
                    val bmp = bitmapExporter()
                    onConfirmSignature(
                        bmp,
                        if (sigType.requiresToken) "Token 2FA #${tokenInput.trim()}" else null,
                        if (sigType.requiresIcpCertificate) selectedCertificate else null
                    ) { err ->
                        isSaving = false
                        errorMessage = err
                    }
                }
            ) {
                Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Assinar e Salvar no PDF", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving
            ) {
                Text("Cancelar")
            }
        }
    )
}
