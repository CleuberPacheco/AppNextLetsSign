package br.com.fiap.letssign.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.fiap.letssign.data.local.UserEntity
import br.com.fiap.letssign.domain.model.DocumentStatus
import br.com.fiap.letssign.domain.model.DraftSignatureField
import br.com.fiap.letssign.domain.model.SignatureType
import br.com.fiap.letssign.domain.model.SignerClassification
import br.com.fiap.letssign.ui.theme.ErrorRed
import br.com.fiap.letssign.ui.theme.GoldLegal
import br.com.fiap.letssign.ui.theme.IcpBlue
import br.com.fiap.letssign.ui.theme.NavyPrimary
import br.com.fiap.letssign.ui.theme.SuccessGreen
import br.com.fiap.letssign.ui.theme.TealAccent
import br.com.fiap.letssign.ui.theme.WarningAmber
import br.com.fiap.letssign.util.PdfEngine
import br.com.fiap.letssign.util.PdfPageBitmap
import kotlin.math.roundToInt

@Composable
fun DocumentStatusBadge(status: DocumentStatus) {
    val (bgColor, textColor) = when (status) {
        DocumentStatus.CONCLUIDO -> Color(0xFFDFF6E8) to SuccessGreen
        DocumentStatus.PARCIALMENTE_ASSINADO -> Color(0xFFFFF3CD) to WarningAmber
        DocumentStatus.ENVIADO -> Color(0xFFE0ECFF) to IcpBlue
        DocumentStatus.RASCUNHO -> Color(0xFFEAECEF) to Color(0xFF495057)
    }
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.35f))
    ) {
        Text(
            text = status.label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun SignerClassificationBadge(classification: SignerClassification) {
    val (bg, fg) = when (classification) {
        SignerClassification.CONTRATANTE -> Color(0xFFE3F2FD) to Color(0xFF0D47A1)
        SignerClassification.CONTRATADO -> Color(0xFFE0F2F1) to Color(0xFF004D40)
        SignerClassification.TESTEMUNHA -> Color(0xFFFFF8E1) to GoldLegal
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = classification.label.uppercase(),
            color = fg,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun SignatureTypeBadge(signatureType: SignatureType) {
    val (bg, fg) = when (signatureType) {
        SignatureType.QUALIFICADA_ICP -> Color(0xFFE8F0FE) to IcpBlue
        SignatureType.AVANCADA -> Color(0xFFE0F2F1) to TealAccent
        SignatureType.SIMPLES -> Color(0xFFECEFF1) to Color(0xFF37474F)
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(0.8.dp, fg.copy(alpha = 0.4f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Icon(
                imageVector = Icons.Default.VerifiedUser,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = signatureType.shortName,
                color = fg,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Modal de Cadastro e Edição de Signatário pelo Admin (Item 2 das Funcionalidades Obrigatórias).
 */
@Composable
fun SignerFormDialog(
    initialSigner: UserEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        editingId: Long?,
        fullName: String,
        email: String,
        phone: String,
        password: String,
        legalDocument: String,
        setError: (String) -> Unit
    ) -> Unit
) {
    var fullName by remember(initialSigner) { mutableStateOf(initialSigner?.fullName ?: "") }
    var email by remember(initialSigner) { mutableStateOf(initialSigner?.email ?: "") }
    var phone by remember(initialSigner) { mutableStateOf(initialSigner?.phone ?: "") }
    var password by remember(initialSigner) { mutableStateOf(initialSigner?.password ?: "") }
    var legalDocument by remember(initialSigner) { mutableStateOf(initialSigner?.legalDocument ?: "") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialSigner == null) "Cadastrar Novo Signatário" else "Editar Signatário",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "O e-mail e a senha cadastrados serão as credenciais de login para este signatário assinar os documentos no app.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

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

                OutlinedTextField(
                    value = fullName,
                    onValueChange = {
                        fullName = it
                        errorMessage = null
                    },
                    label = { Text("Nome completo *") },
                    placeholder = { Text("Ex: Dra. Carolina Mendes") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        errorMessage = null
                    },
                    label = { Text("E-mail (credencial de login) *") },
                    placeholder = { Text("Ex: carolina@escritorio.adv.br") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        phone = it
                        errorMessage = null
                    },
                    label = { Text("Telefone / WhatsApp *") },
                    placeholder = { Text("Ex: (11) 98877-6655") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    label = { Text("Senha (credencial de login) *") },
                    placeholder = { Text("Mínimo 4 caracteres") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Alternar visibilidade da senha"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = legalDocument,
                    onValueChange = { legalDocument = it },
                    label = { Text("CPF ou Registro OAB (Opcional)") },
                    placeholder = { Text("Ex: CPF 123.456.789-00 ou OAB/SP 123.456") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        initialSigner?.id,
                        fullName,
                        email,
                        phone,
                        password,
                        legalDocument
                    ) { err ->
                        errorMessage = err
                    }
                }
            ) {
                Text(if (initialSigner == null) "Cadastrar" else "Salvar Alterações")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

/**
 * Visualizador de Página do PDF com suporte a Posicionamento Interativo (Drag & Drop)
 * preservando coordenadas relativas normalizadas (0.0f a 1.0f) em relação à página do PDF,
 * e garantindo que os campos permaneçam sempre dentro dos limites da página.
 */
@Composable
fun PdfPageInteractiveCanvas(
    pdfFilePath: String,
    pageIndex: Int,
    fieldsOnPage: List<DraftSignatureField>,
    isEditable: Boolean,
    highlightedSignerUserId: Long? = null,
    signedFieldIds: Set<String> = emptySet(),
    onFieldMoved: (fieldId: String, newXRatio: Float, newYRatio: Float) -> Unit = { _, _, _ -> },
    onFieldRemoved: (fieldId: String) -> Unit = {},
    onFieldClicked: (DraftSignatureField) -> Unit = {}
) {
    var pageRender by remember(pdfFilePath, pageIndex) { mutableStateOf<PdfPageBitmap?>(null) }
    var isLoading by remember(pdfFilePath, pageIndex) { mutableStateOf(true) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(pdfFilePath, pageIndex) {
        isLoading = true
        pageRender = PdfEngine.renderPdfPage(pdfFilePath, pageIndex, targetWidthPx = 1200)
        isLoading = false
    }

    val aspectRatio = remember(pageRender) {
        val pr = pageRender
        if (pr != null && pr.pdfPageHeight > 0) {
            (pr.pdfPageWidth.toFloat() / pr.pdfPageHeight.toFloat()).coerceIn(0.45f, 1.8f)
        } else {
            595f / 842f // Proporção A4 padrão
        }
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFCFD8E6)),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .onSizeChanged { containerSize = it }
        ) {
            if (isLoading) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    CircularProgressIndicator(color = NavyPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Renderizando página ${pageIndex + 1} do PDF...",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (pageRender != null) {
                Image(
                    bitmap = pageRender!!.bitmap.asImageBitmap(),
                    contentDescription = "Página ${pageIndex + 1} do PDF",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )

                // Renderiza os campos de assinatura posicionados sobre esta página usando coordenadas relativas
                if (containerSize.width > 0 && containerSize.height > 0) {
                    val containerW = containerSize.width.toFloat()
                    val containerH = containerSize.height.toFloat()
                    val density = LocalDensity.current

                    fieldsOnPage.forEach { field ->
                        DraggableSignatureFieldOverlay(
                            field = field,
                            containerWidthPx = containerW,
                            containerHeightPx = containerH,
                            density = density,
                            isEditable = isEditable,
                            isHighlightedForSigning = highlightedSignerUserId == field.signerUserId,
                            isAlreadySigned = field.id in signedFieldIds,
                            onFieldMoved = onFieldMoved,
                            onFieldRemoved = onFieldRemoved,
                            onFieldClicked = onFieldClicked
                        )
                    }
                }
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "Não foi possível renderizar esta página do PDF.",
                        color = ErrorRed,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DraggableSignatureFieldOverlay(
    field: DraftSignatureField,
    containerWidthPx: Float,
    containerHeightPx: Float,
    density: androidx.compose.ui.unit.Density,
    isEditable: Boolean,
    isHighlightedForSigning: Boolean,
    isAlreadySigned: Boolean,
    onFieldMoved: (fieldId: String, newXRatio: Float, newYRatio: Float) -> Unit,
    onFieldRemoved: (fieldId: String) -> Unit,
    onFieldClicked: (DraftSignatureField) -> Unit
) {
    // Mantém estado local fluido durante o arraste (drag) e sincroniza com o estado externo
    var localXRatio by remember(field.id, field.xRatio) { mutableFloatStateOf(field.xRatio) }
    var localYRatio by remember(field.id, field.yRatio) { mutableFloatStateOf(field.yRatio) }

    val currentOnMoved by rememberUpdatedState(onFieldMoved)
    val maxXRatio = (1f - field.widthRatio).coerceAtLeast(0f)
    val maxYRatio = (1f - field.heightRatio).coerceAtLeast(0f)

    val widthDp = with(density) { (field.widthRatio * containerWidthPx).toDp() }
    val heightDp = with(density) { (field.heightRatio * containerHeightPx).toDp() }
    val offsetX = (localXRatio * containerWidthPx).roundToInt()
    val offsetY = (localYRatio * containerHeightPx).roundToInt()

    val accentColor = when {
        isAlreadySigned -> SuccessGreen
        isHighlightedForSigning -> TealAccent
        field.signatureType == SignatureType.QUALIFICADA_ICP -> IcpBlue
        field.signatureType == SignatureType.AVANCADA -> TealAccent
        else -> NavyPrimary
    }

    val bgColor = when {
        isAlreadySigned -> Color(0xFFE8F8EE).copy(alpha = 0.92f)
        isHighlightedForSigning -> Color(0xFFE0F7FA).copy(alpha = 0.94f)
        else -> Color(0xFFEEF4FF).copy(alpha = 0.92f)
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX, offsetY) }
            .size(width = widthDp, height = heightDp)
            .shadow(if (isEditable || isHighlightedForSigning) 4.dp else 1.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(
                width = if (isHighlightedForSigning) 2.dp else 1.5.dp,
                color = accentColor,
                shape = RoundedCornerShape(8.dp)
            )
            .then(
                if (isEditable) {
                    Modifier.pointerInput(field.id, containerWidthPx, containerHeightPx, maxXRatio, maxYRatio) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaXRatio = dragAmount.x / containerWidthPx
                            val deltaYRatio = dragAmount.y / containerHeightPx
                            localXRatio = (localXRatio + deltaXRatio).coerceIn(0f, maxXRatio)
                            localYRatio = (localYRatio + deltaYRatio).coerceIn(0f, maxYRatio)
                            currentOnMoved(field.id, localXRatio, localYRatio)
                        }
                    }
                } else {
                    Modifier.clickable { onFieldClicked(field) }
                }
            )
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = when {
                            isAlreadySigned -> Icons.Default.CheckCircle
                            isEditable -> Icons.Default.OpenWith
                            else -> Icons.Default.Draw
                        },
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = field.classification.label.uppercase(),
                        color = accentColor,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isEditable) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(ErrorRed.copy(alpha = 0.14f))
                            .clickable { onFieldRemoved(field.id) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remover campo",
                            tint = ErrorRed,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }

            Text(
                text = field.signerName,
                color = Color(0xFF1A2130),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = field.signatureType.shortName,
                    color = accentColor,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = if (isEditable) {
                        "X:${(localXRatio * 100).roundToInt()}% Y:${(localYRatio * 100).roundToInt()}%"
                    } else if (isAlreadySigned) {
                        "ASSINADO"
                    } else if (isHighlightedForSigning) {
                        "TOQUE P/ ASSINAR"
                    } else {
                        "AGUARDANDO"
                    },
                    color = if (isHighlightedForSigning) TealAccent else Color(0xFF5A6577),
                    fontSize = 7.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Quadro de Canvas interativo para coleta de Rubrica / Assinatura Manuscrita na tela do celular.
 */
@Composable
fun HandwrittenSignatureCanvasPad(
    modifier: Modifier = Modifier,
    onSignatureChanged: (hasStrokes: Boolean, exportBitmap: () -> Bitmap?) -> Unit
) {
    val paths = remember { mutableStateListOf<List<Offset>>() }
    var currentPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var canvasSize by remember { mutableStateOf(IntSize(600, 240)) }

    fun createSignatureBitmap(): Bitmap? {
        val allStrokes = if (currentPoints.size > 1) paths + listOf(currentPoints) else paths.toList()
        if (allStrokes.isEmpty()) return null

        val w = canvasSize.width.coerceAtLeast(400)
        val h = canvasSize.height.coerceAtLeast(180)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val androidCanvas = AndroidCanvas(bmp)

        val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(15, 32, 67)
            style = AndroidPaint.Style.STROKE
            strokeWidth = 6.5f
            strokeCap = AndroidPaint.Cap.ROUND
            strokeJoin = AndroidPaint.Join.ROUND
        }

        for (stroke in allStrokes) {
            if (stroke.size < 2) continue
            val androidPath = AndroidPath()
            androidPath.moveTo(stroke.first().x, stroke.first().y)
            for (i in 1 until stroke.size) {
                androidPath.lineTo(stroke[i].x, stroke[i].y)
            }
            androidCanvas.drawPath(androidPath, paint)
        }
        return bmp
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Desenhe sua assinatura / rubrica abaixo:",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = NavyPrimary
            )
            TextButton(
                onClick = {
                    paths.clear()
                    currentPoints = emptyList()
                    onSignatureChanged(false) { null }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Limpar Quadro", fontSize = 12.sp)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(155.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFFAFBFD))
                .border(1.5.dp, Color(0xFFB8C4D9), RoundedCornerShape(10.dp))
                .onSizeChanged { canvasSize = it }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            currentPoints = listOf(startOffset)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val pos = change.position
                            if (pos.x in 0f..size.width.toFloat() && pos.y in 0f..size.height.toFloat()) {
                                currentPoints = currentPoints + pos
                                onSignatureChanged(true) { createSignatureBitmap() }
                            }
                        },
                        onDragEnd = {
                            if (currentPoints.size > 1) {
                                paths.add(currentPoints)
                            }
                            currentPoints = emptyList()
                            onSignatureChanged(paths.isNotEmpty()) { createSignatureBitmap() }
                        }
                    )
                }
        ) {
            // Linha guia de assinatura
            Canvas(modifier = Modifier.fillMaxSize()) {
                val guideY = size.height * 0.76f
                drawLine(
                    color = Color(0xFFD0D9E8),
                    start = Offset(24f, guideY),
                    end = Offset(size.width - 24f, guideY),
                    strokeWidth = 2f
                )

                val allStrokes = paths + listOf(currentPoints)
                for (stroke in allStrokes) {
                    if (stroke.size > 1) {
                        val composePath = Path().apply {
                            moveTo(stroke.first().x, stroke.first().y)
                            for (i in 1 until stroke.size) {
                                lineTo(stroke[i].x, stroke[i].y)
                            }
                        }
                        drawPath(
                            path = composePath,
                            color = NavyPrimary,
                            style = Stroke(
                                width = 6.5f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }
            }

            if (paths.isEmpty() && currentPoints.isEmpty()) {
                Text(
                    text = "Assine aqui com o dedo ou caneta touch",
                    fontSize = 12.sp,
                    color = Color(0xFF9AA5B8),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
