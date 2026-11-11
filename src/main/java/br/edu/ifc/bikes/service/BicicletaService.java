package br.edu.ifc.bikes.service;

import br.edu.ifc.bikes.dto.BicicletaRequestDTO;
import br.edu.ifc.bikes.dto.BicicletaResponseDTO;
import br.edu.ifc.bikes.dto.mapper.BicicletaMapper;
import br.edu.ifc.bikes.entity.Bicicleta;
import br.edu.ifc.bikes.exception.BicicletaConflictException;
import br.edu.ifc.bikes.exception.EntityNotFoundException;
import br.edu.ifc.bikes.repository.BicicletaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class BicicletaService {
    private final BicicletaRepository bicicletaRepository;
    private final BicicletaMapper bicicletaMapper;

    @Transactional
    public BicicletaResponseDTO create(BicicletaRequestDTO request) {
        if (bicicletaRepository.existsByCodigo(request.codigo())) {
            throw new BicicletaConflictException("Código de bicicleta já cadastrado");
        }

        Bicicleta bicicleta = new Bicicleta();
        bicicleta.setCodigo(request.codigo());
        bicicleta.setModelo(request.modelo());
        bicicleta.setStatus(request.status());
        try {
            return bicicletaMapper.toResponse(bicicletaRepository.saveAndFlush(bicicleta));
        } catch (DataIntegrityViolationException ex) {
            throw new BicicletaConflictException("Código de bicicleta já cadastrado");
        }
    }

    @Transactional(readOnly = true)
    public BicicletaResponseDTO getByCodigo(String codigo) {
        return bicicletaMapper.toResponse(bicicletaRepository.findByCodigo(codigo).orElseThrow(
                () -> new EntityNotFoundException(String.format("Bicicleta com código '%s' não encontrada", codigo))
        ));
    }

    @Transactional(readOnly = true)
    public List<BicicletaResponseDTO> getAll() {
        return bicicletaRepository.findAll().stream().map(bicicletaMapper::toResponse).toList();
    }
}
