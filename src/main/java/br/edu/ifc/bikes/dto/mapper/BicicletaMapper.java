package br.edu.ifc.bikes.dto.mapper;

import br.edu.ifc.bikes.dto.BicicletaResponseDTO;
import br.edu.ifc.bikes.entity.Bicicleta;
import org.springframework.stereotype.Component;

@Component
public class BicicletaMapper {
    public BicicletaResponseDTO toResponse(Bicicleta bicicleta) {
        return new BicicletaResponseDTO(
                bicicleta.getId(),
                bicicleta.getCodigo(),
                bicicleta.getModelo(),
                bicicleta.getStatus(),
                bicicleta.getDataCriacao(),
                bicicleta.getDataModificacao(),
                bicicleta.getCriadoPor(),
                bicicleta.getModificadoPor()
        );
    }
}
