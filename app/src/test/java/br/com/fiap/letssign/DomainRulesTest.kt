package br.com.fiap.letssign

import br.com.fiap.letssign.domain.model.DraftSignatureField
import br.com.fiap.letssign.domain.model.SignatureType
import br.com.fiap.letssign.domain.model.SignerClassification
import br.com.fiap.letssign.util.PdfEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainRulesTest {

    @Test
    fun `clamped signature field stays strictly inside page boundaries`() {
        val outOfBoundsField = DraftSignatureField(
            id = "f1",
            signerUserId = 10L,
            signerName = "Dr. Marcos Silva",
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
        val hash1 = PdfEngine.calculateStringSha256("Contrato LetsSign FIAP")
        val hash2 = PdfEngine.calculateStringSha256("Contrato LetsSign FIAP")
        assertEquals(64, hash1.length)
        assertEquals(hash1, hash2)
    }
}
