package br.com.fiap.letssign.data.local

import androidx.room.TypeConverter
import br.com.fiap.letssign.domain.model.AuditEventType
import br.com.fiap.letssign.domain.model.DocumentStatus
import br.com.fiap.letssign.domain.model.SignatureType
import br.com.fiap.letssign.domain.model.SignerClassification
import br.com.fiap.letssign.domain.model.SignerStatus
import br.com.fiap.letssign.domain.model.UserRole

class Converters {
    @TypeConverter
    fun fromUserRole(value: UserRole): String = value.name

    @TypeConverter
    fun toUserRole(value: String): UserRole = UserRole.valueOf(value)

    @TypeConverter
    fun fromSignerClassification(value: SignerClassification): String = value.name

    @TypeConverter
    fun toSignerClassification(value: String): SignerClassification =
        SignerClassification.valueOf(value)

    @TypeConverter
    fun fromSignatureType(value: SignatureType): String = value.name

    @TypeConverter
    fun toSignatureType(value: String): SignatureType = SignatureType.valueOf(value)

    @TypeConverter
    fun fromDocumentStatus(value: DocumentStatus): String = value.name

    @TypeConverter
    fun toDocumentStatus(value: String): DocumentStatus = DocumentStatus.valueOf(value)

    @TypeConverter
    fun fromSignerStatus(value: SignerStatus): String = value.name

    @TypeConverter
    fun toSignerStatus(value: String): SignerStatus = SignerStatus.valueOf(value)

    @TypeConverter
    fun fromAuditEventType(value: AuditEventType): String = value.name

    @TypeConverter
    fun toAuditEventType(value: String): AuditEventType = AuditEventType.valueOf(value)
}
