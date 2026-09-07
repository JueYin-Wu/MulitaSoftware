package uy.edu.um.porytic2026.services;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uy.edu.um.porytic2026.entities.Usuario;
import uy.edu.um.porytic2026.repository.UsuarioRepository;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    public boolean altaUsuario(String nombre, String apellido){
        if (nombre == null || apellido == null)
            return false;

        int i = 1;
        String userName = nombre + "." + apellido;
        while (usuarioRepository.existByUserName((userName)))
            userName = userName + i++;

        Usuario nuevoUsuario = Usuario.builder()
                .name(nombre)
                .lastName(apellido)
                .userName(userName)
                .build();

        nuevoUsuario = usuarioRepository.save(nuevoUsuario);
        return nuevoUsuario.getId()>0L ? true: false;


    }
}
