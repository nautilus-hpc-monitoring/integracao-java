package school.sptech.dto;

public record NodeContextoDto(String empresa, String hpc, String cluster, String node,
                              String proprietarioNome, String proprietarioEmail) {

    public static NodeContextoDto desconhecido(String idNode) {
        return new NodeContextoDto("?", "?", "?", idNode, "Sem proprietário", "-");
    }
}
