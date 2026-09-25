package PDV.PDV.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookSignatureUtilTest {

    private static final String SECRET = "chave-de-teste-secreta";

    @Test
    void montaManifestComTodosOsPares() {
        String manifest = WebhookSignatureUtil.montarManifest("123456789", "req-abc", "1742505638683");
        assertThat(manifest)
                .isEqualTo("id:123456789;request-id:req-abc;ts:1742505638683;");
    }

    @Test
    void montaManifestOmitindoDataIdAusente() {
        String manifest = WebhookSignatureUtil.montarManifest(null, "req-abc", "1742505638683");
        assertThat(manifest).isEqualTo("request-id:req-abc;ts:1742505638683;");
    }

    @Test
    void montaManifestOmitindoRequestIdAusente() {
        String manifest = WebhookSignatureUtil.montarManifest("123456789", null, "1742505638683");
        assertThat(manifest).isEqualTo("id:123456789;ts:1742505638683;");
    }

    @Test
    void hmacProduzHexDe64Caracteres() {
        String hex = WebhookSignatureUtil.hmacSha256Hex(SECRET, "qualquer mensagem");
        assertThat(hex).hasSize(64).matches("[0-9a-f]{64}");
    }

    // Vetor de teste conhecido (HMAC-SHA256 com chave "key") para validar a implementação.
    @Test
    void hmacBateComVetorDeTesteConhecido() {
        String hex = WebhookSignatureUtil.hmacSha256Hex("key", "The quick brown fox jumps over the lazy dog");
        assertThat(hex).isEqualTo("f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8");
    }

    @Test
    void assinaturaConfereQuandoManifestEstaCorreto() {
        String ts = "1742505638683";
        String requestId = "req-123";
        String dataId = "987654321";
        String v1 = WebhookSignatureUtil.hmacSha256Hex(SECRET, WebhookSignatureUtil.montarManifest(dataId, requestId, ts));

        assertThat(WebhookSignatureUtil.assinaturaConfere(SECRET, dataId, requestId, ts, v1)).isTrue();
    }

    @Test
    void assinaturaFalhaComDataIdDiferente() {
        String ts = "1742505638683";
        String v1 = WebhookSignatureUtil.hmacSha256Hex(SECRET, WebhookSignatureUtil.montarManifest("111", "req", ts));

        assertThat(WebhookSignatureUtil.assinaturaConfere(SECRET, "999", "req", ts, v1)).isFalse();
    }

    @Test
    void assinaturaFalhaComSecretDiferente() {
        String ts = "1742505638683";
        String manifest = WebhookSignatureUtil.montarManifest("987", "req", ts);
        String v1 = WebhookSignatureUtil.hmacSha256Hex("outra-chave", manifest);

        assertThat(WebhookSignatureUtil.assinaturaConfere(SECRET, "987", "req", ts, v1)).isFalse();
    }

    @Test
    void timestampDentroDaJanelaEhValido() {
        assertThat(WebhookSignatureUtil.timestampValido("1742505638683", 1742505638683L, 300_000L)).isTrue();
        assertThat(WebhookSignatureUtil.timestampValido("1742505638683", 1742505638683L + 100_000L, 300_000L)).isTrue();
        assertThat(WebhookSignatureUtil.timestampValido("1742505638683", 1742505638683L - 300_000L, 300_000L)).isTrue();
    }

    @Test
    void timestampForaDaJanelaEhInvalido() {
        assertThat(WebhookSignatureUtil.timestampValido("1742505638683", 1742505638683L + 400_000L, 300_000L)).isFalse();
        assertThat(WebhookSignatureUtil.timestampValido("1742505638683", 1742505638683L - 400_000L, 300_000L)).isFalse();
    }

    @Test
    void timestampNaoNumericoEhInvalido() {
        assertThat(WebhookSignatureUtil.timestampValido("abc", 0L, 300_000L)).isFalse();
    }
}
