package uy.edu.um.porytic2026.services;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uy.edu.um.porytic2026.entities.User;
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

        User newUser = User.builder()
                .firstName(nombre)
                .lastName(apellido)
                .username(userName)
                .build();

        newUser = usuarioRepository.save(newUser);
        return newUser.getId()>0L ? true: false;


    }
}
