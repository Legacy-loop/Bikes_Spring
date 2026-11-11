package br.edu.ifc.bikes.service;

import br.edu.ifc.bikes.dto.ClienteRequestDTO;
import br.edu.ifc.bikes.dto.ClienteResponseDTO;
import br.edu.ifc.bikes.dto.mapper.ClienteMapper;
import br.edu.ifc.bikes.entity.Cliente;
import br.edu.ifc.bikes.exception.ClienteConflictException;
import br.edu.ifc.bikes.exception.EntityNotFoundException;
import br.edu.ifc.bikes.repository.ClienteRepository;
import br.edu.ifc.bikes.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class ClienteService {
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final ClienteMapper clienteMapper;

    @Transactional
    public ClienteResponseDTO create(ClienteRequestDTO request, Long usuarioId) {
        if (clienteRepository.existsByCpf(request.cpf())) {
            throw new ClienteConflictException("CPF já cadastrado no sistema");
        }
        if (clienteRepository.existsByUsuarioId(usuarioId)) {
            throw new ClienteConflictException("Este usuário já possui um cadastro de cliente");
        }

        Cliente cliente = new Cliente();
        cliente.setNome(request.nome());
        cliente.setCpf(request.cpf());
        cliente.setTelefone(request.telefone());
        cliente.setEmail(request.email());
        cliente.setUsuario(usuarioRepository.findById(usuarioId).orElseThrow(
                () -> new EntityNotFoundException(String.format("Usuário com id=%d não encontrado", usuarioId))
        ));
        try {
            return clienteMapper.toResponse(clienteRepository.saveAndFlush(cliente));
        } catch (DataIntegrityViolationException ex) {
            throw new ClienteConflictException("CPF ou usuário já possui cadastro de cliente");
        }
    }

    @Transactional(readOnly = true)
    public ClienteResponseDTO getByUsuarioId(Long usuarioId) {
        return clienteMapper.toResponse(clienteRepository.findByUsuarioId(usuarioId).orElseThrow(
                () -> new EntityNotFoundException("Cliente do usuário autenticado não encontrado")
        ));
    }

    @Transactional(readOnly = true)
    public ClienteResponseDTO getById(Long id) {
        return clienteMapper.toResponse(clienteRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException(String.format("Cliente com id=%d não encontrado", id))
        ));
    }

    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> getAll() {
        return clienteRepository.findAll().stream().map(clienteMapper::toResponse).toList();
    }
}
