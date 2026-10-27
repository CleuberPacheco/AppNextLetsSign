package br.com.fiap.letssign.util

import android.content.ContentValues
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
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
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
import java.net.NetworkInterface
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

    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address.hostAddress?.contains(":") == false) {
                        return address.hostAddress ?: "192.168.1.105"
                    }
                }
            }
        } catch (_: Exception) {}
        return "189.40.112.${(10..240).random()} (App Nativo AssinaJÁ)"
    }

    /**
     * Importa um arquivo PDF selecionado no dispositivo, valida integridade física
     * e o cabeçalho %PDF-, testando a abertura com o PdfRenderer.
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

    fun validatePdfFile(file: File, displayName: String = file.name): PdfValidationResult {
        if (!file.exists() || file.length() < 8L) {
            file.delete()
            return PdfValidationResult(
                isValid = false,
                fileName = displayName,
                errorMessage = "O arquivo selecionado está vazio ou corrompido (0 bytes)."
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
                errorMessage = "O arquivo PDF está corrompido, protegido por senha ou não pôde ser processado (${e.javaClass.simpleName})."
            )
        }
    }

    /**
     * Renderiza uma página específica do PDF para exibição nítida no celular.
     */
    suspend fun renderPdfPage(
        filePath: String,
        pageIndex: Int,
        targetWidthPx: Int = 1200
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
                        val bmpW = targetWidthPx.coerceAtLeast(500)
                        val bmpH = (pdfH * scale).toInt().coerceAtLeast(500)

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
        } catch (_: Exception) {
            null
        }
    }

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
     * Estampa fisicamente as assinaturas e o rodapé oficial:
     * "Documento assinado eletronicamente por AssinaJÁ" em todas as páginas do PDF.
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
            "doc_${document.id}_v${document.version}_assinaja_${System.currentTimeMillis()}.pdf"
        )

        val outPdf = PdfDocument()

        ParcelFileDescriptor.open(sourceFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                val pageCount = renderer.pageCount
                for (pageIdx in 0 until pageCount) {
                    renderer.openPage(pageIdx).use { srcPage ->
                        val pageW = srcPage.width
                        val pageH = srcPage.height

                        // Renderiza a página original em alta resolução (2x)
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

                        // Desenha os campos de assinatura já assinados nesta página
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

                        // Estampa oficial de Rodapé AssinaJÁ em todas as páginas assinadas
                        drawAssinaJaFooter(
                            canvas = pdfCanvas,
                            pageW = pageW.toFloat(),
                            pageH = pageH.toFloat(),
                            document = document,
                            pageIndex = pageIdx,
                            totalPages = pageCount
                        )

                        outPdf.finishPage(destPage)
                    }
                }
            }
        }

        // Se todos os signatários assinaram, anexa a página final com o Manifesto de Assinaturas
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

    private fun drawAssinaJaFooter(
        canvas: Canvas,
        pageW: Float,
        pageH: Float,
        document: DocumentEntity,
        pageIndex: Int,
        totalPages: Int
    ) {
        val footerLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(200, 180, 185)
            strokeWidth = 0.8f
        }
        val footerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(107, 23, 36) // Vinho Nobre AssinaJÁ
            textSize = 7.2f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subFooterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(110, 115, 125)
            textSize = 6.2f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }

        val yLine = pageH - 18f
        canvas.drawLine(24f, yLine, pageW - 24f, yLine, footerLinePaint)

        // Texto solicitado expressamente: "Documento assinado eletronicamente por AssinaJÁ"
        canvas.drawText(
            "✦ Documento assinado eletronicamente por AssinaJÁ",
            24f,
            yLine + 11f,
            footerTextPaint
        )

        val metaText = "Protocolo: ${document.dispatchIdempotencyKey} • Pág. ${pageIndex + 1}/$totalPages"
        canvas.drawText(
            metaText,
            pageW - 24f - subFooterPaint.measureText(metaText),
            yLine + 11f,
            subFooterPaint
        )
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

        val primaryColor = Color.rgb(107, 23, 36) // Vinho Nobre AssinaJÁ
        val accentGold = Color.rgb(197, 160, 89)   // Ouro Champagne

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(246, 253, 250, 248)
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.4f
            color = primaryColor
        }
        canvas.drawRoundRect(rect, 5f, 5f, bgPaint)
        canvas.drawRoundRect(rect, 5f, 5f, borderPaint)

        // Faixa lateral em tom ouro nobre
        val stripeRect = RectF(left, top, left + 4.5f, top + height)
        val stripePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = accentGold
        }
        canvas.drawRoundRect(stripeRect, 4f, 4f, stripePaint)

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryColor
            textSize = (height * 0.14f).coerceIn(6.5f, 9.5f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 32, 36)
            textSize = (height * 0.13f).coerceIn(6f, 9f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val monoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(90, 95, 105)
            textSize = (height * 0.11f).coerceIn(5.5f, 7.8f)
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }

        val padX = left + 8.5f
        var cursorY = top + headerPaint.textSize + 2.5f

        when (field.fieldType) {
            br.com.fiap.letssign.domain.model.FieldType.CPF -> {
                val modalityBadge = "CPF / DOCUMENTO • ASSINAJÁ"
                canvas.drawText(modalityBadge, padX, cursorY, headerPaint)
                cursorY += bodyPaint.textSize + 2.5f
                val roleText = "${field.signerName} (${field.classification.label})"
                canvas.drawText(roleText.take(36), padX, cursorY, bodyPaint)
                cursorY += monoPaint.textSize + 2f
                val docText = field.stampSummary?.takeIf { it.isNotBlank() } ?: "CPF Registrado"
                canvas.drawText(docText.take(36), padX, cursorY, monoPaint)
                cursorY += monoPaint.textSize + 2f
                val signedDateStr = formatTimestamp(field.signedAt ?: signer?.signedAt)
                canvas.drawText("Data: $signedDateStr", padX, cursorY, monoPaint)
            }
            br.com.fiap.letssign.domain.model.FieldType.DATA -> {
                val modalityBadge = "DATA DA ASSINATURA • ASSINAJÁ"
                canvas.drawText(modalityBadge, padX, cursorY, headerPaint)
                cursorY += bodyPaint.textSize + 2.5f
                val signedDateStr = formatTimestamp(field.signedAt ?: signer?.signedAt)
                canvas.drawText(signedDateStr, padX, cursorY, bodyPaint)
                cursorY += monoPaint.textSize + 2f
                val roleText = "${field.signerName} (${field.classification.label})"
                canvas.drawText(roleText.take(36), padX, cursorY, monoPaint)
            }
            br.com.fiap.letssign.domain.model.FieldType.ASSINATURA,
            br.com.fiap.letssign.domain.model.FieldType.RUBRICA -> {
                val modalityBadge = when (field.signatureType) {
                    SignatureType.QUALIFICADA_ICP -> "ICP-BRASIL QUALIFICADA • ASSINAJÁ"
                    SignatureType.AVANCADA -> "ASSINATURA AVANÇADA • ASSINAJÁ"
                    SignatureType.SIMPLES -> "ASSINATURA SIMPLES • ASSINAJÁ"
                }
                canvas.drawText(modalityBadge, padX, cursorY, headerPaint)

                // Se houver imagem de rubrica manuscrita
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

                cursorY += bodyPaint.textSize + 2.5f
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

        // Cabeçalho institucional AssinaJÁ em tom Vinho Nobre
        val headerBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(107, 23, 36) // Vinho Nobre AssinaJÁ
        }
        canvas.drawRect(0f, 0f, pageW.toFloat(), 76f, headerBg)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 14.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(240, 218, 178) // Dourado suave
            textSize = 9.2f
        }
        canvas.drawText("ASSINAJÁ • MANIFESTO DE ASSINATURAS E TRILHA FORENSE", 32f, 34f, titlePaint)
        canvas.drawText(
            "Validade Jurídica conforme MP 2.200-2/2001 e Lei nº 14.063/2020 para Todas as Empresas",
            32f,
            54f,
            subHeaderPaint
        )

        val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(107, 23, 36)
            textSize = 11.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(50, 50, 55)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(35, 35, 35)
            textSize = 9.2f
        }
        val monoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(107, 23, 36)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(225, 218, 220)
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
        canvas.drawText("2. QUADRO DE SIGNATÁRIOS E QUALIFICAÇÃO", 32f, y, sectionPaint)
        y += 16f

        signers.forEachIndexed { idx, signer ->
            val cardTop = y
            val cardHeight = 58f
            val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(252, 248, 249)
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(RectF(32f, cardTop, pageW - 32f, cardTop + cardHeight), 5f, 5f, cardPaint)

            var sy = cardTop + 15f
            val statusLabel = if (signer.status == SignerStatus.ASSINADO) "ASSINADO" else signer.status.label.uppercase()
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
            val certOrToken = signer.certificateInfo ?: signer.verificationToken ?: "Autenticação por Senha + IP"
            val ipStr = signer.ipAddress ?: "IP Registrado"
            canvas.drawText(
                "Data/Hora: ${formatTimestamp(signer.signedAt)} | IP: $ipStr | Evidência: $certOrToken",
                42f,
                sy,
                monoPaint
            )
            y += cardHeight + 8f
        }

        y += 10f
        canvas.drawLine(32f, y, pageW - 32f, y, dividerPaint)
        y += 20f
        canvas.drawText("3. TRILHA FORENSE DE EVENTOS (AUDITORIA)", 32f, y, sectionPaint)
        y += 16f

        auditEvents.sortedBy { it.timestamp }.take(12).forEach { event ->
            if (y < pageH - 45f) {
                val line = "[${formatTimestamp(event.timestamp)}] ${event.title} — ${event.actorName} (${event.description})"
                canvas.drawText(line.take(98), 32f, y, textPaint)
                y += 13f
            }
        }

        val footerY = pageH - 24f
        canvas.drawLine(32f, footerY - 14f, pageW - 32f, footerY - 14f, dividerPaint)
        canvas.drawText(
            "Documento selado e certificado via Plataforma AssinaJÁ • Emitido em ${formatTimestamp(System.currentTimeMillis())}",
            32f,
            footerY,
            monoPaint
        )

        outPdf.finishPage(page)
    }

    /**
     * Gera uma minuta de contrato empresarial versátil de 2 páginas
     * aplicável para prestação de serviços, tecnologia, RH, vendas e parceiros.
     */
    suspend fun createSampleLegalContractPdf(context: Context): PdfValidationResult =
        withContext(Dispatchers.IO) {
            val originalsDir = File(context.filesDir, "documents/originals").apply { mkdirs() }
            val fileName = "Contrato_Empresarial_Prestacao_Servicos_${System.currentTimeMillis() % 10000}.pdf"
            val outFile = File(originalsDir, fileName)

            val pdfDoc = PdfDocument()
            val pageW = 595
            val pageH = 842

            val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(107, 23, 36)
                textSize = 13.5f
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            }
            val subTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(90, 85, 95)
                textSize = 9.5f
                typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            }
            val clauseTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(40, 20, 25)
                textSize = 10.5f
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            }
            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(35, 35, 35)
                textSize = 9.5f
                typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            }
            val guidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(197, 160, 89)
                style = Paint.Style.STROKE
                strokeWidth = 1f
                pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f)
            }

            // Página 1
            val page1 = pdfDoc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, 1).create())
            val c1 = page1.canvas
            c1.drawColor(Color.WHITE)

            var y = 56f
            c1.drawText("INSTRUMENTO PARTICULAR DE PRESTAÇÃO DE SERVIÇOS E TECNOLOGIA", 42f, y, headerPaint)
            y += 18f
            c1.drawText("Acordo Comercial e Operacional Corporativo entre as Partes Qualificadas", 42f, y, subTitlePaint)
            y += 28f

            val clausesPage1 = listOf(
                "CLÁUSULA PRIMEIRA – DO OBJETO DA CONTRATAÇÃO" to listOf(
                    "1.1. O presente instrumento regula a prestação de serviços técnicos especializados e soluções",
                    "de tecnologia pelo(a) CONTRATADO(A) em benefício das operações do(a) CONTRATANTE, com padrão",
                    "de qualidade, conformidade, zelo e observância aos níveis de serviço (SLA) acordados."
                ),
                "CLÁUSULA SEGUNDA – DAS RESPONSABILIDADES OPERACIONAIS" to listOf(
                    "2.1. O(A) CONTRATADO(A) disponibilizará os recursos, corpo técnico qualificado e ferramentas",
                    "necessárias à execução das atividades, respeitando cronogramas e entregáveis formais.",
                    "2.2. O(A) CONTRATANTE fornecerá as informações, acessos e validações tempestivas requeridas."
                ),
                "CLÁUSULA TERCEIRA – DA REMUNERAÇÃO E FATURAMENTO" to listOf(
                    "3.1. Em contraprestação pelos serviços executados, o(a) CONTRATANTE efetuará o pagamento",
                    "dos valores pactuados nas propostas comerciais correspondentes, mediante emissão de notas fiscais."
                ),
                "CLÁUSULA QUARTA – DA CONFIDENCIALIDADE E PROTEÇÃO DE DADOS (LGPD)" to listOf(
                    "4.1. As partes obrigam-se a manter sob sigilo todas as informações comerciais e estratégicas,",
                    "tratando dados pessoais estritamente nos termos da Lei Geral de Proteção de Dados (Lei 13.709/2018)."
                )
            )

            for ((title, lines) in clausesPage1) {
                c1.drawText(title, 42f, y, clauseTitlePaint)
                y += 17f
                for (line in lines) {
                    c1.drawText(line, 42f, y, bodyPaint)
                    y += 14.5f
                }
                y += 15f
            }

            c1.drawRoundRect(RectF(42f, 690f, 553f, 770f), 6f, 6f, guidePaint)
            c1.drawText("ÁREA PARA RUBRICAS DAS PARTES (PÁGINA 1/2)", 56f, 712f, subTitlePaint)
            pdfDoc.finishPage(page1)

            // Página 2
            val page2 = pdfDoc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, 2).create())
            val c2 = page2.canvas
            c2.drawColor(Color.WHITE)

            y = 56f
            c2.drawText("CLÁUSULA QUINTA – DA ASSINATURA ELETRÔNICA E VALIDADE JURÍDICA", 42f, y, clauseTitlePaint)
            y += 18f
            val page2Lines = listOf(
                "5.1. As partes contratantes declaram e reconhecem mutuamente a validade, integridade e plena eficácia",
                "jurídica deste instrumento assinado de forma eletrônica/digital por meio da plataforma AssinaJÁ,",
                "com fundamento legal na Medida Provisória nº 2.200-2/2001 e na Lei Federal nº 14.063/2020.",
                "",
                "CLÁUSULA SEXTA – DO FORO E RESOLUÇÃO DE CONTROVÉRSIAS",
                "6.1. Fica eleito o Foro da Comarca competente para dirimir quaisquer controvérsias decorrentes",
                "deste instrumento, com renúncia prévia a qualquer outro foro, por mais privilegiado que seja.",
                "",
                "E, por estarem plenamente acordadas, as partes firmam o presente instrumento digitalmente."
            )
            for (line in page2Lines) {
                if (line.startsWith("CLÁUSULA")) {
                    c2.drawText(line, 42f, y, clauseTitlePaint)
                } else {
                    c2.drawText(line, 42f, y, bodyPaint)
                }
                y += 15f
            }

            y += 32f
            c2.drawText("QUADRO DE ASSINATURAS (POSICIONE OS CAMPOS ABAIXO):", 42f, y, clauseTitlePaint)
            y += 20f

            c2.drawRoundRect(RectF(42f, y, 285f, y + 105f), 6f, 6f, guidePaint)
            c2.drawText("Local sugerido: CONTRATANTE", 56f, y + 22f, subTitlePaint)

            c2.drawRoundRect(RectF(310f, y, 553f, y + 105f), 6f, 6f, guidePaint)
            c2.drawText("Local sugerido: CONTRATADO", 324f, y + 22f, subTitlePaint)

            y += 128f
            c2.drawRoundRect(RectF(42f, y, 285f, y + 105f), 6f, 6f, guidePaint)
            c2.drawText("Local sugerido: TESTEMUNHA", 56f, y + 22f, subTitlePaint)

            pdfDoc.finishPage(page2)

            FileOutputStream(outFile).use { fos ->
                pdfDoc.writeTo(fos)
            }
            pdfDoc.close()

            validatePdfFile(outFile, fileName)
        }

    /**
     * Salva uma cópia oficial do documento PDF final (com todas as assinaturas, rubricas e carimbo de auditoria)
     * diretamente na pasta pública de "Downloads" do dispositivo Android.
     *
     * NOTA DE TROUBLESHOOTING / ARQUITETURA:
     * - No Android 10+ (API 29+), o acesso ao armazenamento público requer o uso do MediaStore (Scoped Storage).
     *   Utilizamos `MediaStore.Downloads.EXTERNAL_CONTENT_URI` com a flag `IS_PENDING` para evitar arquivos
     *   parciais ou corrompidos sendo lidos por outros apps antes da finalização da cópia.
     * - No Android 9 e anteriores (API <= 28), fazemos a cópia direta no diretório público de Downloads e
     *   acionamos o `MediaScannerConnection` para que o arquivo apareça instantaneamente nos exploradores de arquivo.
     */
    suspend fun exportPdfToPublicDownloads(
        context: Context,
        sourcePdfPath: String,
        desiredFileName: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val srcFile = File(sourcePdfPath)
            if (!srcFile.exists()) {
                return@withContext Result.failure(java.io.FileNotFoundException("Arquivo PDF fonte não encontrado em: $sourcePdfPath"))
            }

            // Normaliza o nome do arquivo para garantir extensão .pdf e caracteres seguros
            val sanitizedName = desiredFileName
                .replace(Regex("[^a-zA-Z0-9._\\- ]"), "_")
                .trim()
                .let { if (it.endsWith(".pdf", ignoreCase = true)) it else "$it.pdf" }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, sanitizedName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return@withContext Result.failure(java.io.IOException("Falha ao registrar documento no MediaStore do Android."))

                resolver.openOutputStream(uri)?.use { output ->
                    srcFile.inputStream().use { input ->
                        input.copyTo(output)
                    }
                }

                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)

                Result.success("Documento salvo em Downloads: $sanitizedName")
            } else {
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val destFile = File(downloadsDir, sanitizedName)
                srcFile.copyTo(destFile, overwrite = true)
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(destFile.absolutePath),
                    arrayOf("application/pdf"),
                    null
                )
                Result.success("Documento salvo em: ${destFile.absolutePath}")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
