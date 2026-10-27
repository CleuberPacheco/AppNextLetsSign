package br.com.fiap.letssign

import br.com.fiap.letssign.domain.model.DocumentStatus
import br.com.fiap.letssign.domain.model.DraftSignatureField
import br.com.fiap.letssign.domain.model.SignatureType
import br.com.fiap.letssign.domain.model.SignerClassification
import br.com.fiap.letssign.domain.model.SignerStatus
import br.com.fiap.letssign.util.PdfEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainRulesTest {

    @Test
    fun `clamped signature field stays strictly inside page boundaries`() {
        val outOfBoundsField = DraftSignatureField(
            id = "f1",
            signerUserId = 10L,
            signerName = "Carlos Mendes",
            classification = SignerClassification.CONTRATANTE,
            signatureType = SignatureType.QUALIFICADA_ICP,
            pageIndex = 0,
            xRatio = 0.85f,
            yRatio = -0.20f,
            widthRatio = 0.40f,
            heightRatio = 0.12f
        ).clamped()

        assertTrue(outOfBoundsField.xRatio >= 0f)
        assertTrue(outOfBoundsField.yRatio >= 0f)
        assertTrue(outOfBoundsField.xRatio + outOfBoundsField.widthRatio <= 1.0001f)
        assertTrue(outOfBoundsField.yRatio + outOfBoundsField.heightRatio <= 1.0001f)
        assertEquals(0.60f, outOfBoundsField.xRatio, 0.001f)
        assertEquals(0.0f, outOfBoundsField.yRatio, 0.001f)
    }

    @Test
    fun `sha256 calculation produces deterministic 64 char hex string`() {
        val hash1 = PdfEngine.calculateStringSha256("Contrato AssinaJA FIAP")
        val hash2 = PdfEngine.calculateStringSha256("Contrato AssinaJA FIAP")
        assertEquals(64, hash1.length)
        assertEquals(hash1, hash2)
    }

    @Test
    fun `document and signer status enum values support rejection`() {
        assertEquals("Rejeitado", DocumentStatus.REJEITADO.label)
        assertEquals("Rejeitado", SignerStatus.REJEITADO.label)
        assertEquals("Concluído", DocumentStatus.CONCLUIDO.label)
    }

    @Test
    fun `local ip address fallback provides valid non empty string`() {
        val ip = PdfEngine.getLocalIpAddress()
        assertNotNull(ip)
        assertTrue(ip.isNotBlank())
    }
}
