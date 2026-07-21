package edu.pe.cibertec.taller.servicio;

import edu.pe.cibertec.taller.excepcion.EspecialidadIncorrectaException;
import edu.pe.cibertec.taller.excepcion.HorarioNoPermitidoException;
import edu.pe.cibertec.taller.excepcion.MecanicoNoEncontradoException;
import edu.pe.cibertec.taller.modelo.Cita;
import edu.pe.cibertec.taller.modelo.EstadoCita;
import edu.pe.cibertec.taller.modelo.Mecanico;
import edu.pe.cibertec.taller.modelo.TipoServicio;
import edu.pe.cibertec.taller.repositorio.RepositorioCitas;
import edu.pe.cibertec.taller.repositorio.RepositorioMecanicos;
import edu.pe.cibertec.taller.servicio.impl.ServicioCitasImpl;
import edu.pe.cibertec.taller.util.ProveedorFechaHora;
import edu.pe.cibertec.taller.util.ServicioNotificaciones;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ServicioCitasImplTest {

	@Mock
	private RepositorioMecanicos repositorioMecanicos;

	@Mock
	private RepositorioCitas repositorioCitas;

	@Mock
	private ProveedorFechaHora proveedorFechaHora;

	@Mock
	private ServicioNotificaciones servicioNotificaciones;

	private ServicioCitasImpl servicioCitas;

	private final String PLACA = "SAN-028";
	private final int DIA = 18;
	private final String MECANICO = "Ademir Santos";

	private Mecanico mecanicoT1;
	private LocalDateTime fechaReloj;

	@BeforeEach
	void inicializar() {
		servicioCitas = new ServicioCitasImpl(repositorioMecanicos, repositorioCitas,
				proveedorFechaHora, servicioNotificaciones);
		// TODO: crear aqui los datos comunes que necesiten los tests
		mecanicoT1 = new Mecanico(1L,MECANICO, TipoServicio.CAMBIO_ACEITE);
		fechaReloj = LocalDateTime.of(2026, 9, DIA - 1, 8, 0);
	}

	@Test
	@DisplayName("Agendar una cita valida la guarda, notifica y la retorna en estado PROGRAMADA")
	void agendarCitaExitosa() {
		// Arrange
		// TODO
		LocalDateTime fechaCita = LocalDateTime.of(2026, 9, DIA, 10, 0);
		when(repositorioMecanicos.findById(1L)).thenReturn(Optional.of(mecanicoT1));
		when(proveedorFechaHora.ahora()).thenReturn(fechaReloj);
		when(repositorioCitas.findByMecanicoIdAndEstado(1L, EstadoCita.PROGRAMADA)).thenReturn(new ArrayList<>());
		when(repositorioCitas.save(any(Cita.class))).thenAnswer(i -> i.getArgument(0));

		// Act
		// TODO
		Cita resultado = servicioCitas.agendarCita(1L, PLACA, TipoServicio.CAMBIO_ACEITE, fechaCita);

		// Assert
		// TODO: verificar estado, duracion, save y notificacion
		assertEquals(EstadoCita.PROGRAMADA, resultado.getEstado());
		assertEquals(1, resultado.getDuracionHoras());
		verify(repositorioCitas, times(1)).save(any(Cita.class));
		verify(servicioNotificaciones, times(1)).notificarCitaAgendada(any(Cita.class));
	}

	@Test
	@DisplayName("Agendar con un mecanico inexistente lanza MecanicoNoEncontradoException")
	void agendarConMecanicoInexistente() {
		// Arrange
		// TODO
		LocalDateTime fechaCita = LocalDateTime.of(2026, 9, DIA, 10, 0);
		when(repositorioMecanicos.findById(99L)).thenReturn(Optional.empty());

		// Act y Assert
		// TODO
		assertThrows(MecanicoNoEncontradoException.class, () -> {
			servicioCitas.agendarCita(99L, PLACA, TipoServicio.CAMBIO_ACEITE, fechaCita);
		});

		verify(repositorioCitas, never()).save(any());
	}

	@Test
	@DisplayName("Agendar cuando la especialidad no coincide lanza EspecialidadIncorrectaException")
	void agendarConEspecialidadIncorrecta() {
		// Arrange
		// TODO
		LocalDateTime fechaCita = LocalDateTime.of(2026, 9, DIA, 10, 0);
		when(repositorioMecanicos.findById(1L)).thenReturn(Optional.of(mecanicoT1));

		// Act y Assert
		// TODO
		assertThrows(EspecialidadIncorrectaException.class, () -> {
			servicioCitas.agendarCita(1L, PLACA, TipoServicio.REPARACION_MOTOR, fechaCita);
		});

		verify(repositorioCitas, never()).save(any());
	}

	@Test
	@DisplayName("Un servicio pesado a las 07:00 se rechaza con HorarioNoPermitidoException")
	void rechazareparacion0700() {
		// Arrange
		// TODO
		LocalDateTime fechaCita = LocalDateTime.of(2026, 9, DIA, 8, 0);
		Mecanico mecanicoPesado = new Mecanico(2L, MECANICO, TipoServicio.REPARACION_MOTOR);
		when(repositorioMecanicos.findById(2L)).thenReturn(Optional.of(mecanicoPesado));
		when(proveedorFechaHora.ahora()).thenReturn(fechaReloj);
		when(repositorioCitas.findByMecanicoIdAndEstado(2L, EstadoCita.PROGRAMADA)).thenReturn(new ArrayList<>());
		when(repositorioCitas.save(any(Cita.class))).thenAnswer(i -> i.getArgument(0));

		// Act y Assert
		// TODO
		Cita resultado = servicioCitas.agendarCita(2L, PLACA, TipoServicio.REPARACION_MOTOR, fechaCita);
		assertNotNull(resultado);
		assertEquals(EstadoCita.PROGRAMADA, resultado.getEstado());
	}

	@Test
	@DisplayName("Un servicio pesado a las 08:00 se acepta con limite inferior")
	void aceptareparacion0800() {
		// Arrange
		// TODO
		LocalDateTime fechaCita = LocalDateTime.of(2026, 9, DIA, 8, 0);
		Mecanico mecanicoPesado = new Mecanico(2L, MECANICO, TipoServicio.REPARACION_MOTOR);
		when(repositorioMecanicos.findById(2L)).thenReturn(Optional.of(mecanicoPesado));
		when(proveedorFechaHora.ahora()).thenReturn(fechaReloj);
		when(repositorioCitas.findByMecanicoIdAndEstado(2L, EstadoCita.PROGRAMADA)).thenReturn(new ArrayList<>());
		when(repositorioCitas.save(any(Cita.class))).thenAnswer(i -> i.getArgument(0));

		// Act y Assert
		// TODO
		Cita resultado = servicioCitas.agendarCita(2L, PLACA, TipoServicio.REPARACION_MOTOR, fechaCita);
		assertNotNull(resultado);
		assertEquals(EstadoCita.PROGRAMADA, resultado.getEstado());
	}

	@Test
	@DisplayName("Un servicio pesado a las 08:00 se acepta con limite superior")
	void aceptareparacion1100() {
		// Arrange
		// TODO
		LocalDateTime fechaCita = LocalDateTime.of(2026, 9, DIA, 11, 0);
		Mecanico mecanicoPesado = new Mecanico(2L, MECANICO, TipoServicio.REPARACION_MOTOR);
		when(repositorioMecanicos.findById(2L)).thenReturn(Optional.of(mecanicoPesado));
		when(proveedorFechaHora.ahora()).thenReturn(fechaReloj);
		when(repositorioCitas.findByMecanicoIdAndEstado(2L, EstadoCita.PROGRAMADA)).thenReturn(new ArrayList<>());
		when(repositorioCitas.save(any(Cita.class))).thenAnswer(i -> i.getArgument(0));

		// Act y Assert
		// TODO
		Cita resultado = servicioCitas.agendarCita(2L, PLACA, TipoServicio.REPARACION_MOTOR, fechaCita);
		assertNotNull(resultado);
		assertEquals(EstadoCita.PROGRAMADA, resultado.getEstado());
	}

	@Test
	@DisplayName("Un servicio pesado a las 12:00 se rechaza con HorarioNoPermitidoException")
	void rechazareparacion1200() {
		// Arrange
		// TODO
		LocalDateTime fechaCita = LocalDateTime.of(2026, 9, DIA, 12, 0);
		Mecanico mecanicoPesado = new Mecanico(2L, MECANICO, TipoServicio.REPARACION_MOTOR);
		when(repositorioMecanicos.findById(2L)).thenReturn(Optional.of(mecanicoPesado));

		// Act y Assert
		// TODO
		assertThrows(HorarioNoPermitidoException.class, () -> {
			servicioCitas.agendarCita(2L, PLACA, TipoServicio.REPARACION_MOTOR, fechaCita);
		});
	}



	@Test
	@DisplayName("Agendar en una fecha del pasado lanza FechaInvalidaException")
	void agendarConFechaEnElPasado() {
		// Arrange
		// TODO: recuerden mockear proveedorFechaHora.ahora()

		// Act y Assert
		// TODO
	}

	@Test
	@DisplayName("Agendar sobre una cita ya programada se rechaza con HorarioOcupadoException")
	void agendarConSuperposicion() {
		// Arrange
		// TODO

		// Act y Assert
		// TODO
	}

	@Test
	@DisplayName("Una cita que empieza justo cuando termina otra se acepta")
	void agendarCitaContigua() {
		// Arrange
		// TODO: una cita existente que termina a las 10:00 y la nueva que empieza a las 10:00

		// Act
		// TODO

		// Assert
		// TODO
	}

	@Test
	@DisplayName("Cancelar con 24 horas o mas de anticipacion no genera penalidad")
	void cancelarConAnticipacionSuficiente() {
		// Arrange
		// TODO

		// Act
		// TODO

		// Assert
		// TODO: penalidad 0, estado CANCELADA, notificacion
	}

	@Test
	@DisplayName("Cancelar con menos de 24 horas aplica una penalidad de 50.00")
	void cancelarConAvisoTardio() {
		// Arrange
		// TODO

		// Act
		// TODO

		// Assert
		// TODO
	}

	@Test
	@DisplayName("Cancelar una cita inexistente lanza CitaNoEncontradaException")
	void cancelarCitaInexistente() {
		// Arrange
		// TODO

		// Act y Assert
		// TODO
	}

	@Test
	@DisplayName("Cancelar una cita que ya fue cancelada lanza CitaNoCancelableException")
	void cancelarCitaYaCancelada() {
		// Arrange
		// TODO

		// Act y Assert
		// TODO
	}

	@Test
	@DisplayName("Buscar mecanico disponible retorna el primero sin citas superpuestas")
	void buscarMecanicoDisponibleRetornaPrimeroLibre() {
		// Arrange
		// TODO: dos mecanicos de la misma especialidad, el primero ocupado

		// Act
		// TODO

		// Assert
		// TODO
	}

	@Test
	@DisplayName("Buscar mecanico cuando ninguno esta libre lanza SinDisponibilidadException")
	void buscarMecanicoSinDisponibilidad() {
		// Arrange
		// TODO

		// Act y Assert
		// TODO
	}
}
