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
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import br.com.fiap.letssign.data.local.UserEntity
import br.com.fiap.letssign.domain.model.DocumentStatus
import br.com.fiap.letssign.domain.model.DraftSignatureField
import br.com.fiap.letssign.domain.model.FieldType
import br.com.fiap.letssign.domain.model.SignatureType
import br.com.fiap.letssign.domain.model.SignerClassification
import br.com.fiap.letssign.ui.theme.CardBorder
import br.com.fiap.letssign.ui.theme.CardWhite
import br.com.fiap.letssign.ui.theme.CharcoalText
import br.com.fiap.letssign.ui.theme.ErrorRed
import br.com.fiap.letssign.ui.theme.GoldChampagne
import br.com.fiap.letssign.ui.theme.GoldLight
import br.com.fiap.letssign.ui.theme.MutedText
import br.com.fiap.letssign.ui.theme.SuccessGreen
import br.com.fiap.letssign.ui.theme.WarningAmber
import br.com.fiap.letssign.ui.theme.WineDark
import br.com.fiap.letssign.ui.theme.WineLight
import br.com.fiap.letssign.ui.theme.WinePrimary
import br.com.fiap.letssign.util.PdfEngine
import br.com.fiap.letssign.util.PdfPageBitmap
import kotlin.math.roundToInt

/**
 * Logotipo oficial AssinaJÁ com o desenho da pena clássica de caligrafia
 * estilizada em tons de Vermelho Vinho Nobre e Ouro Champagne.
 */
@Composable
fun AssinaJaLogo(
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    showText: Boolean = true,
    isLightOnDark: Boolean = false
) {
    val featherWineColor = if (isLightOnDark) Color.White else WinePrimary
    val nibGoldColor = GoldChampagne

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height

            val featherPath = Path().apply {
                moveTo(w * 0.78f, h * 0.08f)
                cubicTo(
                    w * 0.95f, h * 0.28f,
                    w * 0.85f, h * 0.62f,
                    w * 0.38f, h * 0.76f
                )
                lineTo(w * 0.34f, h * 0.78f)
                cubicTo(
                    w * 0.48f, h * 0.58f,
                    w * 0.44f, h * 0.34f,
                    w * 0.78f, h * 0.08f
                )
                close()
            }
            drawPath(path = featherPath, color = featherWineColor.copy(alpha = 0.92f))

            val stemPath = Path().apply {
                moveTo(w * 0.78f, h * 0.08f)
                cubicTo(
                    w * 0.58f, h * 0.38f,
                    w * 0.42f, h * 0.65f,
                    w * 0.26f, h * 0.84f
                )
            }
            drawPath(
                path = stemPath,
                color = nibGoldColor,
                style = Stroke(width = w * 0.055f, cap = StrokeCap.Round)
            )

            val nibPath = Path().apply {
                moveTo(w * 0.28f, h * 0.81f)
                lineTo(w * 0.16f, h * 0.95f)
                lineTo(w * 0.22f, h * 0.87f)
                lineTo(w * 0.34f, h * 0.75f)
                close()
            }
            drawPath(path = nibPath, color = nibGoldColor)

            drawCircle(
                color = featherWineColor,
                radius = w * 0.045f,
                center = Offset(w * 0.14f, h * 0.96f)
            )
        }

        if (showText) {
            Spacer(modifier = Modifier.width(9.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Assina",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 21.sp,
                    color = if (isLightOnDark) Color.White else WinePrimary,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "JÁ",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Black,
                    fontSize = 21.sp,
                    color = GoldChampagne,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

/**
 * Card Executivo de Métrica para a Dashboard do AssinaJÁ.
 */
@Composable
fun AssinaJaMetricCard(
    title: String,
    count: Int,
    icon: ImageVector,
    accentColor: Color,
    bgColor: Color,
    subText: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, CardBorder),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = bgColor,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Surface(
                    color = bgColor,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(0.8.dp, accentColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "$count",
                        color = accentColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = CharcoalText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = subText,
                fontSize = 11.sp,
                color = MutedText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun DocumentStatusBadge(status: DocumentStatus) {
    val (bgColor, textColor) = when (status) {
        DocumentStatus.CONCLUIDO -> Color(0xFFE8F7EE) to SuccessGreen
        DocumentStatus.PARCIALMENTE_ASSINADO -> Color(0xFFFFF7E6) to WarningAmber
        DocumentStatus.ENVIADO -> WineLight to WinePrimary
        DocumentStatus.REJEITADO -> Color(0xFFFFEBEE) to ErrorRed
        DocumentStatus.RASCUNHO -> Color(0xFFF0EDE9) to Color(0xFF6B6661)
    }
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(50),
        border = BorderStroke(0.8.dp, textColor.copy(alpha = 0.35f))
    ) {
        Text(
            text = status.label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun SignerClassificationBadge(classification: SignerClassification) {
    val (bg, fg) = when (classification) {
        SignerClassification.CONTRATANTE -> WineLight to WinePrimary
        SignerClassification.CONTRATADO -> Color(0xFFEAF5EC) to SuccessGreen
        SignerClassification.TESTEMUNHA -> GoldLight to Color(0xFF7A5816)
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(0.6.dp, fg.copy(alpha = 0.25f))
    ) {
        Text(
            text = classification.label.uppercase(),
            color = fg,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun SignatureTypeBadge(signatureType: SignatureType) {
    val (bg, fg) = when (signatureType) {
        SignatureType.QUALIFICADA_ICP -> WineLight to WinePrimary
        SignatureType.AVANCADA -> GoldLight to Color(0xFF7A5816)
        SignatureType.SIMPLES -> Color(0xFFF1F0ED) to Color(0xFF4A4E57)
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(0.7.dp, fg.copy(alpha = 0.35f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Icon(
                imageVector = Icons.Default.VerifiedUser,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = signatureType.shortName,
                color = fg,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Modal de Cadastro e Edição de Signatário Corporativo pelo Admin.
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                AssinaJaLogo(size = 24.dp, showText = false)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (initialSigner == null) "Cadastrar Signatário" else "Editar Signatário",
                    fontWeight = FontWeight.Bold,
                    color = WinePrimary
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "O e-mail e a senha cadastrados serão as credenciais de autenticação que o signatário usará para assinar pelo aplicativo.",
                    fontSize = 12.sp,
                    color = MutedText
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
                    placeholder = { Text("Ex: John McClane") },
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
                    placeholder = { Text("Ex: john.mcclane@assinaja.com.br") },
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
                    placeholder = { Text("Ex: (11) 98765-4321") },
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
                    label = { Text("Senha de autenticação *") },
                    placeholder = { Text("Mínimo 4 caracteres") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Alternar visibilidade",
                                tint = WinePrimary
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = legalDocument,
                    onValueChange = { legalDocument = it },
                    label = { Text("CPF, CNPJ ou Documento (Opcional)") },
                    placeholder = { Text("Ex: 123.456.789-00") },
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
                },
                colors = ButtonDefaults.buttonColors(containerColor = WinePrimary)
            ) {
                Text(if (initialSigner == null) "Cadastrar" else "Salvar Alterações")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = WinePrimary)
            }
        }
    )
}

/**
 * Visualizador em Tela Cheia com Zoom Interativo (Pinch-to-zoom e Pan)
 * para leitura clara de cada cláusula do documento antes da assinatura.
 */
@Composable
fun FullscreenPdfViewerDialog(
    pdfFilePath: String,
    initialPageIndex: Int = 0,
    totalPages: Int,
    documentTitle: String,
    onDismiss: () -> Unit
) {
    var currentPage by remember { mutableIntStateOf(initialPageIndex.coerceIn(0, (totalPages - 1).coerceAtLeast(0))) }
    var pageRender by remember(pdfFilePath, currentPage) { mutableStateOf<PdfPageBitmap?>(null) }
    var isLoading by remember(pdfFilePath, currentPage) { mutableStateOf(true) }

    var scale by remember(currentPage) { mutableFloatStateOf(1f) }
    var offset by remember(currentPage) { mutableStateOf(Offset.Zero) }

    LaunchedEffect(pdfFilePath, currentPage) {
        isLoading = true
        pageRender = PdfEngine.renderPdfPage(pdfFilePath, currentPage, targetWidthPx = 1800)
        isLoading = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = Color(0xFF16171A),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Barra de Ferramentas Superior
                Surface(
                    color = WineDark,
                    shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = documentTitle,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Pág. ${currentPage + 1} de $totalPages • Zoom: ${(scale * 100).toInt()}%",
                                    fontSize = 11.sp,
                                    color = GoldChampagne
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    scale = (scale - 0.25f).coerceAtLeast(1f)
                                    if (scale == 1f) offset = Offset.Zero
                                }
                            ) {
                                Icon(Icons.Default.ZoomOut, contentDescription = "Menos Zoom", tint = Color.White)
                            }
                            IconButton(
                                onClick = {
                                    scale = 1f
                                    offset = Offset.Zero
                                }
                            ) {
                                Icon(Icons.Default.RestartAlt, contentDescription = "Resetar Zoom", tint = GoldChampagne)
                            }
                            IconButton(
                                onClick = {
                                    scale = (scale + 0.35f).coerceAtMost(4f)
                                }
                            ) {
                                Icon(Icons.Default.ZoomIn, contentDescription = "Mais Zoom", tint = Color.White)
                            }
                        }
                    }
                }

                // Área do PDF com Gestos de Pinça e Pan
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .pointerInput(currentPage) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 4.5f)
                                if (scale > 1f) {
                                    offset += pan
                                } else {
                                    offset = Offset.Zero
                                }
                            }
                        }
                ) {
                    if (isLoading) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = GoldChampagne)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Carregando página em alta resolução...", color = Color.White, fontSize = 12.sp)
                        }
                    } else if (pageRender != null) {
                        Image(
                            bitmap = pageRender!!.bitmap.asImageBitmap(),
                            contentDescription = "Página ${currentPage + 1}",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x,
                                    translationY = offset.y
                                )
                        )
                    } else {
                        Text("Não foi possível carregar o documento em tela cheia.", color = ErrorRed)
                    }
                }

                // Barra de Navegação de Páginas Inferior
                Surface(
                    color = Color(0xFF202227),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Button(
                            enabled = currentPage > 0,
                            onClick = {
                                currentPage--
                                scale = 1f
                                offset = Offset.Zero
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WinePrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pág. Anterior", fontSize = 12.sp)
                        }

                        Text(
                            text = "${currentPage + 1} / $totalPages",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 13.sp
                        )

                        Button(
                            enabled = currentPage < totalPages - 1,
                            onClick = {
                                currentPage++
                                scale = 1f
                                offset = Offset.Zero
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WinePrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Próxima Pág.", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Visualizador de Página do PDF com suporte a Posicionamento Interativo (Drag & Drop)
 * de Rubricas, CPFs e Datas.
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
    onFieldResized: (fieldId: String, newWidthRatio: Float, newHeightRatio: Float) -> Unit = { _, _, _ -> },
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
            595f / 842f
        }
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CardBorder),
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
                    CircularProgressIndicator(color = WinePrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Renderizando página ${pageIndex + 1}...",
                        fontSize = 12.sp,
                        color = MutedText
                    )
                }
            } else if (pageRender != null) {
                Image(
                    bitmap = pageRender!!.bitmap.asImageBitmap(),
                    contentDescription = "Página ${pageIndex + 1} do Documento",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )

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
                            onFieldResized = onFieldResized,
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
                        text = "Não foi possível carregar o preview da página.",
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
    onFieldResized: (fieldId: String, newWidthRatio: Float, newHeightRatio: Float) -> Unit,
    onFieldRemoved: (fieldId: String) -> Unit,
    onFieldClicked: (DraftSignatureField) -> Unit
) {
    var localXRatio by remember(field.id, field.xRatio) { mutableFloatStateOf(field.xRatio) }
    var localYRatio by remember(field.id, field.yRatio) { mutableFloatStateOf(field.yRatio) }
    var localWidthRatio by remember(field.id, field.widthRatio) { mutableFloatStateOf(field.widthRatio) }
    var localHeightRatio by remember(field.id, field.heightRatio) { mutableFloatStateOf(field.heightRatio) }

    val currentOnMoved by rememberUpdatedState(onFieldMoved)
    val currentOnResized by rememberUpdatedState(onFieldResized)
    val maxXRatio = (1f - localWidthRatio).coerceAtLeast(0f)
    val maxYRatio = (1f - localHeightRatio).coerceAtLeast(0f)

    val widthDp = with(density) { (localWidthRatio * containerWidthPx).toDp() }
    val heightDp = with(density) { (localHeightRatio * containerHeightPx).toDp() }
    val offsetX = (localXRatio * containerWidthPx).roundToInt()
    val offsetY = (localYRatio * containerHeightPx).roundToInt()

    val fieldColor = when (field.fieldType) {
        FieldType.ASSINATURA -> WinePrimary
        FieldType.RUBRICA -> WineDark
        FieldType.CPF -> Color(0xFF006699)
        FieldType.DATA -> Color(0xFF7A5816)
    }

    val fieldIcon = when (field.fieldType) {
        FieldType.ASSINATURA -> if (isAlreadySigned) Icons.Default.CheckCircle else Icons.Default.Draw
        FieldType.RUBRICA -> if (isAlreadySigned) Icons.Default.CheckCircle else Icons.Default.Draw
        FieldType.CPF -> Icons.Default.Badge
        FieldType.DATA -> Icons.Default.CalendarToday
    }

    val accentColor = if (isAlreadySigned) SuccessGreen else fieldColor
    val bgColor = if (isAlreadySigned) Color(0xFFE8F8EE).copy(alpha = 0.94f) else WineLight.copy(alpha = 0.95f)

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX, offsetY) }
            .size(width = widthDp, height = heightDp)
            .shadow(if (isEditable || isHighlightedForSigning) 4.dp else 1.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(
                width = if (isHighlightedForSigning) 2.dp else 1.4.dp,
                color = if (isHighlightedForSigning) GoldChampagne else accentColor,
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
                        imageVector = fieldIcon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${field.fieldType.shortLabel.uppercase()} • ${field.classification.label.uppercase()}",
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
                color = CharcoalText,
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
                    text = field.fieldType.label,
                    color = accentColor,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (isEditable) {
                    // Pega de redimensionamento elegante no canto inferior direito
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(GoldChampagne.copy(alpha = 0.35f))
                            .pointerInput(field.id, containerWidthPx, containerHeightPx) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val deltaW = dragAmount.x / containerWidthPx
                                    val deltaH = dragAmount.y / containerHeightPx
                                    localWidthRatio = (localWidthRatio + deltaW).coerceIn(0.18f, (1f - localXRatio).coerceAtLeast(0.18f))
                                    localHeightRatio = (localHeightRatio + deltaH).coerceIn(0.05f, (1f - localYRatio).coerceAtLeast(0.05f))
                                    currentOnResized(field.id, localWidthRatio, localHeightRatio)
                                }
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenWith,
                            contentDescription = "Arrastar para redimensionar",
                            tint = WinePrimary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                } else {
                    Text(
                        text = if (isAlreadySigned) {
                            "PREENCHIDO"
                        } else if (isHighlightedForSigning) {
                            "TOQUE P/ ASSINAR"
                        } else {
                            "AGUARDANDO"
                        },
                        color = if (isHighlightedForSigning) WinePrimary else MutedText,
                        fontSize = 7.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Quadro de Canvas para coleta de Rubrica com botão Limpar alinhado e sem quebras verticais.
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
            color = AndroidColor.rgb(107, 23, 36)
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
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Desenhe sua rubrica:",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = WinePrimary,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = {
                    paths.clear()
                    currentPoints = emptyList()
                    onSignatureChanged(false) { null }
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.wrapContentWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = WinePrimary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Limpar",
                    fontSize = 11.5.sp,
                    color = WinePrimary,
                    softWrap = false
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFFCFAF9))
                .border(1.5.dp, CardBorder, RoundedCornerShape(10.dp))
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
            Canvas(modifier = Modifier.fillMaxSize()) {
                val guideY = size.height * 0.76f
                drawLine(
                    color = Color(0xFFE2DCD8),
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
                            color = WinePrimary,
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
                    text = "Rubrique com o dedo ou caneta stylus",
                    fontSize = 12.sp,
                    color = MutedText,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
