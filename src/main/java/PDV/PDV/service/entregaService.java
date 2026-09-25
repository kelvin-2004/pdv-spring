package PDV.PDV.service;

import PDV.PDV.dto.ResultadoEntrega;
import PDV.PDV.model.bairroEntrega;
import PDV.PDV.repository.bairroEntregaRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class entregaService {

    private static final String BRASILAPI_URL = "https://brasilapi.com.br/api/cep/v2/";
    private static final String GEOAPIFY_URL = "https://api.geoapify.com/v1/geocode/search";

    private static final String ENDERECO_ORIGEM = "R. Quinze, 292 - Jardim Vassouras, Francisco Morato - SP, 07953-170";
    private static final double LAT_ORIGEM = -23.2668724;
    private static final double LON_ORIGEM = -46.7243454;
    private static final BigDecimal TAXA_PADRAO_MORATO = new BigDecimal("10.00");

    @Autowired
    private bairroEntregaRepository bairroRepo;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${GEOAPIFY_API_KEY:}")
    private String geoapifyApiKey;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public String getEnderecoOrigem() {
        return ENDERECO_ORIGEM;
    }

    public List<bairroEntrega> listarBairros() {
        return bairroRepo.findAllByOrderByNomeAsc();
    }

    public void adicionarBairro(String nome, String taxaEntrega, String tempoEntrega) {
        bairroEntrega b = new bairroEntrega();
        b.setNome(validarNome(nome));
        b.setTaxaEntrega(parseValor(taxaEntrega));
        b.setTempoEntrega(limparTempo(tempoEntrega));
        bairroRepo.save(b);
    }

    public void editarBairro(Long id, String nome, String taxaEntrega, String tempoEntrega) {
        bairroEntrega b = bairroRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bairro não encontrado."));
        b.setNome(validarNome(nome));
        b.setTaxaEntrega(parseValor(taxaEntrega));
        b.setTempoEntrega(limparTempo(tempoEntrega));
        bairroRepo.save(b);
    }

    public void removerBairro(Long id) {
        bairroRepo.deleteById(id);
    }

    private String validarNome(String nome) {
        if (nome == null || nome.trim().isBlank()) {
            throw new IllegalArgumentException("Informe o nome do bairro.");
        }
        return nome.trim();
    }

    private BigDecimal parseValor(String valor) {
        try {
            return new BigDecimal(valor.replace(",", ".").trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Informe um valor numérico válido para a taxa.");
        }
    }

    private String limparTempo(String tempo) {
        if (tempo == null) return null;
        String t = tempo.trim();
        return t.isBlank() ? null : t;
    }

    public ResultadoEntrega consultarEntrega(String cep) throws Exception {
        JsonNode dados = geocodificarCep(cep);
        String cepTexto = dados.path("cep").asText("");
        String logradouro = dados.path("street").asText("");
        String bairro = dados.path("neighborhood").asText("");
        String cidade = dados.path("city").asText("");
        String estado = dados.path("state").asText("");

        double[] coords = geocodificarEndereco(cepTexto, logradouro, bairro, cidade, estado);
        double distancia = calcularDistanciaKm(LAT_ORIGEM, LON_ORIGEM, coords[0], coords[1]);

        ResultadoEntrega r = new ResultadoEntrega();
        r.setSucesso(true);
        r.setCep(cepTexto);
        r.setLogradouro(logradouro);
        r.setBairro(bairro);
        r.setCidade(cidade);
        r.setDistanciaKm(distancia);

        bairroEntrega cobrado = casarBairro(bairro);
        if (cobrado != null) {
            r.setDentroDaArea(true);
            r.setTaxaPadrao(false);
            r.setBairroAplicado(cobrado.getNome());
            r.setValorEntrega(cobrado.getTaxaEntrega());
            r.setTempoEntrega(cobrado.getTempoEntrega());
        } else if (isFranciscoMorato(cidade)) {
            r.setDentroDaArea(true);
            r.setTaxaPadrao(true);
            r.setBairroAplicado(bairro);
            r.setValorEntrega(TAXA_PADRAO_MORATO);
            r.setTempoEntrega(null);
        } else {
            r.setDentroDaArea(false);
            r.setTaxaPadrao(false);
            r.setBairroAplicado(null);
            r.setValorEntrega(null);
            r.setTempoEntrega(null);
            r.setMensagem("Não atendemos essa região.");
        }
        return r;
    }

    private JsonNode geocodificarCep(String cep) throws Exception {
        String cepLimpo = cep == null ? "" : cep.replaceAll("\\D", "");
        if (cepLimpo.length() != 8) {
            throw new IllegalArgumentException("Informe um CEP válido com 8 dígitos.");
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BRASILAPI_URL + cepLimpo))
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", "MarmitasSousa/1.0")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalArgumentException("CEP não encontrado.");
        }
        JsonNode node = objectMapper.readTree(response.body());
        if (node.path("cep").asText("").isBlank()) {
            throw new IllegalArgumentException("CEP não encontrado.");
        }
        return node;
    }

    private double[] geocodificarEndereco(String cep, String logradouro, String bairro, String cidade, String estado) throws Exception {
        String[][] tentativas = {
                {cep},
                {logradouro, bairro, cidade, estado},
                {logradouro, cidade, estado},
                {bairro, cidade, estado},
                {cidade, estado}
        };
        for (String[] partes : tentativas) {
            String query = montarEndereco(partes);
            double[] coords = buscarGeoapify(query);
            if (coords != null) {
                return coords;
            }
        }
        throw new IllegalArgumentException("Endereço não encontrado para calcular a distância.");
    }

    private double[] buscarGeoapify(String query) throws Exception {
        if (geoapifyApiKey == null || geoapifyApiKey.isBlank()) {
            throw new IllegalArgumentException("Chave do Geoapify não configurada (GEOAPIFY_API_KEY).");
        }
        String url = GEOAPIFY_URL + "?text=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
                + "&format=json&limit=1&apiKey=" + geoapifyApiKey;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            return null;
        }
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode results = root.path("results");
        if (!results.isArray() || results.isEmpty()) {
            return null;
        }
        JsonNode primeiro = results.get(0);
        double lat = primeiro.path("lat").asDouble(Double.NaN);
        double lon = primeiro.path("lon").asDouble(Double.NaN);
        if (Double.isNaN(lat) || Double.isNaN(lon)) {
            return null;
        }
        return new double[]{lat, lon};
    }

    private String montarEndereco(String... partes) {
        String endereco = Arrays.stream(partes)
                .filter(p -> p != null && !p.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(", "));
        return endereco + ", Brasil";
    }

    private bairroEntrega casarBairro(String bairroCep) {
        String alvo = normalizar(bairroCep);
        if (alvo.isBlank()) {
            return null;
        }
        List<bairroEntrega> bairros = listarBairros();
        for (bairroEntrega b : bairros) {
            String nome = normalizar(b.getNome());
            if (!nome.isBlank() && alvo.equals(nome)) {
                return b;
            }
        }
        bairroEntrega melhor = null;
        int melhorTamanho = -1;
        for (bairroEntrega b : bairros) {
            String nome = normalizar(b.getNome());
            if (nome.isBlank()) {
                continue;
            }
            if (alvo.contains(nome) || nome.contains(alvo)) {
                if (nome.length() > melhorTamanho) {
                    melhorTamanho = nome.length();
                    melhor = b;
                }
            }
        }
        return melhor;
    }

    private String normalizar(String s) {
        if (s == null) {
            return "";
        }
        String n = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();
        n = n.replaceAll("\\biii\\b", "3")
             .replaceAll("\\bii\\b", "2")
             .replaceAll("\\bi\\b", "1");
        return n.replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean isFranciscoMorato(String cidade) {
        if (cidade == null) {
            return false;
        }
        String c = Normalizer.normalize(cidade, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .trim();
        return c.equals("francisco morato");
    }

    public double calcularDistanciaKm(double lat1, double lon1, double lat2, double lon2) {
        double raioTerra = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return raioTerra * c;
    }

    @PostConstruct
    public void semearBairrosPadrao() {
        if (bairroRepo.count() > 0) {
            return;
        }
        String[][] padrao = {
                {"Jardim Vassouras", "5", null},
                {"Jardim Vassouras II", "5", null},
                {"Jardim Sílvia", "5", null},
                {"Recanto Soraya", "5", null},
                {"Jardim Esperança", "5", null},
                {"Recanto Feliz", "6", null},
                {"Recanto Regina", "7", null},
                {"Arpoador", "7", null},
                {"Parque 120", "7", null},
                {"Jardim Rosas", "8", null},
                {"Jardim Olga", "8", null},
                {"Santa Catarina", "8", null},
                {"Jardim dos Bandeirantes", "12", null},
                {"Centro", "10", null},
                {"Jardim São José", "10", null},
                {"Jardim Professor", "10", null},
                {"Jardim dos Lagos", "12", null},
                {"Jardim São João", "12", null},
                {"Jardim Nossa Senhora Aparecida", "10", null},
                {"Parque Paulista", "14", null},
                {"Jardim Astúrias", "6", null},
                {"Parque Santana", "14", null},
                {"Belém Capela", "7", null}
        };
        for (String[] p : padrao) {
            bairroEntrega b = new bairroEntrega();
            b.setNome(p[0]);
            b.setTaxaEntrega(new BigDecimal(p[1]));
            b.setTempoEntrega(p[2]);
            bairroRepo.save(b);
        }
    }
}
