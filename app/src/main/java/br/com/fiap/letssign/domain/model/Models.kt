package br.com.fiap.letssign.domain.model

enum class UserRole(val label: String) {
    ADMIN("Administrador"),
    SIGNATARIO("Signatário")
}

enum class SignerClassification(val label: String, val legalDescription: String) {
    CONTRATANTE("Contratante", "Parte que contrata ou outorga o instrumento jurídico"),
    CONTRATADO("Contratado", "Parte que presta o serviço ou assume a obrigação contratual"),
    TESTEMUNHA("Testemunha", "Terceiro que atesta a regularidade do ato jurídico (Art. 784, III CPC)")
}

enum class SignatureType(
    val shortName: String,
    val fullName: String,
    val legalBasis: String,
    val description: String,
    val requiresHandwrittenCanvas: Boolean,
    val requiresToken: Boolean,
    val requiresIcpCertificate: Boolean
) {
    SIMPLES(
        shortName = "Simples",
        fullName = "Assinatura Eletrônica Simples",
        legalBasis = "Lei nº 14.063/2020, Art. 4º, I",
        description = "Permite identificar o signatário por credenciais de acesso (E-mail/IP/Timestamp) e anexa dados eletrônicos ao Hash SHA-256 do documento.",
        requiresHandwrittenCanvas = false,
        requiresToken = false,
        requiresIcpCertificate = false
    ),
    AVANCADA(
        shortName = "Avançada",
        fullName = "Assinatura Eletrônica Avançada",
        legalBasis = "Lei nº 14.063/2020, Art. 4º, II & MP 2.200-2/2001, Art. 10, § 2º",
        description = "Utiliza rubrica manuscrita desenhada em Canvas combinada com Token de validação e controle exclusivo do signatário, detectando qualquer modificação posterior.",
        requiresHandwrittenCanvas = true,
        requiresToken = true,
        requiresIcpCertificate = false
    ),
    QUALIFICADA_ICP(
        shortName = "Qualificada ICP-Brasil",
        fullName = "Assinatura Digital Qualificada (ICP-Brasil)",
        legalBasis = "MP 2.200-2/2001, Art. 10, § 1º & Lei nº 14.063/2020, Art. 4º, III",
        description = "Realizada com Certificado Digital padrão ICP-Brasil (e-CPF / OAB A1 ou A3), conferindo presunção legal absoluta de autoria, autenticidade e integridade.",
        requiresHandwrittenCanvas = true,
        requiresToken = false,
        requiresIcpCertificate = true
    )
}

enum class DocumentStatus(val label: String) {
    RASCUNHO("Rascunho"),
    ENVIADO("Pendente de Assinaturas"),
    PARCIALMENTE_ASSINADO("Parcialmente Assinado"),
    CONCLUIDO("Concluído")
}

enum class SignerStatus(val label: String) {
    PENDENTE("Pendente"),
    ASSINADO("Assinado")
}

enum class AuditEventType(val label: String) {
    DOCUMENTO_CRIADO("Documento Criado"),
    SIGNATARIOS_CONFIGURADOS("Signatários Vinculados"),
    CAMPOS_POSICIONADOS("Campos de Assinatura Posicionados"),
    DOCUMENTO_ENVIADO("Solicitações Disparadas"),
    DOCUMENTO_VISUALIZADO("Documento Visualizado"),
    ASSINATURA_REALIZADA("Assinatura Efetuada"),
    NOVA_VERSAO_GERADA("Nova Versão Gerada"),
    DOCUMENTO_CONCLUIDO("Fluxo Concluído")
}

/**
 * Representa um signatário selecionado durante o fluxo (Wizard) de envio de documento,
 * antes ou depois de persistir no banco.
 */
data class DraftSignerSelection(
    val userId: Long,
    val fullName: String,
    val email: String,
    val phone: String,
    val classification: SignerClassification = SignerClassification.CONTRATANTE,
    val signatureType: SignatureType = SignatureType.AVANCADA,
    val isMandatory: Boolean = true
)

/**
 * Representa um campo de assinatura posicionado sobre uma página do PDF
 * usando coordenadas relativas normalizadas (0.0f a 1.0f) em relação às dimensões da página.
 */
data class DraftSignatureField(
    val id: String,
    val signerUserId: Long,
    val signerName: String,
    val classification: SignerClassification,
    val signatureType: SignatureType,
    val pageIndex: Int,
    val xRatio: Float,
    val yRatio: Float,
    val widthRatio: Float = 0.38f,
    val heightRatio: Float = 0.11f
) {
    /**
     * Garante a regra de negócio: "Campos de assinatura devem permanecer dentro dos limites da página".
     */
    fun clamped(): DraftSignatureField {
        val safeW = widthRatio.coerceIn(0.18f, 0.85f)
        val safeH = heightRatio.coerceIn(0.06f, 0.35f)
        val safeX = xRatio.coerceIn(0f, (1f - safeW).coerceAtLeast(0f))
        val safeY = yRatio.coerceIn(0f, (1f - safeH).coerceAtLeast(0f))
        return copy(
            xRatio = safeX,
            yRatio = safeY,
            widthRatio = safeW,
            heightRatio = safeH
        )
    }
}
