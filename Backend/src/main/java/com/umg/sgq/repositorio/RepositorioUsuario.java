package com.umg.sgq.repositorio;

import com.umg.sgq.entidad.Usuario;
import com.umg.sgq.enumeracion.EstadoRegistro;
import com.umg.sgq.enumeracion.CodigoRol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RepositorioUsuario extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsernameIgnoreCase(String username);

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    @Query("""
            select u
            from Usuario u
            where u.role.code = :role
              and u.status = :status
              and (:branchId is null or u.branch.id = :branchId)
            order by u.fullName
            """)
    List<Usuario> findActiveByRoleAndBranch(
            @Param("role") CodigoRol role,
            @Param("status") EstadoRegistro status,
            @Param("branchId") Long branchId
    );

    @Modifying
    @Transactional
    @Query("""
            update Usuario u
            set u.lastActivityAt = :now
            where u.id = :userId
            """)
    int updateLastActivity(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now
    );

    @Modifying
    @Transactional
    @Query("""
            update Usuario u
            set u.lastActivityAt = null
            where u.id = :userId
            """)
    int clearLastActivity(
            @Param("userId") Long userId
    );
}