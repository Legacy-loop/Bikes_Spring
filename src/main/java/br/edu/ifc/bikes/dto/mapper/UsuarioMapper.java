package br.edu.ifc.bikes.dto.mapper;

import br.edu.ifc.bikes.dto.UsuarioRequestDTO;
import br.edu.ifc.bikes.dto.UsuarioResponseDTO;
import br.edu.ifc.bikes.entity.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.util.List;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {
    Usuario toUsuario(UsuarioRequestDTO usuarioRequestDTO);

    @Mapping(target = "role", expression = "java(usuario.getRole().name().substring(\"ROLE_\".length()))")
    UsuarioResponseDTO toResponse(Usuario usuario);
    List<UsuarioResponseDTO> toResponse(List<Usuario> usuarios);
}