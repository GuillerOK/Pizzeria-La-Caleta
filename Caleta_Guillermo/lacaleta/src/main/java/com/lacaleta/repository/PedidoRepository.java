package com.lacaleta.repository;

import com.lacaleta.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Integer> {
    @org.springframework.data.jpa.repository.Query("select distinct p from Pedido p left join fetch p.detalles d left join fetch d.producto where p.id in :ids")
    java.util.List<Pedido> cargarDetalles(@org.springframework.data.repository.query.Param("ids") java.util.Collection<Integer> ids);
    @org.springframework.data.jpa.repository.Query("select p.estado, count(p), coalesce(sum(p.total), 0) from Pedido p where p.fecha >= :inicio and p.fecha < :fin group by p.estado")
    java.util.List<Object[]> resumenReporte(java.time.LocalDateTime inicio, java.time.LocalDateTime fin);

    @org.springframework.data.jpa.repository.Query("select year(p.fecha), month(p.fecha), day(p.fecha), sum(p.total) from Pedido p where p.fecha >= :inicio and p.fecha < :fin and p.estado = 'ENTREGADO' group by year(p.fecha), month(p.fecha), day(p.fecha) order by year(p.fecha), month(p.fecha), day(p.fecha)")
    java.util.List<Object[]> tendenciaReporte(java.time.LocalDateTime inicio, java.time.LocalDateTime fin);

    @org.springframework.data.jpa.repository.Query("select p.metodoPago, count(p), sum(p.total) from Pedido p where p.fecha >= :inicio and p.fecha < :fin and p.estado = 'ENTREGADO' group by p.metodoPago")
    java.util.List<Object[]> pagosReporte(java.time.LocalDateTime inicio, java.time.LocalDateTime fin);

    @org.springframework.data.jpa.repository.Query("select d.producto.id, d.producto.nombre, sum(d.cantidad), sum(d.subtotal) from DetallePedido d where d.pedido.fecha >= :inicio and d.pedido.fecha < :fin and d.pedido.estado = 'ENTREGADO' group by d.producto.id, d.producto.nombre order by sum(d.subtotal) desc, d.producto.id asc")
    java.util.List<Object[]> productosReporte(java.time.LocalDateTime inicio, java.time.LocalDateTime fin, org.springframework.data.domain.Pageable limite);

    java.util.Optional<Pedido> findByUsuarioIdAndClaveCompra(Integer usuarioId, String claveCompra);
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"usuario"})
    org.springframework.data.domain.Page<Pedido> findByUsuarioId(Integer usuarioId, org.springframework.data.domain.Pageable pagina);
    java.util.Optional<Pedido> findFirstByUsuarioIdOrderByFechaDesc(Integer usuarioId);
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update Pedido p set p.usuario = null where p.usuario.id = :id")
    int anonimizarPorUsuario(@org.springframework.data.repository.query.Param("id") Integer id);
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"usuario", "detalles", "detalles.producto", "detalles.producto.categoria"})
    java.util.List<Pedido> findByUsuarioIdOrderByFechaDesc(Integer usuarioId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"usuario"})
    java.util.List<Pedido> findAllByOrderByFechaDesc();

    @org.springframework.data.jpa.repository.Query("select coalesce(sum(p.total), 0) from Pedido p where p.usuario.id = :id")
    double totalPorUsuario(@org.springframework.data.repository.query.Param("id") Integer id);

    @org.springframework.data.jpa.repository.Query("select coalesce(sum(p.total), 0) from Pedido p where p.fecha >= :inicio and p.fecha < :fin")
    double ventasEntre(@org.springframework.data.repository.query.Param("inicio") java.time.LocalDateTime inicio,
                       @org.springframework.data.repository.query.Param("fin") java.time.LocalDateTime fin);

    @org.springframework.data.jpa.repository.Query("select p.metodoPago, count(p) from Pedido p group by p.metodoPago")
    java.util.List<Object[]> contarPorMetodoPago();

    @org.springframework.data.jpa.repository.Query("select d.producto.nombre, sum(d.subtotal) from DetallePedido d group by d.producto.nombre order by sum(d.subtotal) desc")
    java.util.List<Object[]> ventasPorProducto(org.springframework.data.domain.Pageable limite);
}
