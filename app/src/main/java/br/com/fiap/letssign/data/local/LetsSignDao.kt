package br.com.fiap.letssign.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import br.com.fiap.letssign.domain.model.DocumentStatus
import br.com.fiap.letssign.domain.model.SignerStatus
import br.com.fiap.letssign.domain.model.UserRole
import kotlinx.coroutines.flow.Flow

@Dao
interface LetsSignDao {

    // ================== USUÁRIOS & SIGNATÁRIOS ==================
    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) AND password = :password LIMIT 1")
    suspend fun authenticate(email: String, password: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): UserEntity?

    @Query("SELECT * FROM users WHERE role = :role ORDER BY fullName ASC")
    fun observeUsersByRole(role: UserRole): Flow<List<UserEntity>>

    @Query("SELECT * FROM users ORDER BY role ASC, fullName ASC")
    fun observeAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT COUNT(*) FROM users WHERE role = :role")
    suspend fun countUsersByRole(role: UserRole): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    // ================== DOCUMENTOS (ADMIN & SIGNATÁRIO) ==================
    @Transaction
    @Query("SELECT * FROM documents ORDER BY createdAt DESC")
    fun observeAllDocumentsWithDetails(): Flow<List<DocumentFullDetails>>

    @Transaction
    @Query("SELECT * FROM documents WHERE id = :documentId LIMIT 1")
    fun observeDocumentDetails(documentId: Long): Flow<DocumentFullDetails?>

    @Transaction
    @Query("SELECT * FROM documents WHERE id = :documentId LIMIT 1")
    suspend fun getDocumentDetails(documentId: Long): DocumentFullDetails?

    @Query("SELECT * FROM documents WHERE dispatchIdempotencyKey = :idempotencyKey LIMIT 1")
    suspend fun getDocumentByIdempotencyKey(idempotencyKey: String): DocumentEntity?

    /**
     * Regra de negócio de isolamento:
     * "Dados e documentos de um usuário não poderão ser acessados por outro sem que sejam enviados."
     * Retorna apenas documentos com status != RASCUNHO nos quais o signatário logado participa.
     */
    @Transaction
    @Query(
        """
        SELECT DISTINCT d.* FROM documents d
        INNER JOIN document_signers ds ON d.id = ds.documentId
        WHERE ds.signerUserId = :signerUserId
          AND d.status != 'RASCUNHO'
        ORDER BY d.sentAt DESC, d.createdAt DESC
        """
    )
    fun observeDocumentsForSigner(signerUserId: Long): Flow<List<DocumentFullDetails>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertDocument(document: DocumentEntity): Long

    @Update
    suspend fun updateDocument(document: DocumentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocumentSigners(signers: List<DocumentSignerEntity>)

    @Update
    suspend fun updateDocumentSigner(signer: DocumentSignerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignatureFields(fields: List<SignatureFieldEntity>)

    @Update
    suspend fun updateSignatureFields(fields: List<SignatureFieldEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditEvent(event: AuditEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditEvents(events: List<AuditEventEntity>)

    @Query(
        """
        UPDATE document_signers
        SET signerName = :fullName, signerEmail = :email, signerPhone = :phone
        WHERE signerUserId = :userId AND status = 'PENDENTE'
        """
    )
    suspend fun syncPendingSignerInfo(userId: Long, fullName: String, email: String, phone: String)

    @Query(
        """
        UPDATE signature_fields
        SET signerName = :fullName
        WHERE signerUserId = :userId AND isSigned = 0
        """
    )
    suspend fun syncPendingFieldSignerName(userId: Long, fullName: String)
}
