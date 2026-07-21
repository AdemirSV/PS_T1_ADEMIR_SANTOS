package edu.pe.cibertec.taller.bdd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.pe.cibertec.taller.modelo.Cita;
import edu.pe.cibertec.taller.modelo.EstadoCita;
import edu.pe.cibertec.taller.modelo.Mecanico;
import edu.pe.cibertec.taller.modelo.TipoServicio;
import edu.pe.cibertec.taller.repositorio.RepositorioCitas;
import edu.pe.cibertec.taller.repositorio.RepositorioMecanicos;
import edu.pe.cibertec.taller.servicio.impl.ServicioCitasImpl;
import edu.pe.cibertec.taller.util.ProveedorFechaHora;
import edu.pe.cibertec.taller.util.ServicioNotificaciones;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GestionCitasSteps {

	private RepositorioMecanicos repositorioMecanicos;
	private RepositorioCitas repositorioCitas;
	private ProveedorFechaHora proveedorFechaHora;
	private ServicioNotificaciones servicioNotificaciones;
	private ServicioCitasImpl servicioCitas;

	private Mecanico mecanicoTest;
	private Cita citaResultado;
	private Exception excepcionCapturada;

	@Before
	public void inicializar() {
		repositorioMecanicos = mock(RepositorioMecanicos.class);
		repositorioCitas = mock(RepositorioCitas.class);
		proveedorFechaHora = mock(ProveedorFechaHora.class);
		servicioNotificaciones = mock(ServicioNotificaciones.class);
		servicioCitas = new ServicioCitasImpl(repositorioMecanicos, repositorioCitas,
				proveedorFechaHora, servicioNotificaciones);
	}

	// TODO: implementar aqui los pasos de los escenarios con
	// @Given, @When, @Then y @And (io.cucumber.java.en)

	@Given("que existe un mecanico con especialidad MANTENIMIENTO_LIGERO")
	public void queExisteUnMecanicoConEspecialidadMantenimientoLigero() {
		mecanicoTest = new Mecanico(10L, "Ademir Santos", TipoServicio.MANTENIMIENTO_LIGERO);
		when(repositorioMecanicos.findById(10L)).thenReturn(Optional.of(mecanicoTest));
	}

	@When("el cliente con placa {string} agenda una cita de MANTENIMIENTO_LIGERO el dia 18 de setiembre de 2026 a las 14:00")
	public void elClienteAgendaCita(String placa) {
		LocalDateTime fechaCita = LocalDateTime.of(2026, 9, 18, 14, 0);
		LocalDateTime relojSimulado = LocalDateTime.of(2026, 9, 17, 8, 0);

		when(proveedorFechaHora.ahora()).thenReturn(relojSimulado);
		when(repositorioCitas.findByMecanicoIdAndEstado(10L, EstadoCita.PROGRAMADA)).thenReturn(new ArrayList<>());
		when(repositorioCitas.save(any(Cita.class))).thenAnswer(i -> i.getArgument(0));

		citaResultado = servicioCitas.agendarCita(10L, placa, TipoServicio.MANTENIMIENTO_LIGERO, fechaCita);
	}
	@Then("la cita queda registrada con estado PROGRAMADA")
	public void laCitaQuedaRegistradaConEstadoProgramada() {
		assertNotNull(citaResultado);
		assertEquals(EstadoCita.PROGRAMADA, citaResultado.getEstado());
	}

	@And("se notifica que la cita fue agendada exitosamente")
	public void seNotificaQueLaCitaFueAgendadaExitosamente() {
		verify(servicioNotificaciones, times(1)).notificarCitaAgendada(any(Cita.class));
	}

	@Given("que el mecanico ya tiene una cita programada el dia 18 de setiembre de 2026 de 10:00 a 12:00")
	public void queElMecanicoYaTieneUnaCitaProgramada() {
		mecanicoTest = new Mecanico(1L, "Ademir Santos", TipoServicio.CAMBIO_ACEITE);
		when(repositorioMecanicos.findById(1L)).thenReturn(Optional.of(mecanicoTest));

		LocalDateTime inicioExistente = LocalDateTime.of(2026, 9, 18, 10, 0);
		Cita citaExistente = new Cita();
		citaExistente.setId(1L);
		citaExistente.setFechaHoraInicio(inicioExistente);
		citaExistente.setDuracionHoras(2); // Termina a las 12:00
		citaExistente.setEstado(EstadoCita.PROGRAMADA);
		citaExistente.setMecanico(mecanicoTest);

		List<Cita> listaCitas = new ArrayList<>();
		listaCitas.add(citaExistente);

		when(repositorioCitas.findByMecanicoIdAndEstado(1L, EstadoCita.PROGRAMADA)).thenReturn(listaCitas);
	}

	@When("el cliente con placa {string} intenta agendar otra cita con el mismo mecanico a las 11:00")
	public void intentaAgendarALas11(String placa) {
		LocalDateTime fechaCita = LocalDateTime.of(2026, 9, 18, 11, 0);
		LocalDateTime relojSimulado = LocalDateTime.of(2026, 9, 17, 8, 0);
		when(proveedorFechaHora.ahora()).thenReturn(relojSimulado);

		excepcionCapturada = assertThrows(Exception.class, () -> {
			servicioCitas.agendarCita(1L, placa, TipoServicio.CAMBIO_ACEITE, fechaCita);
		});
	}

	@When("el cliente con placa {string} intenta agendar otra cita con el mismo mecanico a las 12:00")
	public void intentaAgendarALas12(String placa) {
		LocalDateTime fechaCita = LocalDateTime.of(2026, 9, 18, 12, 0);
		LocalDateTime relojSimulado = LocalDateTime.of(2026, 9, 17, 8, 0);
		when(proveedorFechaHora.ahora()).thenReturn(relojSimulado);
		when(repositorioCitas.save(any(Cita.class))).thenAnswer(i -> i.getArgument(0));

		// A las 12:00, al ser contigua (termina a las 12 y la nueva empieza a las 12), el sistema la acepta
		try {
			citaResultado = servicioCitas.agendarCita(1L, placa, TipoServicio.CAMBIO_ACEITE, fechaCita);
		} catch (Exception e) {
			excepcionCapturada = e;
		}
	}

	@Then("se rechaza el agendamiento afirmando el resultado de superposicion")
	public void seRechazaElAgendamientoSuperposicion() {
		assertNotNull(excepcionCapturada);
	}

	@Then("se evalua el agendamiento afirmando el resultado real del servicio")
	public void seEvaluaElAgendamientoResultadoReal() {
		// Al ser contigua, pasa exitosamente
		assertNotNull(citaResultado);
		assertEquals(EstadoCita.PROGRAMADA, citaResultado.getEstado());
	}
}
