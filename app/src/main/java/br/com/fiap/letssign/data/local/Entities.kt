package br.com.fiap.letssign.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import br.com.fiap.letssign.domain.model.AuditEventType
import br.com.fiap.letssign.domain.model.DocumentStatus
import br.com.fiap.letssign.domain.model.SignatureType
import br.com.fiap.letssign.domain.model.SignerClassification
import br.com.fiap.letssign.domain.model.SignerStatus
import br.com.fiap.letssign.domain.model.UserRole

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fullName: String,
    val email: String,
    val phone: String,
    val password: String,
    val legalDocument: String = "", // CPF ou OAB opcional para o carimbo jurídico
    val role: UserRole,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "documents",
    indices = [Index(value = ["dispatchIdempotencyKey"], unique = true)]
)
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val originalFileName: String,
    val originalPdfPath: String,
    val currentPdfPath: String,
    val pageCount: Int,
    val originalSha256: String,
    val currentSha256: String,
    val invitationMessage: String,
    val status: DocumentStatus,
    val version: Int = 1,
    val parentDocumentId: Long? = null,
    val createdByAdminId: Long,
    val dispatchIdempotencyKey: String,
    val createdAt: Long = System.currentTimeMillis(),
    val sentAt: Long? = null,
    val completedAt: Long? = null
)

@Entity(
    tableName = "document_signers",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["signerUserId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["documentId"]),
        Index(value = ["signerUserId"])
    ]
)
data class DocumentSignerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val documentId: Long,
    val signerUserId: Long,
    val signerName: String,
    val signerEmail: String,
    val signerPhone: String,
    val classification: SignerClassification,
    val signatureType: SignatureType,
    val isMandatory: Boolean = true,
    val status: SignerStatus = SignerStatus.PENDENTE,
    val signedAt: Long? = null,
    val signatureImagePath: String? = null,
    val certificateInfo: String? = null,
    val verificationToken: String? = null,
    val signatureHash: String? = null,
    val ipAddress: String? = null
)

@Entity(
    tableName = "signature_fields",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["documentId"]),
        Index(value = ["signerUserId"])
    ]
)
data class SignatureFieldEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val documentId: Long,
    val signerUserId: Long,
    val signerName: String,
    val classification: SignerClassification,
    val signatureType: SignatureType,
    val pageIndex: Int,
    /** Coordenada horizontal normalizada (0.0 a 1.0) relativa à largura da página do PDF */
    val xRatio: Float,
    /** Coordenada vertical normalizada (0.0 a 1.0) relativa à altura da página do PDF */
    val yRatio: Float,
    /** Largura normalizada (0.0 a 1.0) relativa à largura da página do PDF */
    val widthRatio: Float,
    /** Altura normalizada (0.0 a 1.0) relativa à altura da página do PDF */
    val heightRatio: Float,
    val isSigned: Boolean = false,
    val signedAt: Long? = null,
    val signatureImagePath: String? = null,
    val stampSummary: String? = null
)

@Entity(
    tableName = "audit_events",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["documentId"])]
)
data class AuditEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val documentId: Long,
    val eventType: AuditEventType,
    val title: String,
    val description: String,
    val actorName: String,
    val actorEmail: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class DocumentFullDetails(
    @Embedded
    val document: DocumentEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "documentId"
    )
    val signers: List<DocumentSignerEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "documentId"
    )
    val fields: List<SignatureFieldEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "documentId"
    )
    val auditEvents: List<AuditEventEntity>
)
