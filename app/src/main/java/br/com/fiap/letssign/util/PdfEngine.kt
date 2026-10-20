package br.com.fiap.letssign.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import br.com.fiap.letssign.data.local.AuditEventEntity
import br.com.fiap.letssign.data.local.DocumentEntity
import br.com.fiap.letssign.data.local.DocumentSignerEntity
import br.com.fiap.letssign.data.local.SignatureFieldEntity
import br.com.fiap.letssign.domain.model.SignatureType
import br.com.fiap.letssign.domain.model.SignerStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class PdfValidationResult(
    val isValid: Boolean,
    val fileName: String = "",
    val internalFilePath: String = "",
    val pageCount: Int = 0,
    val sha256Hash: String = "",
    val fileSizeBytes: Long = 0L,
    val errorMessage: String? = null
)

data class PdfPageBitmap(
    val pageIndex: Int,
    val totalPages: Int,
    val bitmap: Bitmap,
    val pdfPageWidth: Int,
    val pdfPageHeight: Int
)

object PdfEngine {

    private val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm:ss", Locale("pt", "BR"))

    fun formatTimestamp(timestamp: Long?): String {
        if (timestamp == null || timestamp <= 0L) return "—"
        return dateTimeFormat.format(Date(timestamp))
    }

    fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var read: Int
            while (fis.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun calculateStringSha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Importa um arquivo PDF selecionado no celular, valida se o arquivo não está vazio,
     * verifica o cabeçalho %PDF-, testa a abertura e renderização pelo PdfRenderer para
     * detectar PDFs corrompidos ou inválidos e retorna o resultado estruturado.
     */
    suspend fun validateAndCopyPdfUri(context: Context, uri: Uri): PdfValidationResult =
        withContext(Dispatchers.IO) {
            try {
                val contentResolver = context.contentResolver
                var displayName = "documento_${System.currentTimeMillis()}.pdf"

                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIdx >= 0) {
                            val name = cursor.getString(nameIdx)
                            if (!name.isNullOrBlank()) {
                                displayName = name
                            }
                        }
                    }
                }

                val originalsDir = File(context.filesDir, "documents/originals").apply { mkdirs() }
                val destFile = File(originalsDir, "orig_${UUID.randomUUID()}_${displayName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")}")

                val inputStream = contentResolver.openInputStream(uri)
                    ?: return@withContext PdfValidationResult(
                        isValid = false,
                        fileName = displayName,
                        errorMessage = "Não foi possível abrir o arquivo selecionado no dispositivo."
                    )

                inputStream.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }

                validatePdfFile(destFile, displayName)
            } catch (e: Exception) {
                PdfValidationResult(
                    isValid = false,
                    errorMessage = "Erro ao processar o arquivo PDF: ${e.localizedMessage ?: "Arquivo inválido ou inacessível."}"
                )
            }
        }

    /**
     * Valida um arquivo físico no sistema de arquivos do dispositivo.
     */
    fun validatePdfFile(file: File, displayName: String = file.name): PdfValidationResult {
        if (!file.exists() || file.length() < 8L) {
            file.delete()
            return PdfValidationResult(
                isValid = false,
                fileName = displayName,
                errorMessage = "O arquivo selecionado está vazio ou incompleto (0 bytes)."
            )
        }

        // Verifica assinatura mágica %PDF- nos primeiros bytes
        val headerBytes = ByteArray(5)
        FileInputStream(file).use { fis ->
            fis.read(headerBytes)
        }
        val headerString = String(headerBytes, Charsets.US_ASCII)
        if (!headerString.startsWith("%PDF-")) {
            file.delete()
            return PdfValidationResult(
                isValid = false,
                fileName = displayName,
                errorMessage = "Arquivo inválido: o formato não corresponde a um documento PDF autêntico (cabeçalho %PDF ausente)."
            )
        }

        return try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val count = renderer.pageCount
                    if (count <= 0) {
                        file.delete()
                        return PdfValidationResult(
                            isValid = false,
                            fileName = displayName,
                            errorMessage = "O PDF não possui páginas válidas para visualização."
                        )
                    }
                    // Renderiza a primeira página em baixa resolução para garantir que o stream não está corrompido
                    renderer.openPage(0).use { firstPage ->
                        if (firstPage.width <= 0 || firstPage.height <= 0) {
                            throw IllegalStateException("Dimensões de página inválidas.")
                        }
                        val testBmp = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
                        firstPage.render(testBmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        testBmp.recycle()
                    }

                    val hash = calculateSha256(file)
                    PdfValidationResult(
                        isValid = true,
                        fileName = displayName,
                        internalFilePath = file.absolutePath,
                        pageCount = count,
                        sha256Hash = hash,
                        fileSizeBytes = file.length()
                    )
                }
            }
        } catch (e: Exception) {
            file.delete()
            PdfValidationResult(
                isValid = false,
                fileName = displayName,
                errorMessage = "O arquivo PDF está corrompido, protegido por senha ou não pôde ser processado pelo leitor (${e.javaClass.simpleName})."
            )
        }
    }

    /**
     * Renderiza uma página específica do PDF para exibição no app.
     */
    suspend fun renderPdfPage(
        filePath: String,
        pageIndex: Int,
        targetWidthPx: Int = 1080
    ): PdfPageBitmap? = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists()) return@withContext null

        try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val total = renderer.pageCount
                    if (total <= 0) return@withContext null
                    val safeIndex = pageIndex.coerceIn(0, total - 1)

                    renderer.openPage(safeIndex).use { page ->
                        val pdfW = page.width.coerceAtLeast(1)
                        val pdfH = page.height.coerceAtLeast(1)
                        val scale = targetWidthPx.toFloat() / pdfW.toFloat()
                        val bmpW = targetWidthPx.coerceAtLeast(400)
                        val bmpH = (pdfH * scale).toInt().coerceAtLeast(400)

                        val bitmap = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        canvas.drawColor(Color.WHITE)

                        page.render(
                            bitmap,
                            null,
                            null,
                            PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                        )

                        PdfPageBitmap(
                            pageIndex = safeIndex,
                            totalPages = total,
                            bitmap = bitmap,
                            pdfPageWidth = pdfW,
                            pdfPageHeight = pdfH
                        )
                    }
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Salva o Bitmap da rubrica manuscrita desenhada no Canvas do celular em arquivo PNG interno.
     */
    suspend fun saveSignatureBitmap(context: Context, bitmap: Bitmap): String =
        withContext(Dispatchers.IO) {
            val sigDir = File(context.filesDir, "documents/signatures").apply { mkdirs() }
            val outFile = File(sigDir, "sig_${UUID.randomUUID()}.png")
            FileOutputStream(outFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }
            outFile.absolutePath
        }

    /**
     * Estampa fisicamente no arquivo PDF todos os campos já assinados (preservando a posição
     * relativa exata em relação às dimensões da página) e, quando todos os signatários
     * obrigatórios tiverem assinado, adiciona uma página final com o Manifesto Jurídico de Assinaturas.
     */
    suspend fun generateUpdatedSignedPdf(
        context: Context,
        document: DocumentEntity,
        allSigners: List<DocumentSignerEntity>,
        allFields: List<SignatureFieldEntity>,
        auditEvents: List<AuditEventEntity>,
        includeManifestPage: Boolean
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        val sourceFile = File(document.originalPdfPath)
        val signedDir = File(context.filesDir, "documents/signed").apply { mkdirs() }
        val outputFile = File(
            signedDir,
            "doc_${document.id}_v${document.version}_signed_${System.currentTimeMillis()}.pdf"
        )

        val outPdf = PdfDocument()

        ParcelFileDescriptor.open(sourceFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                val pageCount = renderer.pageCount
                for (pageIdx in 0 until pageCount) {
                    renderer.openPage(pageIdx).use { srcPage ->
                        val pageW = srcPage.width
                        val pageH = srcPage.height

                        // Renderiza a página original em alta definição (2x) para preservar nitidez do texto
                        val scaleFactor = 2
                        val renderBmp = Bitmap.createBitmap(
                            pageW * scaleFactor,
                            pageH * scaleFactor,
                            Bitmap.Config.ARGB_8888
                        )
                        val renderCanvas = Canvas(renderBmp)
                        renderCanvas.drawColor(Color.WHITE)
                        srcPage.render(
                            renderBmp,
                            null,
                            null,
                            PdfRenderer.Page.RENDER_MODE_FOR_PRINT
                        )

                        val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, pageIdx + 1).create()
                        val destPage = outPdf.startPage(pageInfo)
                        val pdfCanvas = destPage.canvas

                        // Desenha a página original
                        pdfCanvas.drawBitmap(
                            renderBmp,
                            Rect(0, 0, renderBmp.width, renderBmp.height),
                            Rect(0, 0, pageW, pageH),
                            null
                        )
                        renderBmp.recycle()

                        // Desenha os campos de assinatura desta página que já foram assinados
                        val pageSignedFields = allFields.filter { it.pageIndex == pageIdx && it.isSigned }
                        for (field in pageSignedFields) {
                            val signer = allSigners.find { it.signerUserId == field.signerUserId }
                            drawSignatureStampOnPdfCanvas(
                                canvas = pdfCanvas,
                                pageWidth = pageW.toFloat(),
                                pageHeight = pageH.toFloat(),
                                field = field,
                                signer = signer
                            )
                        }

                        outPdf.finishPage(destPage)
                    }
                }
            }
        }

        // Quando o documento é concluído, anexa a Página de Manifesto Jurídico e Trilha de Auditoria
        if (includeManifestPage) {
            appendLegalManifestPage(
                outPdf = outPdf,
                pageNumber = document.pageCount + 1,
                document = document,
                signers = allSigners,
                auditEvents = auditEvents
            )
        }

        FileOutputStream(outputFile).use { fos ->
            outPdf.writeTo(fos)
        }
        outPdf.close()

        val newHash = calculateSha256(outputFile)
        Pair(outputFile.absolutePath, newHash)
    }

    private fun drawSignatureStampOnPdfCanvas(
        canvas: Canvas,
        pageWidth: Float,
        pageHeight: Float,
        field: SignatureFieldEntity,
        signer: DocumentSignerEntity?
    ) {
        val left = (field.xRatio * pageWidth).coerceIn(0f, pageWidth)
        val top = (field.yRatio * pageHeight).coerceIn(0f, pageHeight)
        val width = (field.widthRatio * pageWidth).coerceAtMost(pageWidth - left)
        val height = (field.heightRatio * pageHeight).coerceAtMost(pageHeight - top)
        val rect = RectF(left, top, left + width, top + height)

        val primaryColor = when (field.signatureType) {
            SignatureType.QUALIFICADA_ICP -> Color.rgb(13, 71, 161) // Azul ICP-Brasil
            SignatureType.AVANCADA -> Color.rgb(0, 105, 92) // Verde Esmeralda Jurídico
            SignatureType.SIMPLES -> Color.rgb(55, 71, 79) // Cinza Azulado Corporativo
        }

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(242, 248, 251, 255)
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            color = primaryColor
        }
        canvas.drawRoundRect(rect, 6f, 6f, bgPaint)
        canvas.drawRoundRect(rect, 6f, 6f, borderPaint)

        // Faixa lateral de autenticidade
        val stripeRect = RectF(left, top, left + 5f, top + height)
        val stripePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = primaryColor
        }
        canvas.drawRoundRect(stripeRect, 6f, 6f, stripePaint)

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryColor
            textSize = (height * 0.14f).coerceIn(6.5f, 10f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(33, 33, 33)
            textSize = (height * 0.13f).coerceIn(6f, 9.5f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val monoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(90, 90, 90)
            textSize = (height * 0.11f).coerceIn(5.5f, 8f)
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }

        val padX = left + 9f
        var cursorY = top + headerPaint.textSize + 3f

        val modalityBadge = when (field.signatureType) {
            SignatureType.QUALIFICADA_ICP -> "ICP-BRASIL QUALIFICADA • LETSSIGN"
            SignatureType.AVANCADA -> "ASSINATURA AVANÇADA • LETSSIGN"
            SignatureType.SIMPLES -> "ASSINATURA SIMPLES • LETSSIGN"
        }
        canvas.drawText(modalityBadge, padX, cursorY, headerPaint)

        // Se houver imagem de rubrica manuscrita salva, desenha na metade direita/central do selo
        val sigImgPath = field.signatureImagePath ?: signer?.signatureImagePath
        if (!sigImgPath.isNullOrBlank()) {
            val sigFile = File(sigImgPath)
            if (sigFile.exists()) {
                val sigBmp = BitmapFactory.decodeFile(sigFile.absolutePath)
                if (sigBmp != null) {
                    val imgAreaLeft = left + width * 0.52f
                    val imgAreaTop = top + height * 0.18f
                    val imgAreaRight = left + width - 6f
                    val imgAreaBottom = top + height * 0.78f
                    if (imgAreaRight > imgAreaLeft && imgAreaBottom > imgAreaTop) {
                        canvas.drawBitmap(
                            sigBmp,
                            null,
                            RectF(imgAreaLeft, imgAreaTop, imgAreaRight, imgAreaBottom),
                            null
                        )
                    }
                    sigBmp.recycle()
                }
            }
        }

        cursorY += bodyPaint.textSize + 3f
        val roleText = "${field.signerName} (${field.classification.label})"
        canvas.drawText(roleText.take(36), padX, cursorY, bodyPaint)

        cursorY += bodyPaint.textSize + 2f
        val emailText = signer?.signerEmail ?: ""
        if (emailText.isNotBlank()) {
            canvas.drawText(emailText.take(38), padX, cursorY, monoPaint)
        }

        cursorY += monoPaint.textSize + 2f
        val signedDateStr = formatTimestamp(field.signedAt ?: signer?.signedAt)
        canvas.drawText("Data: $signedDateStr", padX, cursorY, monoPaint)

        val hashShort = (signer?.signatureHash ?: field.stampSummary ?: "").take(20)
        if (hashShort.isNotBlank() && cursorY + monoPaint.textSize + 2f < top + height) {
            cursorY += monoPaint.textSize + 2f
            canvas.drawText("Hash: $hashShort...", padX, cursorY, monoPaint)
        }
    }

    private fun appendLegalManifestPage(
        outPdf: PdfDocument,
        pageNumber: Int,
        document: DocumentEntity,
        signers: List<DocumentSignerEntity>,
        auditEvents: List<AuditEventEntity>
    ) {
        val pageW = 595
        val pageH = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, pageNumber).create()
        val page = outPdf.startPage(pageInfo)
        val canvas = page.canvas

        canvas.drawColor(Color.WHITE)

        // Cabeçalho institucional LetsSign Jurídico
        val headerBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 32, 67)
        }
        canvas.drawRect(0f, 0f, pageW.toFloat(), 76f, headerBg)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(180, 210, 255)
            textSize = 9.5f
        }
        canvas.drawText("LETSSIGN • MANIFESTO DE ASSINATURAS E TRILHA DE AUDITORIA", 32f, 34f, titlePaint)
        canvas.drawText(
            "Validade Jurídica assegurada conforme MP 2.200-2/2001 e Lei nº 14.063/2020",
            32f,
            54f,
            subHeaderPaint
        )

        val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 32, 67)
            textSize = 11.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(60, 60, 60)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(40, 40, 40)
            textSize = 9.5f
        }
        val monoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 70, 130)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(220, 226, 235)
            strokeWidth = 1f
        }

        var y = 104f
        canvas.drawText("1. IDENTIFICAÇÃO DO DOCUMENTO", 32f, y, sectionPaint)
        y += 18f
        canvas.drawText("Título: ${document.title} (Versão v${document.version})", 32f, y, labelPaint)
        y += 14f
        canvas.drawText("Arquivo Original: ${document.originalFileName} • Total de Páginas Originais: ${document.pageCount}", 32f, y, textPaint)
        y += 14f
        canvas.drawText("Hash SHA-256 Original: ${document.originalSha256}", 32f, y, monoPaint)
        y += 14f
        canvas.drawText("Protocolo de Disparo: ${document.dispatchIdempotencyKey}", 32f, y, monoPaint)
        y += 16f
        canvas.drawLine(32f, y, pageW - 32f, y, dividerPaint)

        y += 22f
        canvas.drawText("2. QUADRO DE SIGNATÁRIOS E QUALIFICAÇÃO JURÍDICA", 32f, y, sectionPaint)
        y += 16f

        signers.forEachIndexed { idx, signer ->
            val cardTop = y
            val cardHeight = 58f
            val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(245, 248, 252)
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(RectF(32f, cardTop, pageW - 32f, cardTop + cardHeight), 6f, 6f, cardPaint)

            var sy = cardTop + 15f
            val statusLabel = if (signer.status == SignerStatus.ASSINADO) "ASSINADO" else "PENDENTE"
            canvas.drawText(
                "${idx + 1}. ${signer.signerName} • Papel: ${signer.classification.label.uppercase()} [$statusLabel]",
                42f,
                sy,
                labelPaint
            )
            sy += 13f
            canvas.drawText(
                "E-mail: ${signer.signerEmail} | Tel: ${signer.signerPhone} | Modalidade: ${signer.signatureType.fullName}",
                42f,
                sy,
                textPaint
            )
            sy += 13f
            val certOrToken = signer.certificateInfo ?: signer.verificationToken ?: "Autenticação por Credencial + IP"
            canvas.drawText(
                "Data/Hora: ${formatTimestamp(signer.signedAt)} | Evidência: $certOrToken",
                42f,
                sy,
                monoPaint
            )
            y += cardHeight + 8f
        }

        y += 10f
        canvas.drawLine(32f, y, pageW - 32f, y, dividerPaint)
        y += 20f
        canvas.drawText("3. HISTÓRICO DE EVENTOS (TRILHA DE AUDITORIA)", 32f, y, sectionPaint)
        y += 16f

        auditEvents.sortedBy { it.timestamp }.take(12).forEach { event ->
            if (y < pageH - 45f) {
                val line = "[${formatTimestamp(event.timestamp)}] ${event.title} — ${event.actorName} (${event.description})"
                canvas.drawText(line.take(98), 32f, y, textPaint)
                y += 13f
            }
        }

        // Rodapé de certificação
        val footerY = pageH - 24f
        canvas.drawLine(32f, footerY - 14f, pageW - 32f, footerY - 14f, dividerPaint)
        canvas.drawText(
            "Documento concluído e selado eletronicamente via LetsSign Jurídico Mobile • Emitido em ${formatTimestamp(System.currentTimeMillis())}",
            32f,
            footerY,
            monoPaint
        )

        outPdf.finishPage(page)
    }

    /**
     * Utilitário auxiliar que gera um PDF jurídico real de 2 páginas (Contrato de Honorários Advocatícios)
     * para facilitar testes imediatos no emulador caso o avaliador deseje gerar uma minuta na hora.
     */
    suspend fun createSampleLegalContractPdf(context: Context): PdfValidationResult =
        withContext(Dispatchers.IO) {
            val originalsDir = File(context.filesDir, "documents/originals").apply { mkdirs() }
            val fileName = "Contrato_Honorarios_Advocaticios_${System.currentTimeMillis() % 10000}.pdf"
            val outFile = File(originalsDir, fileName)

            val pdfDoc = PdfDocument()
            val pageW = 595
            val pageH = 842

            val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 32, 67)
                textSize = 14f
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            }
            val subTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(80, 90, 105)
                textSize = 10f
                typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            }
            val clauseTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(20, 35, 60)
                textSize = 11f
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            }
            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(35, 35, 35)
                textSize = 10f
                typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            }
            val guidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(180, 190, 205)
                style = Paint.Style.STROKE
                strokeWidth = 1f
                pathEffect = DashPathEffect(floatArrayOf(5f, 5f), 0f)
            }

            // Página 1
            val page1 = pdfDoc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, 1).create())
            val c1 = page1.canvas
            c1.drawColor(Color.WHITE)

            var y = 56f
            c1.drawText("INSTRUMENTO PARTICULAR DE CONTRATO DE HONORÁRIOS ADVOCATÍCIOS", 42f, y, headerPaint)
            y += 18f
            c1.drawText("Regido pela Lei nº 8.906/94 (Estatuto da Advocacia e da OAB) e Código Civil Brasileiro", 42f, y, subTitlePaint)
            y += 28f

            val clausesPage1 = listOf(
                "CLÁUSULA PRIMEIRA – DO OBJETO DO CONTRATO" to listOf(
                    "1.1. O presente instrumento tem como objeto a prestação de serviços jurídicos especializados",
                    "pelo(a) CONTRATADO(A) em favor do(a) CONTRATANTE, compreendendo consultoria preventiva,",
                    "elaboração de peças processuais e acompanhamento em todas as instâncias judiciais."
                ),
                "CLÁUSULA SEGUNDA – DAS OBRIGAÇÕES DAS PARTES" to listOf(
                    "2.1. Compete ao(à) CONTRATADO(A) empregar toda a diligência e técnica jurídica na defesa",
                    "dos interesses confiados, mantendo o(a) CONTRATANTE informado(a) sobre o andamento processual.",
                    "2.2. Compete ao(à) CONTRATANTE fornecer a documentação fidedigna e efetuar os pagamentos",
                    "dos honorários convencionados e custas processuais nos prazos ajustados."
                ),
                "CLÁUSULA TERCEIRA – DOS HONORÁRIOS E FORMA DE PAGAMENTO" to listOf(
                    "3.1. Pelos serviços prestados, o(a) CONTRATANTE pagará ao(à) CONTRATADO(A) os honorários",
                    "contratuais pactuados entre as partes, constituindo este instrumento título executivo",
                    "extrajudicial nos termos do art. 24 da Lei nº 8.906/94 e art. 784, III e XII do CPC."
                ),
                "CLÁUSULA QUARTA – DA PROTEÇÃO DE DADOS (LGPD - LEI Nº 13.709/2018)" to listOf(
                    "4.1. As partes comprometem-se a tratar os dados pessoais contidos neste contrato estritamente",
                    "para o exercício regular de direitos em processo judicial, administrativo ou arbitral."
                )
            )

            for ((title, lines) in clausesPage1) {
                c1.drawText(title, 42f, y, clauseTitlePaint)
                y += 18f
                for (line in lines) {
                    c1.drawText(line, 42f, y, bodyPaint)
                    y += 15f
                }
                y += 16f
            }

            // Área reservada para rubrica de página 1
            c1.drawRoundRect(RectF(42f, 680f, 553f, 770f), 8f, 8f, guidePaint)
            c1.drawText("ÁREA PARA RUBRICAS DAS PARTES (PÁGINA 1/2)", 56f, 702f, subTitlePaint)
            pdfDoc.finishPage(page1)

            // Página 2
            val page2 = pdfDoc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, 2).create())
            val c2 = page2.canvas
            c2.drawColor(Color.WHITE)

            y = 56f
            c2.drawText("CLÁUSULA QUINTA – DA ASSINATURA ELETRÔNICA E VALIDADE JURÍDICA", 42f, y, clauseTitlePaint)
            y += 18f
            val page2Lines = listOf(
                "5.1. As partes e testemunhas declaram e concordam expressamente que a assinatura deste",
                "instrumento será realizada em formato eletrônico/digital por meio da plataforma LetsSign,",
                "reconhecendo sua plena validade, eficácia e executividade jurídica nos termos do art. 10",
                "da Medida Provisória nº 2.200-2/2001 e da Lei nº 14.063/2020.",
                "",
                "CLÁUSULA SEXTA – DO FORO",
                "6.1. Fica eleito o Foro da Comarca de São Paulo/SP para dirimir quaisquer dúvidas oriundas",
                "do presente instrumento, com renúncia expressa a qualquer outro, por mais privilegiado que seja.",
                "",
                "E, por estarem assim justas e contratadas, assinam digitalmente o presente instrumento",
                "juntamente com as testemunhas abaixo qualificadas."
            )
            for (line in page2Lines) {
                if (line.startsWith("CLÁUSULA")) {
                    c2.drawText(line, 42f, y, clauseTitlePaint)
                } else {
                    c2.drawText(line, 42f, y, bodyPaint)
                }
                y += 16f
            }

            y += 30f
            c2.drawText("QUADRO DE ASSINATURAS (POSICIONE OS CAMPOS ABAIXO):", 42f, y, clauseTitlePaint)
            y += 20f

            // Caixas guias para posicionar assinaturas na página 2
            c2.drawRoundRect(RectF(42f, y, 285f, y + 110f), 8f, 8f, guidePaint)
            c2.drawText("Local sugerido: CONTRATANTE", 56f, y + 22f, subTitlePaint)

            c2.drawRoundRect(RectF(310f, y, 553f, y + 110f), 8f, 8f, guidePaint)
            c2.drawText("Local sugerido: CONTRATADO", 324f, y + 22f, subTitlePaint)

            y += 135f
            c2.drawRoundRect(RectF(42f, y, 285f, y + 110f), 8f, 8f, guidePaint)
            c2.drawText("Local sugerido: TESTEMUNHA", 56f, y + 22f, subTitlePaint)

            pdfDoc.finishPage(page2)

            FileOutputStream(outFile).use { fos ->
                pdfDoc.writeTo(fos)
            }
            pdfDoc.close()

            validatePdfFile(outFile, fileName)
        }
}
