package uy.edu.um.porytic2026.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uy.edu.um.porytic2026.entities.Usuario;

//Una interfaz define las operaciones que puede tener un objeto y el tipo de dato
// eg: class [Class] implements [Interface]
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    public boolean existByUserName(String userName);
}
