# LetsSign Jurídico — Mobile App (FIAP Challenge • Sprint 1)

Aplicativo móvel nativo para **Android (Kotlin + Jetpack Compose)** voltado à geração, gestão e assinatura eletrônica/digital de documentos jurídicos em formato PDF, desenvolvido para a **Sprint 1 do Challenge FIAP + LetsSign**.

---

## Funcionalidades Implementadas (Sprint 1)

### 1. Acesso e Perfis (`Admin` e `Signatário`)
- **Login unificado por credenciais (E-mail e Senha):**
  - **Admin Padrão:** `admin@letssign.com.br` / Senha: `admin123`
  - **Signatários:** Autenticam com o e-mail e a senha cadastrados pelo Admin.
- **Isolamento de Dados:** Documentos e dados de um usuário não são acessíveis por outro sem que tenham sido efetivamente enviados para ele.

### 2. Gestão de Signatários (Admin)
- Cadastro e edição de signatários com validação obrigatória de:
  - **Nome completo**
  - **E-mail** (credencial de autenticação)
  - **Telefone**
  - **Senha** (credencial de autenticação)
  - **Documento Jurídico (CPF / OAB)** (opcional, exibido no carimbo jurídico)

### 3. Fluxo de Cadastro e Envio de Documento PDF (Wizard de 4 Etapas)
1. **Upload e Validação de PDF:** Seleção de arquivo PDF no dispositivo (ou geração de minuta jurídica de exemplo) com validação de integridade via `PdfRenderer` e cálculo de **Hash SHA-256** (alertando arquivos inválidos ou corrompidos).
2. **Seleção, Classificação e Modalidade de Assinatura:**
   - Mínimo de **1 signatário obrigatório**, com suporte a múltiplos signatários, remoção e substituição antes do envio.
   - **Classificação Jurídica:** `Contratante`, `Contratado` ou `Testemunha`.
   - **Modalidades de Assinatura Digital (MP 2.200-2/2001 & Lei nº 14.063/2020):**
     - **Assinatura Eletrônica Simples:** Identificação por login/e-mail, IP, Timestamp e Hash SHA-256.
     - **Assinatura Eletrônica Avançada:** Rubrica manuscrita desenhada em Canvas + Token 2FA de verificação.
     - **Assinatura Digital Qualificada (ICP-Brasil):** Certificado Digital ICP-Brasil (A1/A3) + PIN + Rubrica em Canvas.
3. **Posicionamento Visual das Assinaturas (Drag & Drop):**
   - Visualização página a página do PDF e adição de um ou mais campos por signatário.
   - Persistência de página, posição e dimensões em **coordenadas relativas normalizadas (`0.0` a `1.0`)** em relação ao tamanho da página do PDF, preservando o posicionamento em qualquer dispositivo e garantindo que os campos permaneçam dentro dos limites da página.
4. **Revisão e Disparo Idempotente:**
   - Conferência do documento final, signatários, páginas/campos e mensagem de convite.
   - Controle de idempotência (`dispatchIdempotencyKey`) permitindo nova tentativa em caso de falha sem duplicar solicitações.

### 4. Acompanhamento, Versionamento e Assinatura no PDF
- **Acompanhamento completo:** Status geral (`Pendente`, `Parcialmente Assinado`, `Concluído`), status individual por signatário, datas/horários e trilha de auditoria cronológica.
- **Preservação e Nova Versão (`v2`, `v3`...):** O documento enviado é preservado; alterações posteriores geram uma nova versão e um novo fluxo.
- **Estampa Real no PDF + Manifesto Jurídico:** Ao assinar, o app grava visualmente a assinatura e o selo jurídico nas coordenadas exatas do PDF no sistema de arquivos do dispositivo e, quando todos os signatários obrigatórios assinam, anexa a **Página de Manifesto de Assinaturas e Trilha de Auditoria** ao PDF final.

---

## Tecnologias e Arquitetura
- **Linguagem & UI:** Kotlin, Jetpack Compose, Material Design 3, Navigation Compose
- **Persistência Local:** Room (SQLite) + Armazenamento de PDFs no File System interno (`filesDir`)
- **Arquitetura:** MVVM + Repository Pattern (com interface `LetsSignApiService` via Retrofit pronta para futura integração REST)
- **Manipulação de PDF:** `PdfRenderer`, `PdfDocument` e `Canvas` nativos do Android + `MessageDigest` (SHA-256)
